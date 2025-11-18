package com.early_express.inventory_service.global.presentation.exception;

import com.early_express.inventory_service.global.presentation.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기
 * GlobalException과 공통 Spring 예외만 처리
 * */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * GlobalException 및 모든 하위 예외 통합 처리
     * */
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            HttpServletRequest request,
            GlobalException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("글로벌 예외 발생: {} - {}", errorCode.getCode(), e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode.getCode(),
                e.getMessage(),
                errorCode.getStatus(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(errorResponse);
    }

    /**
     * 입력값 검증 예외 처리 (@Valid 관련)
     * */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            HttpServletRequest request,
            MethodArgumentNotValidException e) {

        log.error("입력값 검증 실패: {}", e.getMessage());

        String message = e.getBindingResult()
                .getAllErrors()
                .getFirst()
                .getDefaultMessage();

        ErrorResponse errorResponse = ErrorResponse.of(
                GlobalErrorCode.INVALID_INPUT_VALUE.getCode(),
                message != null ? message : GlobalErrorCode.INVALID_INPUT_VALUE.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 바인딩 예외 처리
     * */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            HttpServletRequest request,
            BindException e) {

        log.error("바인딩 실패: {}", e.getMessage());

        String message = e.getBindingResult()
                .getAllErrors()
                .getFirst()
                .getDefaultMessage();

        ErrorResponse errorResponse = ErrorResponse.of(
                GlobalErrorCode.INVALID_INPUT_VALUE.getCode(),
                message != null ? message : GlobalErrorCode.INVALID_INPUT_VALUE.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

	/**
	 * 스프링 시큐리티 예외처리
	 * */
	@ExceptionHandler({
			LockedException.class,
			DisabledException.class,
			AccountExpiredException.class,
			CredentialsExpiredException.class,
			BadCredentialsException.class
	})
	public ResponseEntity<ErrorResponse> handleAuthenticationException(HttpServletRequest request, AuthenticationException e) {

		log.error("인증 예외 발생: {}", e.getMessage());

		ErrorCode errorCode = determineErrorCode(e);

		ErrorResponse errorResponse = ErrorResponse.of(
				errorCode,
				request.getRequestURI()
		);

		return ResponseEntity
				.status(errorCode.getStatus())
				.body(errorResponse);
	}

	private ErrorCode determineErrorCode(AuthenticationException e) {
		if (e instanceof LockedException) {
			return AuthErrorCode.ACCOUNT_LOCKED;
		} else if (e instanceof DisabledException) {
			return AuthErrorCode.ACCOUNT_DISABLED;
		} else if (e instanceof AccountExpiredException) {
			return AuthErrorCode.ACCOUNT_EXPIRED;
		} else if (e instanceof CredentialsExpiredException) {
			return AuthErrorCode.CREDENTIALS_EXPIRED;
		} else if (e instanceof BadCredentialsException) {
			return AuthErrorCode.INVALID_CREDENTIALS;
		}
		return AuthErrorCode.AUTHENTICATION_FAILED;
	}

    /**
     * 기타 모든 예외 처리
     * */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            HttpServletRequest request,
            Exception e) {

        log.error("처리되지 않은 예외 발생: {}", e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.of(
                GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);

    }
}
