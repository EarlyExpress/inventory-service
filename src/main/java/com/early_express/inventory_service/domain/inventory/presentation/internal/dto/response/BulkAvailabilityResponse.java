package com.early_express.inventory_service.domain.inventory.presentation.internal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 대량 재고 가용성 확인 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAvailabilityResponse {

    private String hubId;
    private boolean allAvailable;
    private List<ItemAvailability> results;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemAvailability {
        private String productId;
        private Integer requiredQuantity;
        private Integer availableQuantity;
        private boolean isAvailable;
    }

    public static BulkAvailabilityResponse of(
            String hubId,
            boolean allAvailable,
            List<ItemAvailability> results
    ) {
        return BulkAvailabilityResponse.builder()
                .hubId(hubId)
                .allAvailable(allAvailable)
                .results(results)
                .build();
    }
}