package com.example.azurestoragetest.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class BlobStorageService {

    private static final Logger logger = LoggerFactory.getLogger(BlobStorageService.class);

    private final BlobServiceClient blobServiceClient;
    private final String containerName;

    @Autowired
    public BlobStorageService(@Autowired(required = false) BlobServiceClient blobServiceClient,
                              @Autowired(required = false) @Qualifier("containerName") String containerName) {
        this.blobServiceClient = blobServiceClient;
        this.containerName = containerName;
    }

    private void checkConfigured() {
        if (blobServiceClient == null) {
            throw new IllegalStateException("Azure Storage is not configured. " +
                    "Use dynamic endpoints (/api/dynamic/*) with credentials passed via HTTP headers, or configure environment variables.");
        }
    }

    private BlobContainerClient getContainerClient() {
        checkConfigured();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            logger.info("Container '{}' does not exist. Creating it.", containerName);
            containerClient.create();
        }
        return containerClient;
    }

    public String uploadBlob(String blobName, String content) {
        logger.info("Uploading blob '{}' to container '{}'", blobName, containerName);
        BlobContainerClient containerClient = getContainerClient();
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(BinaryData.fromString(content), true);
        logger.info("Successfully uploaded blob '{}'", blobName);
        return blobClient.getBlobUrl();
    }

    public String downloadBlob(String blobName) {
        logger.info("Downloading blob '{}' from container '{}'", blobName, containerName);
        BlobContainerClient containerClient = getContainerClient();
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

    public List<String> listBlobs() {
        logger.info("Listing blobs in container '{}'", containerName);
        BlobContainerClient containerClient = getContainerClient();
        List<String> blobNames = new ArrayList<>();

        for (BlobItem blobItem : containerClient.listBlobs()) {
            blobNames.add(blobItem.getName());
        }

        logger.info("Found {} blobs in container '{}'", blobNames.size(), containerName);
        return blobNames;
    }

    public void deleteBlob(String blobName) {
        logger.info("Deleting blob '{}' from container '{}'", blobName, containerName);
        BlobContainerClient containerClient = getContainerClient();
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        if (blobClient.exists()) {
            blobClient.delete();
            logger.info("Successfully deleted blob '{}'", blobName);
        } else {
            logger.warn("Blob '{}' does not exist, nothing to delete", blobName);
        }
    }

    public boolean blobExists(String blobName) {
        BlobContainerClient containerClient = getContainerClient();
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        return blobClient.exists();
    }

    public String testConnection() {
        try {
            BlobContainerClient containerClient = getContainerClient();
            logger.info("Successfully connected to Azure Storage. Container '{}' is accessible.", containerName);
            return "Connection successful. Container '" + containerName + "' is accessible.";
        } catch (Exception e) {
            logger.error("Failed to connect to Azure Storage: {}", e.getMessage());
            throw new RuntimeException("Connection failed: " + e.getMessage(), e);
        }
    }
}
