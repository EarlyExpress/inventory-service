package com.early_express.inventory_service.domain.inventory.domain.messaging.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 재고 생성 이벤트 데이터 (도메인 DTO)
 */
@Getter
@Builder
public class InventoryCreatedEventData {

    private final String inventoryId;
    private final String productId;
    private final String hubId;
    private final Integer quantity;
    private final LocalDateTime createdAt;

    public static InventoryCreatedEventData of(
            String inventoryId,
            String productId,
            String hubId,
            Integer quantity) {

        return InventoryCreatedEventData.builder()
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .quantity(quantity)
                .createdAt(LocalDateTime.now())
                .build();
    }
}