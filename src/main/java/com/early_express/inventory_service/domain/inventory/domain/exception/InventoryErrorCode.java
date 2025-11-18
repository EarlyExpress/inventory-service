package com.early_express.inventory_service.domain.inventory.domain.exception;

import com.early_express.inventory_service.global.presentation.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Inventory 도메인 전용 에러 코드
 * 코드 네이밍: INVENTORY_XXX
 * HTTP 상태: 주로 400(Bad Request), 404(Not Found), 409(Conflict)
 */
@Getter
@RequiredArgsConstructor
public enum InventoryErrorCode implements ErrorCode {

    // ===== 404 Not Found =====
    INVENTORY_NOT_FOUND("INVENTORY_001", "재고 정보를 찾을 수 없습니다.", 404),
    MOVEMENT_NOT_FOUND("INVENTORY_002", "재고 변동 이력을 찾을 수 없습니다.", 404),
    HUB_NOT_FOUND("INVENTORY_003", "허브 정보를 찾을 수 없습니다.", 404),

    // ===== 400 Bad Request - Validation =====
    INVALID_QUANTITY("INVENTORY_101", "수량은 0 이상이어야 합니다.", 400),
    INVALID_RESERVED_QUANTITY("INVENTORY_102", "예약 수량이 올바르지 않습니다.", 400),
    INVALID_SAFETY_STOCK("INVENTORY_103", "안전 재고는 0 이상이어야 합니다.", 400),
    INVALID_REORDER_POINT("INVENTORY_104", "재주문 시점은 안전 재고보다 크거나 같아야 합니다.", 400),
    INVALID_MOVEMENT_QUANTITY("INVENTORY_105", "변동 수량이 올바르지 않습니다.", 400),
    INVALID_LOCATION_FORMAT("INVENTORY_106", "위치 형식이 올바르지 않습니다. (예: A-1-3)", 400),

    // ===== 400 Bad Request - Business Logic =====
    INSUFFICIENT_STOCK("INVENTORY_201", "재고가 부족합니다.", 400),
    INSUFFICIENT_AVAILABLE_STOCK("INVENTORY_202", "판매 가능한 재고가 부족합니다.", 400),
    NEGATIVE_STOCK_NOT_ALLOWED("INVENTORY_203", "재고는 음수가 될 수 없습니다.", 400),
    EXCEED_AVAILABLE_QUANTITY("INVENTORY_204", "예약 가능한 수량을 초과했습니다.", 400),
    ALREADY_RESERVED("INVENTORY_205", "이미 예약된 재고입니다.", 400),
    RESERVATION_NOT_FOUND("INVENTORY_206", "예약 정보를 찾을 수 없습니다.", 400),
    CANNOT_RELEASE_MORE_THAN_RESERVED("INVENTORY_207", "예약된 수량보다 많이 해제할 수 없습니다.", 400),
    BELOW_SAFETY_STOCK("INVENTORY_208", "안전 재고 수준 이하입니다.", 400),
    REORDER_POINT_REACHED("INVENTORY_209", "재주문 시점에 도달했습니다.", 400),

    // ===== 403 Forbidden =====
    CANNOT_MODIFY_RESERVED_INVENTORY("INVENTORY_301", "예약된 재고는 수정할 수 없습니다.", 403),
    UNAUTHORIZED_HUB_ACCESS("INVENTORY_302", "해당 허브에 대한 접근 권한이 없습니다.", 403),

    // ===== 409 Conflict =====
    INVENTORY_ALREADY_EXISTS("INVENTORY_401", "해당 상품의 재고가 이미 존재합니다.", 409),
    DUPLICATE_LOCATION("INVENTORY_402", "중복된 위치입니다.", 409),
    CONCURRENT_MODIFICATION("INVENTORY_403", "동시 수정이 발생했습니다. 다시 시도해주세요.", 409),
    OPTIMISTIC_LOCK_FAILURE("INVENTORY_404", "재고 업데이트 충돌이 발생했습니다. 다시 시도해주세요.", 409),

    // ===== 500 Internal Server Error =====
    INVENTORY_CREATION_FAILED("INVENTORY_501", "재고 생성에 실패했습니다.", 500),
    INVENTORY_UPDATE_FAILED("INVENTORY_502", "재고 수정에 실패했습니다.", 500),
    INVENTORY_DELETE_FAILED("INVENTORY_503", "재고 삭제에 실패했습니다.", 500),
    MOVEMENT_RECORD_FAILED("INVENTORY_504", "재고 변동 이력 기록에 실패했습니다.", 500),
    RESERVATION_FAILED("INVENTORY_505", "재고 예약에 실패했습니다.", 500),
    RELEASE_FAILED("INVENTORY_506", "재고 예약 해제에 실패했습니다.", 500),

    // ===== 503 Service Unavailable - External Service =====
    PRODUCT_SERVICE_UNAVAILABLE("INVENTORY_601", "상품 서비스에 연결할 수 없습니다.", 503),
    HUB_SERVICE_UNAVAILABLE("INVENTORY_602", "허브 서비스에 연결할 수 없습니다.", 503);

    private final String code;
    private final String message;
    private final int status;
}
