package com.early_express.inventory_service.domain.inventory.domain.model;

import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryErrorCode;
import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryException;
import com.early_express.inventory_service.domain.inventory.domain.model.vo.StockQuantity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Inventory 도메인 모델 (Aggregate Root)
 * - 순수 자바 객체 (JPA 의존성 없음)
 * - 재고 관리 비즈니스 로직 포함
 */
@Getter
public class Inventory {

    private final String inventoryId;
    private final String productId;
    private final String hubId;

    private StockQuantity quantityInHub;        // 허브 내 전체 수량
    private StockQuantity reservedQuantity;     // 예약된 수량 (주문 처리 중)
    private StockQuantity safetyStock;          // 안전 재고
    private StockQuantity reorderPoint;         // 재주문 시점

    private String location;                     // 허브 내 물리적 위치
    private LocalDateTime lastRestockedAt;       // 마지막 입고 시간

    private Long version;                        // 낙관적 락용 버전

    // Audit 필드 (BaseEntity와 매핑용)
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private boolean isDeleted;

    @Builder(access = AccessLevel.PRIVATE)
    private Inventory(
            String inventoryId,
            String productId,
            String hubId,
            StockQuantity quantityInHub,
            StockQuantity reservedQuantity,
            StockQuantity safetyStock,
            StockQuantity reorderPoint,
            String location,
            LocalDateTime lastRestockedAt,
            Long version,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy,
            LocalDateTime deletedAt,
            String deletedBy,
            boolean isDeleted
    ) {
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.hubId = hubId;
        this.quantityInHub = quantityInHub;
        this.reservedQuantity = reservedQuantity;
        this.safetyStock = safetyStock;
        this.reorderPoint = reorderPoint;
        this.location = location;
        this.lastRestockedAt = lastRestockedAt;
        this.version = version;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
        this.isDeleted = isDeleted;
    }

    /**
     * 새 재고 생성 (팩토리 메서드)
     */
    public static Inventory create(
            String inventoryId,
            String productId,
            String hubId,
            Integer initialQuantity,
            Integer safetyStock,
            String location
    ) {
        validateLocation(location);

        return Inventory.builder()
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .quantityInHub(StockQuantity.of(initialQuantity))
                .reservedQuantity(StockQuantity.zero())
                .safetyStock(StockQuantity.of(safetyStock))
                .reorderPoint(StockQuantity.of(safetyStock)) // 기본값: 안전재고와 동일
                .location(location)
                .lastRestockedAt(LocalDateTime.now())
                .version(0L)
                .isDeleted(false)
                .build();
    }

    /**
     * 기존 재고 재구성 (from Entity)
     */
    public static Inventory reconstruct(
            String inventoryId,
            String productId,
            String hubId,
            StockQuantity quantityInHub,
            StockQuantity reservedQuantity,
            StockQuantity safetyStock,
            StockQuantity reorderPoint,
            String location,
            LocalDateTime lastRestockedAt,
            Long version,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy,
            LocalDateTime deletedAt,
            String deletedBy,
            boolean isDeleted
    ) {
        return Inventory.builder()
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .quantityInHub(quantityInHub)
                .reservedQuantity(reservedQuantity)
                .safetyStock(safetyStock)
                .reorderPoint(reorderPoint)
                .location(location)
                .lastRestockedAt(lastRestockedAt)
                .version(version)
                .createdAt(createdAt)
                .createdBy(createdBy)
                .updatedAt(updatedAt)
                .updatedBy(updatedBy)
                .deletedAt(deletedAt)
                .deletedBy(deletedBy)
                .isDeleted(isDeleted)
                .build();
    }

    // ==================== 비즈니스 로직 ====================

    /**
     * 입고 (재고 증가)
     */
    public void restock(Integer quantity) {
        if (quantity <= 0) {
            throw new InventoryException(
                    InventoryErrorCode.INVALID_QUANTITY,
                    "입고 수량은 0보다 커야 합니다."
            );
        }

        this.quantityInHub = this.quantityInHub.increase(quantity);
        this.lastRestockedAt = LocalDateTime.now();
    }

