package com.early_express.inventory_service.global.presentation.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private Long timestamp;

    /**
     * 데이터와 함께 성공 응답 생성
     * */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 데이터와 메시지를 포함한 성공 응답 생성
     * */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 데이터 없이 성공 응답 생성
     * */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 메시지와 함께 실패 응답 생성
     * */
    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 에러 코드와 함께 실패 응답 생성
     * */
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 에러 코드, 메시지, 데이터와 함께 실패 응답 생성
     * */
    public static <T> ApiResponse<T> error(String errorCode, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
