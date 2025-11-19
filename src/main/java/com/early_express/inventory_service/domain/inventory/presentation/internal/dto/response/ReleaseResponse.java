package com.early_express.inventory_service.domain.inventory.presentation.internal.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 재고 예약 해제 응답 DTO
 */
@Getter
@Builder
public class ReleaseResponse {

    private final String orderId;
    private final String productId;
    private final String hubId;
    private final Integer quantity;
    private final boolean released;

    /**
     * 정적 팩토리 메서드
     */
    public static ReleaseResponse of(
            String orderId,
            String productId,
            String hubId,
            Integer quantity,
            boolean released
    ) {
        return ReleaseResponse.builder()
                .orderId(orderId)
                .productId(productId)
                .hubId(hubId)
                .quantity(quantity)
                .released(released)
                .build();
    }
}