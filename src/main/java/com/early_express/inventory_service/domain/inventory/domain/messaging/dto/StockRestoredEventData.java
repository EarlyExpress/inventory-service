package com.early_express.inventory_service.domain.inventory.domain.messaging.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 재고 복원 이벤트 데이터 (도메인 DTO)
 * Inventory Service → Order Service
 */
@Getter
@Builder
public class StockRestoredEventData {

    private final String inventoryId;
    private final String productId;
    private final String hubId;
    private final String orderId;
    private final Integer restoredQuantity;
    private final Integer currentQuantity;
    private final LocalDateTime restoredAt;

    public static StockRestoredEventData of(
            String inventoryId,
            String productId,
            String hubId,
            String orderId,
            Integer restoredQuantity,
            Integer currentQuantity) {

        return StockRestoredEventData.builder()
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .orderId(orderId)
                .restoredQuantity(restoredQuantity)
                .currentQuantity(currentQuantity)
                .restoredAt(LocalDateTime.now())
                .build();
    }
}