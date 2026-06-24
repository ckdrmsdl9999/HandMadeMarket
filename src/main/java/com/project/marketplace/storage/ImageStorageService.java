package com.project.marketplace.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    // 컨트롤러가 S3 구현 세부사항 없이 이미지 저장 결과 URL만 받게 함
    String upload(MultipartFile file, ImageUploadType uploadType);
}
