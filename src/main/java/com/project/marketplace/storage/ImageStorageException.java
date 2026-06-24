package com.project.marketplace.storage;

import org.springframework.http.HttpStatus;

public class ImageStorageException extends RuntimeException {

    // 저장소 실패 원인에 따라 API 응답 상태를 다르게 내려주기 위해 상태값을 함께 보관함
    private final HttpStatus status;

    public ImageStorageException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ImageStorageException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
