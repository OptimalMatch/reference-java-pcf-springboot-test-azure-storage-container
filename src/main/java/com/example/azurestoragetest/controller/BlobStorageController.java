package com.example.azurestoragetest.controller;

import com.example.azurestoragetest.service.BlobStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blobs")
public class BlobStorageController {

    private final BlobStorageService blobStorageService;

    public BlobStorageController(BlobStorageService blobStorageService) {
        this.blobStorageService = blobStorageService;
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testConnection() {
        Map<String, String> response = new HashMap<>();
        try {
            String result = blobStorageService.testConnection();
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{blobName}")
    public ResponseEntity<Map<String, String>> uploadBlob(
            @PathVariable String blobName,
            @RequestBody String content) {
        Map<String, String> response = new HashMap<>();
        try {
            String blobUrl = blobStorageService.uploadBlob(blobName, content);
            response.put("status", "success");
            response.put("message", "Blob uploaded successfully");
            response.put("blobName", blobName);
            response.put("blobUrl", blobUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{blobName}")
    public ResponseEntity<Map<String, String>> downloadBlob(@PathVariable String blobName) {
        Map<String, String> response = new HashMap<>();
        try {
            String content = blobStorageService.downloadBlob(blobName);
            response.put("status", "success");
            response.put("blobName", blobName);
            response.put("content", content);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listBlobs() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> blobs = blobStorageService.listBlobs();
            response.put("status", "success");
            response.put("blobs", blobs);
            response.put("count", blobs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{blobName}")
    public ResponseEntity<Map<String, String>> deleteBlob(@PathVariable String blobName) {
        Map<String, String> response = new HashMap<>();
        try {
            blobStorageService.deleteBlob(blobName);
            response.put("status", "success");
            response.put("message", "Blob deleted successfully");
            response.put("blobName", blobName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{blobName}/exists")
    public ResponseEntity<Map<String, Object>> blobExists(@PathVariable String blobName) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean exists = blobStorageService.blobExists(blobName);
            response.put("status", "success");
            response.put("blobName", blobName);
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
