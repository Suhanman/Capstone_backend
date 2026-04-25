package com.emailagent.service;

import com.emailagent.domain.entity.BusinessResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class BusinessFileStorageService {

    @Value("${app.file.storage:local}")
    private String storageMode;

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file.s3.bucket:}")
    private String s3Bucket;

    @Value("${app.file.s3.prefix:business-resources}")
    private String s3Prefix;

    @Value("${app.file.s3.region:ap-northeast-2}")
    private String s3Region;

    @Value("${app.file.s3.presigned-url-duration-minutes:15}")
    private long presignedUrlDurationMinutes;

    public String store(Long userId, MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String savedFileName = UUID.randomUUID() + "_" + sanitizeFileName(originalFileName);

        if (isS3Mode()) {
            ensureS3BucketConfigured();
            String objectKey = buildObjectKey(userId, savedFileName);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(objectKey)
                    .contentType(resolveContentType(file))
                    .contentLength(file.getSize())
                    .build();

            try (S3Client s3Client = buildS3Client()) {
                s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            }

            log.info("[BusinessFileStorageService] S3 파일 저장 완료 — bucket={}, key={}", s3Bucket, objectKey);
            return objectKey;
        }

        Path uploadPath = Paths.get(uploadDir, String.valueOf(userId));
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(savedFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }

    public void delete(BusinessResource resource) {
        if (resource == null || resource.getFilePath() == null) {
            return;
        }

        if (isS3Mode()) {
            ensureS3BucketConfigured();
            try (S3Client s3Client = buildS3Client()) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(s3Bucket)
                        .key(resource.getFilePath())
                        .build());
            } catch (Exception error) {
                log.warn("[BusinessFileStorageService] S3 파일 삭제 실패 — key={}", resource.getFilePath(), error);
            }
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(resource.getFilePath()));
        } catch (IOException error) {
            log.warn("파일 삭제 실패: {}", resource.getFilePath(), error);
        }
    }

    public String createPresignedGetUrl(BusinessResource resource) {
        if (!isS3Mode() || resource == null || resource.getFilePath() == null) {
            return null;
        }

        ensureS3BucketConfigured();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(resource.getFilePath())
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlDurationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        try (S3Presigner presigner = buildS3Presigner()) {
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        }
    }

    public boolean isS3Mode() {
        return "s3".equalsIgnoreCase(storageMode);
    }

    private S3Client buildS3Client() {
        return S3Client.builder()
                .region(Region.of(s3Region))
                .build();
    }

    private S3Presigner buildS3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(s3Region))
                .build();
    }

    private String buildObjectKey(Long userId, String savedFileName) {
        String normalizedPrefix = s3Prefix == null ? "" : s3Prefix.strip();
        if (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        if (normalizedPrefix.isBlank()) {
            return userId + "/" + savedFileName;
        }
        return normalizedPrefix + "/" + userId + "/" + savedFileName;
    }

    private void ensureS3BucketConfigured() {
        if (s3Bucket == null || s3Bucket.isBlank()) {
            throw new IllegalStateException("APP_FILE_STORAGE=s3 사용 시 APP_FILE_S3_BUCKET 설정이 필요합니다.");
        }
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType;
    }

    private String sanitizeFileName(String fileName) {
        String fallback = "upload";
        String resolved = (fileName == null || fileName.isBlank()) ? fallback : fileName;
        String sanitized = resolved.replaceAll("[\\\\/]+", "_").strip();
        if (sanitized.isBlank()) {
            sanitized = fallback;
        }
        return URLEncoder.encode(sanitized, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .toLowerCase(Locale.ROOT);
    }
}
