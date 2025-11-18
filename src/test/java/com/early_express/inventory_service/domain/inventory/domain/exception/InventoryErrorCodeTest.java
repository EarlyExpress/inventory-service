package com.early_express.inventory_service.domain.inventory.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InventoryErrorCode 단위 테스트")
class InventoryErrorCodeTest {

    @Test
    @DisplayName("INVENTORY_NOT_FOUND 에러 코드 정보 확인")
    void inventoryNotFound_errorCode() {
        // given
        InventoryErrorCode errorCode = InventoryErrorCode.INVENTORY_NOT_FOUND;

        // when & then
        assertThat(errorCode.getCode()).isEqualTo("INVENTORY_001");
        assertThat(errorCode.getMessage()).isEqualTo("재고 정보를 찾을 수 없습니다.");
        assertThat(errorCode.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("INSUFFICIENT_AVAILABLE_STOCK 에러 코드 정보 확인")
    void insufficientAvailableStock_errorCode() {
        // given
        InventoryErrorCode errorCode = InventoryErrorCode.INSUFFICIENT_AVAILABLE_STOCK;

        // when & then
        assertThat(errorCode.getCode()).isEqualTo("INVENTORY_202");
        assertThat(errorCode.getMessage()).isEqualTo("판매 가능한 재고가 부족합니다.");
        assertThat(errorCode.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("UNAUTHORIZED_HUB_ACCESS 에러 코드 정보 확인")
    void unauthorizedHubAccess_errorCode() {
        // given
        InventoryErrorCode errorCode = InventoryErrorCode.UNAUTHORIZED_HUB_ACCESS;

        // when & then
        assertThat(errorCode.getCode()).isEqualTo("INVENTORY_302");
        assertThat(errorCode.getMessage()).isEqualTo("해당 허브에 대한 접근 권한이 없습니다.");
        assertThat(errorCode.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("OPTIMISTIC_LOCK_FAILURE 에러 코드 정보 확인")
    void optimisticLockFailure_errorCode() {
        // given
        InventoryErrorCode errorCode = InventoryErrorCode.OPTIMISTIC_LOCK_FAILURE;

        // when & then
        assertThat(errorCode.getCode()).isEqualTo("INVENTORY_404");
        assertThat(errorCode.getMessage()).isEqualTo("재고 업데이트 충돌이 발생했습니다. 다시 시도해주세요.");
        assertThat(errorCode.getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("PRODUCT_SERVICE_UNAVAILABLE 에러 코드 정보 확인")
    void productServiceUnavailable_errorCode() {
        // given
        InventoryErrorCode errorCode = InventoryErrorCode.PRODUCT_SERVICE_UNAVAILABLE;

        // when & then
        assertThat(errorCode.getCode()).isEqualTo("INVENTORY_601");
        assertThat(errorCode.getMessage()).isEqualTo("상품 서비스에 연결할 수 없습니다.");
        assertThat(errorCode.getStatus()).isEqualTo(503);
    }
}