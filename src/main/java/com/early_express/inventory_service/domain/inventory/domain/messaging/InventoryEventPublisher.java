package com.early_express.inventory_service.domain.inventory.domain.messaging;

import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;

/**
 * Inventory 이벤트 발행 포트 (도메인 인터페이스)
 * - Infrastructure 계층에서 구현
 */
public interface InventoryEventPublisher {

    /**
     * 재고 생성 이벤트 발행
     */
    void publishInventoryCreated(Inventory inventory);

    /**
     * 재고 부족 이벤트 발행
     */
    void publishInventoryLowStock(Inventory inventory);

    /**
     * 재입고 이벤트 발행
     */
    void publishInventoryRestocked(Inventory inventory, Integer restockedQuantity);

    /**
     * 재고 예약 이벤트 발행 (→ Order)
     */
    void publishInventoryReserved(Inventory inventory, String orderId, Integer reservedQuantity);

    /**
     * 재고 차감 이벤트 발행 (→ Order)
     */
    void publishStockDecreased(Inventory inventory, String orderId, Integer decreasedQuantity);

    /**
     * 재고 복원 이벤트 발행 (→ Order)
     */
    void publishStockRestored(Inventory inventory, String orderId, Integer restoredQuantity);
}