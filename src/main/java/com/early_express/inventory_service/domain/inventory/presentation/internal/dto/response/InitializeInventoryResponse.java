package com.early_express.inventory_service.domain.inventory.presentation.internal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 초기 재고 생성 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeInventoryResponse {

    private String productId;
    private List<HubInventory> inventories;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HubInventory {
        private String inventoryId;
        private String hubId;
        private Integer totalQuantity;
    }

    public static InitializeInventoryResponse of(
            String productId,
            List<HubInventory> inventories
    ) {
        return InitializeInventoryResponse.builder()
                .productId(productId)
                .inventories(inventories)
                .build();
    }
}