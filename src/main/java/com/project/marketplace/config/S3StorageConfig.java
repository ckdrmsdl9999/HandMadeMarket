package com.project.marketplace.config;

import com.project.marketplace.storage.StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(StorageProperties storageProperties) {
        // S3 클라이언트 리전을 운영 환경변수와 같은 app.storage.s3.region 기준으로 생성함
        return S3Client.builder()
                .region(Region.of(storageProperties.getS3().getRegion()))
                .build();
    }
}
