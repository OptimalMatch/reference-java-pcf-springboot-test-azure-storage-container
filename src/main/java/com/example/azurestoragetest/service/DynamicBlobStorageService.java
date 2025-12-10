package com.example.azurestoragetest.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class DynamicBlobStorageService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicBlobStorageService.class);

    public BlobServiceClient createBlobServiceClient(String accountName, String accountKey, String blobEndpoint) {
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);

        String endpoint;
        if (blobEndpoint != null && !blobEndpoint.isEmpty()) {
            endpoint = blobEndpoint;
        } else {
            endpoint = String.format("https://%s.blob.core.windows.net", accountName);
        }

        logger.info("Creating dynamic blob service client for endpoint: {}", endpoint);

        return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(credential)
                .buildClient();
    }

    private BlobContainerClient getContainerClient(BlobServiceClient client, String containerName) {
        BlobContainerClient containerClient = client.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            logger.info("Container '{}' does not exist. Creating it.", containerName);
            containerClient.create();
        }
        return containerClient;
    }

    public String testConnection(String accountName, String accountKey, String blobEndpoint, String containerName) {
        try {
            BlobServiceClient client = createBlobServiceClient(accountName, accountKey, blobEndpoint);
            BlobContainerClient containerClient = getContainerClient(client, containerName);
            logger.info("Successfully connected to Azure Storage. Container '{}' is accessible.", containerName);
            return "Connection successful. Container '" + containerName + "' is accessible.";
        } catch (Exception e) {
            logger.error("Failed to connect to Azure Storage: {}", e.getMessage());
            throw new RuntimeException("Connection failed: " + e.getMessage(), e);
        }
    }

    public String uploadBlob(String accountName, String accountKey, String blobEndpoint,
                             String containerName, String blobName, String content) {
        logger.info("Uploading blob '{}' to container '{}'", blobName, containerName);
        BlobServiceClient client = createBlobServiceClient(accountName, accountKey, blobEndpoint);
        BlobContainerClient containerClient = getContainerClient(client, containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(BinaryData.fromString(content), true);
        logger.info("Successfully uploaded blob '{}'", blobName);
        return blobClient.getBlobUrl();
    }

    public String downloadBlob(String accountName, String accountKey, String blobEndpoint,
                               String containerName, String blobName) {
        logger.info("Downloading blob '{}' from container '{}'", blobName, containerName);
        BlobServiceClient client = createBlobServiceClient(accountName, accountKey, blobEndpoint);
        BlobContainerClient containerClient = getContainerClient(client, containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        if (!blobClient.exists()) {
            throw new RuntimeException("Blob '" + blobName + "' does not exist");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        String content = outputStream.toString();
        logger.info("Successfully downloaded blob '{}'", blobName);
        return content;
    }

    public List<String> listBlobs(String accountName, String accountKey, String blobEndpoint, String containerName) {
        logger.info("Listing blobs in container '{}'", containerName);
        BlobServiceClient client = createBlobServiceClient(accountName, accountKey, blobEndpoint);
        BlobContainerClient containerClient = getContainerClient(client, containerName);
        List<String> blobNames = new ArrayList<>();

        for (BlobItem blobItem : containerClient.listBlobs()) {
            blobNames.add(blobItem.getName());
        }

        logger.info("Found {} blobs in container '{}'", blobNames.size(), containerName);
        return blobNames;
    }

    public void deleteBlob(String accountName, String accountKey, String blobEndpoint,
                           String containerName, String blobName) {
        logger.info("Deleting blob '{}' from container '{}'", blobName, containerName);
        BlobServiceClient client = createBlobServiceClient(accountName, accountKey, blobEndpoint);
        BlobContainerClient containerClient = getContainerClient(client, containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        if (blobClient.exists()) {
            blobClient.delete();
            logger.info("Successfully deleted blob '{}'", blobName);
        } else {
            logger.warn("Blob '{}' does not exist, nothing to delete", blobName);
        }
    }

    public boolean blobExists(String accountName, String accountKey, String blobEndpoint,
                              String containerName, String blobName) {
        BlobServiceClient client = createBlobServiceClient(accountName, accountKey, blobEndpoint);
        BlobContainerClient containerClient = getContainerClient(client, containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        return blobClient.exists();
    }
}
