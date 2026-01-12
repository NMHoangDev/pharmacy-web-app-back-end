package com.backend.media.api;

import com.backend.media.api.dto.MediaUploadResponse;
import com.backend.media.api.dto.PresignResponse;
import com.backend.media.service.MediaStorageService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
}
