package com.example.azurestoragetest.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class AzureStorageConfig {

    private static final Logger logger = LoggerFactory.getLogger(AzureStorageConfig.class);

    // Azurite default credentials
    private static final String AZURITE_ACCOUNT_NAME = "devstoreaccount1";
    private static final String AZURITE_ACCOUNT_KEY = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    private static final String AZURITE_BLOB_ENDPOINT = "http://127.0.0.1:10000/devstoreaccount1";

    private String accountName;
    private String accountKey;
    private String containerName;
    private String blobEndpoint;
    private boolean configured = false;

    @PostConstruct
    public void init() {
        parseVcapServices();
    }

    private void parseVcapServices() {
        String vcapServices = System.getenv("VCAP_SERVICES");

        // Check if we should use Azurite (local emulator)
        String useAzuriteEnv = System.getenv("USE_AZURITE");
        boolean useAzurite = "true".equalsIgnoreCase(useAzuriteEnv);

        if (useAzurite) {
            logger.info("Using Azurite local emulator");
            accountName = AZURITE_ACCOUNT_NAME;
            accountKey = AZURITE_ACCOUNT_KEY;
            blobEndpoint = System.getenv("AZURITE_BLOB_ENDPOINT") != null
                    ? System.getenv("AZURITE_BLOB_ENDPOINT")
                    : AZURITE_BLOB_ENDPOINT;
            containerName = System.getenv("AZURE_STORAGE_CONTAINER_NAME") != null
                    ? System.getenv("AZURE_STORAGE_CONTAINER_NAME")
                    : "test-container";
            configured = true;
            return;
        }

        if (vcapServices == null || vcapServices.isEmpty()) {
            logger.warn("VCAP_SERVICES not found. Falling back to environment variables.");
            accountName = System.getenv("AZURE_STORAGE_ACCOUNT_NAME");
            accountKey = System.getenv("AZURE_STORAGE_ACCOUNT_KEY");
            containerName = System.getenv("AZURE_STORAGE_CONTAINER_NAME");
            blobEndpoint = System.getenv("AZURE_STORAGE_BLOB_ENDPOINT");
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(vcapServices);

                // Look for user-provided service named "azure-storage" or similar
                JsonNode services = root.get("user-provided");
                if (services != null && services.isArray()) {
                    for (JsonNode service : services) {
                        String serviceName = service.get("name").asText();
                        if (serviceName.contains("azure") || serviceName.contains("storage")) {
                            JsonNode credentials = service.get("credentials");
                            if (credentials != null) {
                                accountName = getCredentialValue(credentials, "account-name", "accountName", "storage_account_name");
                                accountKey = getCredentialValue(credentials, "account-key", "accountKey", "storage_account_key");
                                containerName = getCredentialValue(credentials, "container-name", "containerName", "container_name");
                                blobEndpoint = getCredentialValue(credentials, "blob-endpoint", "blobEndpoint", "blob_endpoint");
                                logger.info("Azure Storage credentials loaded from VCAP_SERVICES (service: {})", serviceName);
                                configured = true;
                                return;
                            }
                        }
                    }
                }

                logger.warn("No Azure storage service found in VCAP_SERVICES. Falling back to environment variables.");
                accountName = System.getenv("AZURE_STORAGE_ACCOUNT_NAME");
                accountKey = System.getenv("AZURE_STORAGE_ACCOUNT_KEY");
                containerName = System.getenv("AZURE_STORAGE_CONTAINER_NAME");
                blobEndpoint = System.getenv("AZURE_STORAGE_BLOB_ENDPOINT");

            } catch (Exception e) {
                logger.error("Error parsing VCAP_SERVICES: {}", e.getMessage());
                logger.warn("Application will start but static endpoints (/api/blobs/*) will not work. Use dynamic endpoints (/api/dynamic/*) instead.");
                return;
            }
        }

        if (accountName == null || accountKey == null || containerName == null) {
            logger.warn("Azure Storage credentials not configured. Static endpoints (/api/blobs/*) will not work.");
            logger.info("Use dynamic endpoints (/api/dynamic/*) with credentials passed via HTTP headers, or configure:");
            logger.info("  - Environment variables: AZURE_STORAGE_ACCOUNT_NAME, AZURE_STORAGE_ACCOUNT_KEY, AZURE_STORAGE_CONTAINER_NAME");
            logger.info("  - Or set USE_AZURITE=true for local testing with Azurite emulator");
            logger.info("  - Or bind a user-provided service in PCF");
            configured = false;
        } else {
            configured = true;
        }
    }

    private String getCredentialValue(JsonNode credentials, String... keys) {
        for (String key : keys) {
            JsonNode value = credentials.get(key);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    public boolean isConfigured() {
        return configured;
    }

    @Bean
    public StorageSharedKeyCredential storageSharedKeyCredential() {
        if (!configured) {
            return null;
        }
        return new StorageSharedKeyCredential(accountName, accountKey);
    }

    @Bean
    public BlobServiceClient blobServiceClient() {
        if (!configured) {
            return null;
        }

        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);

        String endpoint;
        if (blobEndpoint != null && !blobEndpoint.isEmpty()) {
            endpoint = blobEndpoint;
        } else {
            endpoint = String.format("https://%s.blob.core.windows.net", accountName);
        }

        logger.info("Connecting to blob endpoint: {}", endpoint);

        return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(credential)
                .buildClient();
    }

    @Bean
    public String containerName() {
        return containerName;
    }
}
