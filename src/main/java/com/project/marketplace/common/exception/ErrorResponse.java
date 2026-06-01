package com.project.marketplace.common.exception;

import java.time.LocalDateTime;

// API 에러 응답에 message를 항상 포함하도록 공통 응답 형태를 정의함
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {

    // 예외 처리기에서 같은 형태의 응답을 만들 수 있게 생성 로직을 모음
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status,
                error,
                message,
                path
        );
    }
}
