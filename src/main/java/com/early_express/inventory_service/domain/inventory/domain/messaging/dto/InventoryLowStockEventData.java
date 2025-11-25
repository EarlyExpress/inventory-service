package com.early_express.inventory_service.domain.inventory.domain.messaging.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 재고 부족 이벤트 데이터 (도메인 DTO)
 * Inventory Service → Product Service
 */
@Getter
@Builder
public class InventoryLowStockEventData {

    private final String inventoryId;
    private final String productId;
    private final String hubId;
    private final Integer currentQuantity;
    private final Integer safetyStock;
    private final LocalDateTime detectedAt;

    public static InventoryLowStockEventData of(
            String inventoryId,
            String productId,
            String hubId,
            Integer currentQuantity,
            Integer safetyStock) {

        return InventoryLowStockEventData.builder()
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .currentQuantity(currentQuantity)
                .safetyStock(safetyStock)
                .detectedAt(LocalDateTime.now())
                .build();
    }
}