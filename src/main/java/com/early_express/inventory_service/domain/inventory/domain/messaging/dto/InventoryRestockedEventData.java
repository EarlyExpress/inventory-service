package com.early_express.inventory_service.domain.inventory.domain.messaging.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 재입고 이벤트 데이터 (도메인 DTO)
 * Inventory Service → Product Service
 */
@Getter
@Builder
public class InventoryRestockedEventData {

    private final String inventoryId;
    private final String productId;
    private final String hubId;
    private final Integer restockedQuantity;
    private final Integer currentQuantity;
    private final LocalDateTime restockedAt;

    public static InventoryRestockedEventData of(
            String inventoryId,
            String productId,
            String hubId,
            Integer restockedQuantity,
            Integer currentQuantity) {

        return InventoryRestockedEventData.builder()
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .restockedQuantity(restockedQuantity)
                .currentQuantity(currentQuantity)
                .restockedAt(LocalDateTime.now())
                .build();
    }
}