package com.project.marketplace.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// 컨트롤러와 서비스에서 발생한 예외를 공통 JSON 에러 응답으로 변환함
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ResponseStatusException의 reason을 message 필드로 내려 프론트에서 표시할 수 있게 함
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode = exception.getStatusCode();
        String message = resolveMessage(exception.getReason(), "요청을 처리할 수 없습니다.");

        return ResponseEntity.status(statusCode)
                .body(ErrorResponse.of(
                        statusCode.value(),
                        resolveError(statusCode),
                        message,
                        request.getRequestURI()
                ));
    }

    // 필수 요청 파라미터 누락도 message 필드로 명확히 전달함
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        String message = "필수 요청 파라미터 '" + exception.getParameterName() + "'가 없습니다.";

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        message,
                        request.getRequestURI()
                ));
    }

    // 검증 어노테이션 실패 시 첫 번째 필드 오류 메시지를 응답에 담음
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null
                ? resolveMessage(fieldError.getDefaultMessage(), "요청값이 올바르지 않습니다.")
                : "요청값이 올바르지 않습니다.";

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        message,
                        request.getRequestURI()
                ));
    }

    // 현재 서비스 검증 로직에서 사용하는 RuntimeException 메시지도 프론트에 표시되게 함
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        resolveMessage(exception.getMessage(), "요청 처리 중 오류가 발생했습니다."),
                        request.getRequestURI()
                ));
    }

    // 예상하지 못한 예외도 message 필드를 가진 JSON으로 통일해 내려줌
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "서버 처리 중 오류가 발생했습니다.",
                        request.getRequestURI()
                ));
    }

    // HttpStatusCode 구현 차이와 상관없이 error 문자열을 안정적으로 만들게 함
    private String resolveError(HttpStatusCode statusCode) {
        if (statusCode instanceof HttpStatus httpStatus) {
            return httpStatus.getReasonPhrase();
        }

        return "HTTP " + statusCode.value();
    }

    // 비어 있는 예외 메시지 대신 기본 메시지를 내려 응답 message가 비지 않게 함
    private String resolveMessage(String message, String defaultMessage) {
        return message == null || message.isBlank() ? defaultMessage : message;
    }
}
