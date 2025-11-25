package com.early_express.inventory_service.domain.inventory.domain.messaging;

import com.early_express.inventory_service.domain.inventory.domain.messaging.dto.*;

/**
 * Inventory 이벤트 발행 포트 (도메인 인터페이스)
 * Infrastructure 계층에서 구현
 */
public interface InventoryEventPublisher {

    /**
     * 재고 생성 이벤트 발행
     *
     * @param eventData 재고 생성 이벤트 데이터
     */
    void publishInventoryCreated(InventoryCreatedEventData eventData);

    /**
     * 재고 부족 이벤트 발행
     * Inventory Service → Product Service
     *
     * @param eventData 재고 부족 이벤트 데이터
     */
    void publishInventoryLowStock(InventoryLowStockEventData eventData);

    /**
     * 재입고 이벤트 발행
     * Inventory Service → Product Service
     *
     * @param eventData 재입고 이벤트 데이터
     */
    void publishInventoryRestocked(InventoryRestockedEventData eventData);

    /**
     * 재고 예약 이벤트 발행
     * Inventory Service → Order Service
     *
     * @param eventData 재고 예약 이벤트 데이터
     */
    void publishInventoryReserved(InventoryReservedEventData eventData);

    /**
     * 재고 차감 이벤트 발행
     * Inventory Service → Order Service
     *
     * @param eventData 재고 차감 이벤트 데이터
     */
    void publishStockDecreased(StockDecreasedEventData eventData);

    /**
     * 재고 복원 이벤트 발행
     * Inventory Service → Order Service
     *
     * @param eventData 재고 복원 이벤트 데이터
     */
    void publishStockRestored(StockRestoredEventData eventData);
}