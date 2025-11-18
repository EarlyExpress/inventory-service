package com.early_express.inventory_service.global.presentation.dto;

import com.early_express.inventory_service.global.presentation.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 에러 응답 DTO
 * */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final int status;
    private final String path;
    private final Long timestamp;

    /**
     * ErrorCode로 ErrorResponse 생성
     * */
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus())
                .path(path)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * ErrorCode와 커스텀 메시지로 ErrorResponse 생성
     * */
    public static ErrorResponse of(ErrorCode errorCode, String customMessage, String path) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(customMessage)
                .status(errorCode.getStatus())
                .path(path)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 직접 파라미터로 ErrorResponse 생성
     * */
    public static ErrorResponse of(String code, String message, int status, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .status(status)
                .path(path)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
