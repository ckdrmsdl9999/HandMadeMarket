package com.project.marketplace.storage;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3ImageStorageServiceTest {

    @Test
    void uploadStoresImageWithProductPrefixAndReturnsCloudFrontUrl() {
        S3Client s3Client = mock(S3Client.class);
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setCloudFrontBaseUrl("https://cdn.example.com");
        storageProperties.getS3().setBucket("handmade-images");
        storageProperties.getS3().setProductPrefix("products");
        S3ImageStorageService storageService = new S3ImageStorageService(s3Client, storageProperties);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "main.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        String imageUrl = storageService.upload(file, ImageUploadType.PRODUCT);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();

        assertThat(request.bucket()).isEqualTo("handmade-images");
        assertThat(request.key()).startsWith("products/").endsWith(".jpg");
        assertThat(request.contentType()).isEqualTo("image/jpeg");
        assertThat(imageUrl).startsWith("https://cdn.example.com/products/").endsWith(".jpg");
    }

    @Test
    void uploadFailsWithServiceUnavailableWhenBucketIsMissing() {
        S3Client s3Client = mock(S3Client.class);
        StorageProperties storageProperties = new StorageProperties();
        S3ImageStorageService storageService = new S3ImageStorageService(s3Client, storageProperties);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "main.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        assertThatThrownBy(() -> storageService.upload(file, ImageUploadType.PRODUCT))
                .isInstanceOf(ImageStorageException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
