package com.backend.media.api;

import com.backend.media.api.dto.Base64ImageRequest;
import com.backend.media.api.dto.Base64UploadRequest;
import com.backend.media.api.dto.BatchUploadResponse;
import com.backend.media.api.dto.MediaUploadResponse;
import com.backend.media.api.dto.PresignResponse;
import com.backend.media.service.MediaStorageService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaStorageService storageService;

    public MediaController(MediaStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("media-service ok");
    }

    @PostMapping("/drugs")
    public ResponseEntity<MediaUploadResponse> uploadDrug(@RequestParam("file") MultipartFile file) {
        var stored = storageService.uploadDrug(file);
        return ResponseEntity.ok(new MediaUploadResponse(stored.bucket(), stored.key(), stored.presignedUrl()));
    }

    @PostMapping("/drugs/base64")
    public ResponseEntity<BatchUploadResponse> uploadDrugBase64(
            @org.springframework.web.bind.annotation.RequestBody Base64UploadRequest request) {
        if (request == null || request.images() == null || request.images().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "images are required");
        }
        String albumId = request.albumId() == null || request.albumId().isBlank()
                ? UUID.randomUUID().toString()
                : request.albumId().trim();

        List<MediaUploadResponse> items = request.images().stream()
                .filter(Objects::nonNull)
                .map(image -> uploadBase64Image(albumId, image))
                .toList();

        return ResponseEntity.ok(new BatchUploadResponse(albumId, items));
    }

    @PostMapping("/banners")
    public ResponseEntity<MediaUploadResponse> uploadBanner(@RequestParam("file") MultipartFile file) {
        var stored = storageService.uploadBanner(file);
        return ResponseEntity.ok(new MediaUploadResponse(stored.bucket(), stored.key(), stored.presignedUrl()));
    }

    @GetMapping("/presign")
    public ResponseEntity<PresignResponse> presign(@RequestParam @NotBlank String bucket,
            @RequestParam @NotBlank String key) {
        String url = storageService.presign(bucket, key);
        return ResponseEntity.ok(new PresignResponse(bucket, key, url));
    }

    @GetMapping("/local/{prefix}/{albumId}/{filename}")
    public ResponseEntity<byte[]> serveLocal(@PathVariable String prefix,
            @PathVariable String albumId,
            @PathVariable String filename) {
        if (!prefix.matches("[a-zA-Z0-9_-]+")
                || albumId.contains("..")
                || filename.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid path");
        }
        try {
            Path file = Paths.get("uploads")
                    .resolve(prefix)
                    .resolve(albumId)
                    .resolve(filename)
                    .normalize();
            if (!Files.exists(file)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found");
            }
            byte[] bytes = Files.readAllBytes(file);
            String contentType = Files.probeContentType(file);
            return ResponseEntity.ok()
                    .contentType(contentType != null ? MediaType.parseMediaType(contentType)
                            : MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "read file failed", ex);
        }
    }

    private MediaUploadResponse uploadBase64Image(String albumId, Base64ImageRequest image) {
        String base64 = image.base64();
        if (base64 == null || base64.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "base64 is required");
        }

        String contentType = image.contentType();
        String payload = base64.trim();
        if (payload.startsWith("data:")) {
            int comma = payload.indexOf(',');
            if (comma > 0) {
                String meta = payload.substring(5, comma);
                if (meta.contains(";")) {
                    contentType = meta.substring(0, meta.indexOf(';'));
                }
                payload = payload.substring(comma + 1);
            }
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid base64 data", ex);
        }

        var stored = storageService.uploadDrugBytes(albumId, bytes, contentType, image.filename());
        return new MediaUploadResponse(stored.bucket(), stored.key(), stored.presignedUrl());
    }
}
