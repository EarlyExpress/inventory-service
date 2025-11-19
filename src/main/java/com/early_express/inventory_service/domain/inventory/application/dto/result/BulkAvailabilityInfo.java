package com.early_express.inventory_service.domain.inventory.application.dto.result;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 대량 재고 가용성 정보 (Application Layer)
 */
@Getter
@Builder
public class BulkAvailabilityInfo {

    private final String hubId;
    private final boolean allAvailable;
    private final List<ItemAvailabilityInfo> results;

    @Getter
    @Builder
    public static class ItemAvailabilityInfo {
        private final String productId;
        private final Integer requiredQuantity;
        private final Integer availableQuantity;
        private final boolean isAvailable;
    }
}