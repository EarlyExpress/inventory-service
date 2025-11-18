package com.early_express.inventory_service.global.presentation.exception;

/**
 * 모든 에러 코드 Enum이 구현해야 하는 인터페이스
 * */
public interface ErrorCode {

    /**
     * 에러 코드 반환 (예: "GLOBAL_001")
     * */
    String getCode();

    /**
     * 에러 메시지 반환
     * */
    String getMessage();

    /**
     * HTTP 상태 코드 반환 (예: 400, 500)
     * */
    int getStatus();

}
