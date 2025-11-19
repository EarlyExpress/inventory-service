package com.early_express.inventory_service.domain.inventory.presentation.internal.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 출고 확정 응답 DTO
 */
@Getter
@Builder
public class ConfirmResponse {

    private final String orderId;
    private final String productId;
    private final String hubId;
    private final Integer quantity;
    private final boolean confirmed;

    /**
     * 정적 팩토리 메서드
     */
    public static ConfirmResponse of(
            String orderId,
            String productId,
            String hubId,
            Integer quantity,
            boolean confirmed
    ) {
        return ConfirmResponse.builder()
                .orderId(orderId)
                .productId(productId)
                .hubId(hubId)
                .quantity(quantity)
                .confirmed(confirmed)
                .build();
    }
}