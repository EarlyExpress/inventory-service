package com.early_express.inventory_service.domain.inventory.presentation.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 조정 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustmentResponse {

    private String inventoryId;
    private String productId;
    private String hubId;
    private Integer previousQuantity;
    private Integer adjustmentQuantity;
    private Integer currentQuantity;
    private String reason;

    public static AdjustmentResponse of(
            String inventoryId,
            String productId,
            String hubId,
            Integer previousQuantity,
            Integer adjustmentQuantity,
            Integer currentQuantity,
            String reason
    ) {
        return AdjustmentResponse.builder()
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .previousQuantity(previousQuantity)
                .adjustmentQuantity(adjustmentQuantity)
                .currentQuantity(currentQuantity)
                .reason(reason)
                .build();
    }
}