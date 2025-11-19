package com.early_express.inventory_service.domain.inventory.presentation.internal.dto.response;

import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 내부 API용 재고 응답 DTO (간소화)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalInventoryResponse {

    private String inventoryId;
    private String productId;
    private String hubId;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private boolean isOutOfStock;

    public static InternalInventoryResponse from(Inventory inventory) {
        return InternalInventoryResponse.builder()
                .inventoryId(inventory.getInventoryId())
                .productId(inventory.getProductId())
                .hubId(inventory.getHubId())
                .totalQuantity(inventory.getQuantityInHub().getValue())
                .availableQuantity(inventory.getAvailableQuantity().getValue())
                .reservedQuantity(inventory.getReservedQuantity().getValue())
                .isOutOfStock(inventory.isOutOfStock())
                .build();
    }
}