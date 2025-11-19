package com.early_express.inventory_service.domain.inventory.presentation.web.dto.response;

import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private String inventoryId;
    private String productId;
    private String hubId;
    private Integer totalQuantity;           // 허브 내 전체 수량
    private Integer availableQuantity;       // 판매 가능 수량 (전체 - 예약)
    private Integer reservedQuantity;        // 예약 수량
    private Integer safetyStock;             // 안전 재고
    private String location;                 // 허브 내 위치
    private boolean isOutOfStock;            // 품절 여부
    private boolean isBelowSafetyStock;      // 안전 재고 이하 여부
    private LocalDateTime lastRestockedAt;   // 마지막 입고 시간
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InventoryResponse from(Inventory inventory) {
        return InventoryResponse.builder()
                .inventoryId(inventory.getInventoryId())
                .productId(inventory.getProductId())
                .hubId(inventory.getHubId())
                .totalQuantity(inventory.getQuantityInHub().getValue())
                .availableQuantity(inventory.getAvailableQuantity().getValue())
                .reservedQuantity(inventory.getReservedQuantity().getValue())
                .safetyStock(inventory.getSafetyStock().getValue())
                .location(inventory.getLocation())
                .isOutOfStock(inventory.isOutOfStock())
                .isBelowSafetyStock(inventory.isBelowSafetyStock())
                .lastRestockedAt(inventory.getLastRestockedAt())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}