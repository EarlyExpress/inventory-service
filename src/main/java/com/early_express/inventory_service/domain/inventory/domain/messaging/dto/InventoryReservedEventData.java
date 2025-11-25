package com.early_express.inventory_service.domain.inventory.domain.messaging.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 재고 예약 이벤트 데이터 (도메인 DTO)
 * Inventory Service → Order Service
 */
@Getter
@Builder
public class InventoryReservedEventData {

    private final String inventoryId;
    private final String productId;
    private final String hubId;
    private final String orderId;
    private final Integer reservedQuantity;
    private final Integer availableQuantity;
    private final LocalDateTime reservedAt;

    public static InventoryReservedEventData of(
            String inventoryId,
            String productId,
            String hubId,
            String orderId,
            Integer reservedQuantity,
            Integer availableQuantity) {

        return InventoryReservedEventData.builder()
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .orderId(orderId)
                .reservedQuantity(reservedQuantity)
                .availableQuantity(availableQuantity)
                .reservedAt(LocalDateTime.now())
                .build();
    }
}