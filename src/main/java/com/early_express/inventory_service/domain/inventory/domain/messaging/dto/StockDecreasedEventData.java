package com.early_express.inventory_service.domain.inventory.domain.messaging.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 재고 차감 이벤트 데이터 (도메인 DTO)
 * Inventory Service → Order Service
 */
@Getter
@Builder
public class StockDecreasedEventData {

    private final String inventoryId;
    private final String productId;
    private final String hubId;
    private final String orderId;
    private final Integer decreasedQuantity;
    private final Integer remainingQuantity;
    private final LocalDateTime decreasedAt;

    public static StockDecreasedEventData of(
            String inventoryId,
            String productId,
            String hubId,
            String orderId,
            Integer decreasedQuantity,
            Integer remainingQuantity) {

        return StockDecreasedEventData.builder()
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .orderId(orderId)
                .decreasedQuantity(decreasedQuantity)
                .remainingQuantity(remainingQuantity)
                .decreasedAt(LocalDateTime.now())
                .build();
    }
}