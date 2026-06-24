package com.project.marketplace.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ImageStorageService implements ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    @Override
    public String upload(MultipartFile file, ImageUploadType uploadType) {
        validateFile(file);

        String objectKey = buildObjectKey(file, uploadType);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.getS3().getBucket())
                .key(objectKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try {
            // S3에는 원본 파일 bytes를 저장하고 응답에는 CloudFront 기준 공개 URL만 반환함
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 파일을 읽지 못했습니다.", e);
        }

        return storageProperties.buildPublicUrl(objectKey);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지 파일이 없습니다.");
        }

        if (storageProperties.getS3().getBucket() == null || storageProperties.getS3().getBucket().isBlank()) {
            throw new IllegalStateException("S3 버킷 설정이 필요합니다.");
        }

        long maxUploadSize = storageProperties.getS3().getMaxUploadSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxUploadSize) {
            throw new IllegalArgumentException("이미지 파일 크기가 제한을 초과했습니다.");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 확장자입니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private String buildObjectKey(MultipartFile file, ImageUploadType uploadType) {
        String prefix = storageProperties.getPrefix(uploadType);
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;

        if (prefix.isBlank()) {
            return filename;
        }

        return prefix + "/" + filename;
    }

    private String getExtension(String filename) {
        String cleanFilename = StringUtils.cleanPath(filename == null ? "" : filename);
        int extensionIndex = cleanFilename.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == cleanFilename.length() - 1) {
            return "";
        }

        return cleanFilename.substring(extensionIndex + 1).toLowerCase();
    }
}