    /**
     * 재고 예약 (주문 시)
     */
    public void reserve(Integer quantity) {
        StockQuantity available = getAvailableQuantity();

        if (available.isLessThan(StockQuantity.of(quantity))) {
            throw new InventoryException(
                    InventoryErrorCode.INSUFFICIENT_AVAILABLE_STOCK,
                    String.format("요청 수량: %d, 가용 재고: %d", quantity, available.getValue())
            );
        }

        this.reservedQuantity = this.reservedQuantity.increase(quantity);
    }

    /**
     * 예약 해제 (주문 취소 시)
     */
    public void releaseReservation(Integer quantity) {
        if (this.reservedQuantity.isLessThan(StockQuantity.of(quantity))) {
            throw new InventoryException(
                    InventoryErrorCode.CANNOT_RELEASE_MORE_THAN_RESERVED,
                    String.format("예약 수량: %d, 해제 요청: %d",
                            this.reservedQuantity.getValue(), quantity)
            );
        }

        this.reservedQuantity = this.reservedQuantity.decrease(quantity);
    }

    /**
     * 출고 확정 (배송 시작 시)
     */
    public void confirmShipment(Integer quantity) {
        // 예약에서 차감
        if (this.reservedQuantity.isLessThan(StockQuantity.of(quantity))) {
            throw new InventoryException(
                    InventoryErrorCode.CANNOT_RELEASE_MORE_THAN_RESERVED,
                    "예약된 수량보다 많이 출고할 수 없습니다."
            );
        }

        this.reservedQuantity = this.reservedQuantity.decrease(quantity);
        this.quantityInHub = this.quantityInHub.decrease(quantity);
    }

    /**
     * 재고 조정 (실사 후)
     */
    public void adjust(Integer newQuantity, String reason) {
        if (newQuantity < 0) {
            throw new InventoryException(
                    InventoryErrorCode.INVALID_QUANTITY,
                    "조정 후 수량은 0 이상이어야 합니다."
            );
        }

        this.quantityInHub = StockQuantity.of(newQuantity);
    }

    /**
     * 안전 재고 설정
     */
    public void setSafetyStock(Integer safetyStock) {
        this.safetyStock = StockQuantity.of(safetyStock);
    }

    /**
     * 재주문 시점 설정
     */
    public void setReorderPoint(Integer reorderPoint) {
        StockQuantity newReorderPoint = StockQuantity.of(reorderPoint);

        if (newReorderPoint.isLessThan(this.safetyStock)) {
            throw new InventoryException(
                    InventoryErrorCode.INVALID_REORDER_POINT,
                    "재주문 시점은 안전 재고보다 크거나 같아야 합니다."
            );
        }

        this.reorderPoint = newReorderPoint;
    }

    /**
     * 위치 변경
     */
    public void changeLocation(String newLocation) {
        validateLocation(newLocation);
        this.location = newLocation;
    }

    /**
     * 소프트 삭제
     */
    public void delete(String deletedBy) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    /**
     * 소프트 삭제 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }

    // ==================== 조회 메서드 ====================

    /**
     * 판매 가능한 수량 (전체 - 예약)
     */
    public StockQuantity getAvailableQuantity() {
        return this.quantityInHub.decrease(this.reservedQuantity.getValue());
    }

    /**
     * 안전 재고 이하인지 확인
     */
    public boolean isBelowSafetyStock() {
        return this.quantityInHub.isLessThanOrEqual(this.safetyStock);
    }

    /**
     * 재주문 시점 도달 여부
     */
    public boolean needsReorder() {
        return this.quantityInHub.isLessThanOrEqual(this.reorderPoint);
    }

    /**
     * 재고 없음
     */
    public boolean isOutOfStock() {
        return getAvailableQuantity().isZero();
    }

    // ==================== 검증 로직 ====================

    private static void validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new InventoryException(
                    InventoryErrorCode.INVALID_LOCATION_FORMAT,
                    "위치 정보는 필수입니다."
            );
        }

        // 위치 형식 검증 (예: A-1-3)
        if (!location.matches("^[A-Z]-\\d+-\\d+$")) {
            throw new InventoryException(
                    InventoryErrorCode.INVALID_LOCATION_FORMAT,
                    "위치 형식이 올바르지 않습니다. (예: A-1-3)"
            );
        }
    }
}
