package com.example.azurestoragetest.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private boolean useAzurite;

    @PostConstruct
    public void init() {
        parseVcapServices();
    }

    private void parseVcapServices() {
        String vcapServices = System.getenv("VCAP_SERVICES");

        // Check if we should use Azurite (local emulator)
        String useAzuriteEnv = System.getenv("USE_AZURITE");
        useAzurite = "true".equalsIgnoreCase(useAzuriteEnv);

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
                throw new RuntimeException("Failed to parse VCAP_SERVICES", e);
            }
        }

        if (accountName == null || accountKey == null || containerName == null) {
            throw new RuntimeException("Azure Storage credentials not found. Please configure VCAP_SERVICES or environment variables.");
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

    @Bean
    public StorageSharedKeyCredential storageSharedKeyCredential() {
        return new StorageSharedKeyCredential(accountName, accountKey);
    }

    @Bean
    public BlobServiceClient blobServiceClient(StorageSharedKeyCredential credential) {
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
