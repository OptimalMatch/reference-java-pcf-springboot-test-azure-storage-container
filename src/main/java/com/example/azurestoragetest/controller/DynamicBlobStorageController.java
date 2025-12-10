package com.example.azurestoragetest.controller;

import com.example.azurestoragetest.service.DynamicBlobStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller that accepts Azure Storage credentials via request headers.
 * Use this endpoint to test different storage accounts without restarting the app.
 *
 * Required headers:
 * - X-Azure-Account-Name: Storage account name
 * - X-Azure-Account-Key: Storage account key
 * - X-Azure-Container-Name: Container name
 *
 * Optional headers:
 * - X-Azure-Blob-Endpoint: Custom blob endpoint (for Azurite or sovereign clouds)
 */
@RestController
@RequestMapping("/api/dynamic")
public class DynamicBlobStorageController {

    private static final String HEADER_ACCOUNT_NAME = "X-Azure-Account-Name";
    private static final String HEADER_ACCOUNT_KEY = "X-Azure-Account-Key";
    private static final String HEADER_CONTAINER_NAME = "X-Azure-Container-Name";
    private static final String HEADER_BLOB_ENDPOINT = "X-Azure-Blob-Endpoint";

    private final DynamicBlobStorageService dynamicBlobStorageService;

    public DynamicBlobStorageController(DynamicBlobStorageService dynamicBlobStorageService) {
        this.dynamicBlobStorageService = dynamicBlobStorageService;
    }

    private void validateHeaders(String accountName, String accountKey, String containerName) {
        if (accountName == null || accountName.isEmpty()) {
            throw new IllegalArgumentException("Missing required header: " + HEADER_ACCOUNT_NAME);
        }
        if (accountKey == null || accountKey.isEmpty()) {
            throw new IllegalArgumentException("Missing required header: " + HEADER_ACCOUNT_KEY);
        }
        if (containerName == null || containerName.isEmpty()) {
            throw new IllegalArgumentException("Missing required header: " + HEADER_CONTAINER_NAME);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testConnection(
            @RequestHeader(value = HEADER_ACCOUNT_NAME, required = false) String accountName,
            @RequestHeader(value = HEADER_ACCOUNT_KEY, required = false) String accountKey,
            @RequestHeader(value = HEADER_CONTAINER_NAME, required = false) String containerName,
            @RequestHeader(value = HEADER_BLOB_ENDPOINT, required = false) String blobEndpoint) {

        Map<String, String> response = new HashMap<>();
        try {
            validateHeaders(accountName, accountKey, containerName);
            String result = dynamicBlobStorageService.testConnection(accountName, accountKey, blobEndpoint, containerName);
            response.put("status", "success");
            response.put("message", result);
            response.put("accountName", accountName);
            response.put("containerName", containerName);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/blobs/{blobName}")
    public ResponseEntity<Map<String, String>> uploadBlob(
            @PathVariable String blobName,
            @RequestBody String content,
            @RequestHeader(value = HEADER_ACCOUNT_NAME, required = false) String accountName,
            @RequestHeader(value = HEADER_ACCOUNT_KEY, required = false) String accountKey,
            @RequestHeader(value = HEADER_CONTAINER_NAME, required = false) String containerName,
            @RequestHeader(value = HEADER_BLOB_ENDPOINT, required = false) String blobEndpoint) {

        Map<String, String> response = new HashMap<>();
        try {
            validateHeaders(accountName, accountKey, containerName);
            String blobUrl = dynamicBlobStorageService.uploadBlob(accountName, accountKey, blobEndpoint, containerName, blobName, content);
            response.put("status", "success");
            response.put("message", "Blob uploaded successfully");
            response.put("blobName", blobName);
            response.put("blobUrl", blobUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/blobs/{blobName}")
    public ResponseEntity<Map<String, String>> downloadBlob(
            @PathVariable String blobName,
            @RequestHeader(value = HEADER_ACCOUNT_NAME, required = false) String accountName,
            @RequestHeader(value = HEADER_ACCOUNT_KEY, required = false) String accountKey,
            @RequestHeader(value = HEADER_CONTAINER_NAME, required = false) String containerName,
            @RequestHeader(value = HEADER_BLOB_ENDPOINT, required = false) String blobEndpoint) {

        Map<String, String> response = new HashMap<>();
        try {
            validateHeaders(accountName, accountKey, containerName);
            String content = dynamicBlobStorageService.downloadBlob(accountName, accountKey, blobEndpoint, containerName, blobName);
            response.put("status", "success");
            response.put("blobName", blobName);
            response.put("content", content);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/blobs")
    public ResponseEntity<Map<String, Object>> listBlobs(
            @RequestHeader(value = HEADER_ACCOUNT_NAME, required = false) String accountName,
            @RequestHeader(value = HEADER_ACCOUNT_KEY, required = false) String accountKey,
            @RequestHeader(value = HEADER_CONTAINER_NAME, required = false) String containerName,
            @RequestHeader(value = HEADER_BLOB_ENDPOINT, required = false) String blobEndpoint) {

        Map<String, Object> response = new HashMap<>();
        try {
            validateHeaders(accountName, accountKey, containerName);
            List<String> blobs = dynamicBlobStorageService.listBlobs(accountName, accountKey, blobEndpoint, containerName);
            response.put("status", "success");
            response.put("blobs", blobs);
            response.put("count", blobs.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/blobs/{blobName}")
    public ResponseEntity<Map<String, String>> deleteBlob(
            @PathVariable String blobName,
            @RequestHeader(value = HEADER_ACCOUNT_NAME, required = false) String accountName,
            @RequestHeader(value = HEADER_ACCOUNT_KEY, required = false) String accountKey,
            @RequestHeader(value = HEADER_CONTAINER_NAME, required = false) String containerName,
            @RequestHeader(value = HEADER_BLOB_ENDPOINT, required = false) String blobEndpoint) {

        Map<String, String> response = new HashMap<>();
        try {
            validateHeaders(accountName, accountKey, containerName);
            dynamicBlobStorageService.deleteBlob(accountName, accountKey, blobEndpoint, containerName, blobName);
            response.put("status", "success");
            response.put("message", "Blob deleted successfully");
            response.put("blobName", blobName);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/blobs/{blobName}/exists")
    public ResponseEntity<Map<String, Object>> blobExists(
            @PathVariable String blobName,
            @RequestHeader(value = HEADER_ACCOUNT_NAME, required = false) String accountName,
            @RequestHeader(value = HEADER_ACCOUNT_KEY, required = false) String accountKey,
            @RequestHeader(value = HEADER_CONTAINER_NAME, required = false) String containerName,
            @RequestHeader(value = HEADER_BLOB_ENDPOINT, required = false) String blobEndpoint) {

        Map<String, Object> response = new HashMap<>();
        try {
            validateHeaders(accountName, accountKey, containerName);
            boolean exists = dynamicBlobStorageService.blobExists(accountName, accountKey, blobEndpoint, containerName, blobName);
            response.put("status", "success");
            response.put("blobName", blobName);
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
