package com.backend.media.service;

import com.backend.media.config.S3Config;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
public class MediaStorageService {
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final S3Config props;

    public MediaStorageService(S3Client s3Client, S3Presigner presigner, S3Config props) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.props = props;
    }

    public StoredObject uploadDrug(MultipartFile file) {
        return upload(props.getBucketDrugs(), "drugs", file);
    }

    public StoredObject uploadBanner(MultipartFile file) {
        return upload(props.getBucketBanners(), "banners", file);
    }

    public String presign(String bucket, String key) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(props.presignDuration())
                .getObjectRequest(get)
                .build();
        return presigner.presignGetObject(presignReq).url().toString();
    }

    private StoredObject upload(String bucket, String prefix, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }
        String ext = extractExt(file.getOriginalFilename());
        String key = prefix + "/" + UUID.randomUUID() + (ext.isEmpty() ? "" : ("." + ext));
        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(put, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            String url = presign(bucket, key);
            return new StoredObject(bucket, key, url, Instant.now());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "upload failed", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "storage error", e);
        }
    }

    private String extractExt(String name) {
        if (name == null) {
            return "";
        }
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx + 1).toLowerCase();
    }

    public record StoredObject(String bucket, String key, String presignedUrl, Instant createdAt) {
    }
}
