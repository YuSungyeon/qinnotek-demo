package com.qinnotek.photo.exception;

import org.springframework.http.HttpStatus;

/**
 * 비즈니스 규칙 위반 예외.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    /** 만료되었거나 더 이상 쓸 수 없는 자원 (예: 기간이 지난 제출 링크) */
    public static BusinessException gone(String message) {
        return new BusinessException(HttpStatus.GONE, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
