package com.backend.media.service;

import com.backend.media.config.S3Config;
import org.springframework.beans.factory.annotation.Value;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

@Service
public class MediaStorageService {
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final S3Config props;
    private final Path localRoot;
    private final String baseUrl;
    private final boolean localFallback;

    public MediaStorageService(S3Client s3Client,
            S3Presigner presigner,
            S3Config props,
            @Value("${media.local-path:uploads}") String localPath,
            @Value("${media.base-url:http://localhost:8087}") String baseUrl,
            @Value("${media.local-fallback:true}") boolean localFallback) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.props = props;
        this.localRoot = Paths.get(localPath).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        this.localFallback = localFallback;
    }

    public StoredObject uploadDrug(MultipartFile file) {
        return upload(props.getBucketDrugs(), "drugs", file);
    }

    public StoredObject uploadDrugBytes(String albumId, byte[] bytes, String contentType, String filename) {
        return uploadBytes(props.getBucketDrugs(), "drugs", albumId, bytes, contentType, filename);
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
            if (localFallback) {
                return storeLocal(prefix, null, safeBytes(file), file.getContentType(), file.getOriginalFilename());
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "storage error", e);
        }
    }

    private StoredObject uploadBytes(String bucket,
            String prefix,
            String albumId,
            byte[] bytes,
            String contentType,
            String filename) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "image bytes are required");
        }
        String safeAlbum = albumId == null || albumId.isBlank() ? UUID.randomUUID().toString() : albumId.trim();
        String ext = extractExt(filename);
        String key = prefix + "/" + safeAlbum + "/" + UUID.randomUUID() + (ext.isEmpty() ? "" : ("." + ext));
        String finalContentType = contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType;
        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(finalContentType)
                    .contentLength((long) bytes.length)
                    .build();
            s3Client.putObject(put, RequestBody.fromBytes(bytes));
            String url = presign(bucket, key);
            return new StoredObject(bucket, key, url, Instant.now());
        } catch (Exception e) {
            if (localFallback) {
                return storeLocal(prefix, safeAlbum, bytes, finalContentType, filename);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "storage error", e);
        }
    }

    private StoredObject storeLocal(String prefix,
            String albumId,
            byte[] bytes,
            String contentType,
            String filename) {
        try {
            String safeAlbum = albumId == null || albumId.isBlank() ? UUID.randomUUID().toString() : albumId.trim();
            String ext = extractExt(filename);
            String fileName = UUID.randomUUID() + (ext.isEmpty() ? "" : ("." + ext));
            Path targetDir = localRoot.resolve(prefix).resolve(safeAlbum).normalize();
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(fileName).normalize();
            Files.write(targetFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            String key = prefix + "/" + safeAlbum + "/" + fileName;
            String url = baseUrl.replaceAll("/$", "") + "/api/media/local/" + key;
            return new StoredObject("local", key, url, Instant.now());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "local storage failed", e);
        }
    }

    private byte[] safeBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "read file failed", e);
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
