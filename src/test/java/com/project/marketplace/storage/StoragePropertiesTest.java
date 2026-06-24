package com.project.marketplace.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StoragePropertiesTest {

    @Test
    void buildPublicUrlUsesCloudFrontBaseUrl() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setCloudFrontBaseUrl("https://cdn.example.com/");

        String publicUrl = storageProperties.buildPublicUrl("/products/main.jpg");

        assertThat(publicUrl).isEqualTo("https://cdn.example.com/products/main.jpg");
    }

    @Test
    void getPrefixSeparatesProductAndBanner() {
        StorageProperties storageProperties = new StorageProperties();
        // 상품과 배너 업로드 위치가 서로 섞이지 않도록 prefix 정규화를 검증함
        storageProperties.getS3().setProductPrefix("/products/");
        storageProperties.getS3().setBannerPrefix("/home/");

        assertThat(storageProperties.getPrefix(ImageUploadType.PRODUCT)).isEqualTo("products");
        assertThat(storageProperties.getPrefix(ImageUploadType.BANNER)).isEqualTo("home");
    }
}
