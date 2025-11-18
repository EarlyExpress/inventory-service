package com.early_express.inventory_service.domain.inventory.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InventoryException 단위 테스트")
class InventoryExceptionTest {

    @Test
    @DisplayName("ErrorCode만으로 예외 생성")
    void createException_withErrorCodeOnly() {
        // given
        InventoryErrorCode errorCode = InventoryErrorCode.INVENTORY_NOT_FOUND;

        // when
        InventoryException exception = new InventoryException(errorCode);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo("재고 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("ErrorCode와 커스텀 메시지로 예외 생성")
    void createException_withErrorCodeAndCustomMessage() {
        // given
        InventoryErrorCode errorCode = InventoryErrorCode.INSUFFICIENT_AVAILABLE_STOCK;
        String customMessage = "요청 수량: 100, 가용 재고: 50";

        // when
        InventoryException exception = new InventoryException(errorCode, customMessage);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
    }

    @Test
    @DisplayName("ErrorCode와 원인 예외로 예외 생성")
    void createException_withErrorCodeAndCause() {
        // given
        InventoryErrorCode errorCode = InventoryErrorCode.RESERVATION_FAILED;
        RuntimeException cause = new RuntimeException("Transaction rollback");

        // when
        InventoryException exception = new InventoryException(errorCode, cause);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo("재고 예약에 실패했습니다.");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("ErrorCode, 커스텀 메시지, 원인 예외로 예외 생성")
    void createException_withErrorCodeAndMessageAndCause() {
        // given
        InventoryErrorCode errorCode = InventoryErrorCode.OPTIMISTIC_LOCK_FAILURE;
        String customMessage = "다른 사용자가 동시에 재고를 수정했습니다.";
        RuntimeException cause = new RuntimeException("Version mismatch");

        // when
        InventoryException exception = new InventoryException(errorCode, customMessage, cause);

        // then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(customMessage);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("HTTP 상태 코드가 올바르게 매핑되는지 확인")
    void errorCode_httpStatusMapping() {
        // given & when & then
        assertThat(new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND)
                .getErrorCode().getStatus()).isEqualTo(404);

        assertThat(new InventoryException(InventoryErrorCode.INSUFFICIENT_AVAILABLE_STOCK)
                .getErrorCode().getStatus()).isEqualTo(400);

        assertThat(new InventoryException(InventoryErrorCode.UNAUTHORIZED_HUB_ACCESS)
                .getErrorCode().getStatus()).isEqualTo(403);

        assertThat(new InventoryException(InventoryErrorCode.OPTIMISTIC_LOCK_FAILURE)
                .getErrorCode().getStatus()).isEqualTo(409);

        assertThat(new InventoryException(InventoryErrorCode.RESERVATION_FAILED)
                .getErrorCode().getStatus()).isEqualTo(500);

        assertThat(new InventoryException(InventoryErrorCode.PRODUCT_SERVICE_UNAVAILABLE)
                .getErrorCode().getStatus()).isEqualTo(503);
    }
}