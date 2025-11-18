package com.early_express.inventory_service.global.presentation.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 전역 공통 에러 코드
 * */
@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    /**
     * 서버 에러 (500번대)
     * */
    INTERNAL_SERVER_ERROR("GLOBAL_001", "서버 내부 오류가 발생했습니다.", 500),
    SERVICE_UNAVAILABLE("GLOBAL_002", "서비스를 일시적으로 사용할 수 없습니다.", 503),
	INVALID_SECURITY_CONTEXT("GLOBAL_003", "보안 컨텍스트에 인증 정보가 존재하지 않습니다.", 500),

    /**
     * 클라이언트 에러(400번대)
     * */
    BAD_REQUEST("GLOBAL_100", "잘못된 요청입니다.", 400),
    INVALID_INPUT_VALUE("GLOBAL_101", "유효하지 않은 입력값입니다.", 400),
    INVALID_TYPE_VALUE("GLOBAL_102", "잘못된 데이터 타입입니다.", 400),
    MISSING_PARAMETER("GLOBAL_103", "필수 파라미터가 누락되었습니다.", 400),

    /**
     * 인증/인가 에러(401, 403)
     * */
    UNAUTHORIZED("GLOBAL_200", "인증이 필요합니다.", 401),
    INVALID_TOKEN("GLOBAL_201", "유효하지 않은 토큰입니다.", 401),
    EXPIRED_TOKEN("GLOBAL_202", "만료된 토큰입니다.", 401),
    TOKEN_MISSING("GLOBAL_203", "토큰이 누락되었습니다.", 401),
    FORBIDDEN("GLOBAL_210", "접근 권한이 없습니다.", 403),

    /**
     * 리소스 관련 에러 (404, 405, 409)
     * */
    RESOURCE_NOT_FOUND("GLOBAL_300", "요청한 리소스를 찾을 수 없습니다.", 404),
    METHOD_NOT_ALLOWED("GLOBAL_301", "지원하지 않는 HTTP 메소드입니다.", 405),
    CONFLICT("GLOBAL_302", "리소스 충돌이 발생했습니다.", 409),
    DUPLICATE_RESOURCE("GLOBAL_303", "이미 존재하는 리소스입니다.", 409),

    /**
     * 외부 API 에러(502, 504)
     * */
    EXTERNAL_API_ERROR("GLOBAL_400", "외부 서비스 연동 중 오류가 발생했습니다.", 502),
    EXTERNAL_API_TIMEOUT("GLOBAL_401", "외부 서비스 응답 시간이 초과되었습니다.", 504);

    private final String code;
    private final String message;
    private final int status;
}
