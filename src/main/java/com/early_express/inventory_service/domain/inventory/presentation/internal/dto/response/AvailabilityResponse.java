package com.early_express.inventory_service.domain.inventory.presentation.internal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 가용성 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {

    private String productId;
    private String hubId;
    private boolean isAvailable;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer totalQuantity;

    public static AvailabilityResponse of(
            String productId,
            String hubId,
            boolean isAvailable,
            Integer availableQuantity,
            Integer reservedQuantity,
            Integer totalQuantity
    ) {
        return AvailabilityResponse.builder()
                .productId(productId)
                .hubId(hubId)
                .isAvailable(isAvailable)
                .availableQuantity(availableQuantity)
                .reservedQuantity(reservedQuantity)
                .totalQuantity(totalQuantity)
                .build();
    }
}