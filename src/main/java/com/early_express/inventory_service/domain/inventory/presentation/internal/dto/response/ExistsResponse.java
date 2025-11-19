package com.early_express.inventory_service.domain.inventory.presentation.internal.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 재고 존재 여부 확인 응답 DTO
 */
@Getter
@Builder
public class ExistsResponse {

    private final String inventoryId;
    private final boolean exists;

    /**
     * 정적 팩토리 메서드
     */
    public static ExistsResponse of(String inventoryId, boolean exists) {
        return ExistsResponse.builder()
                .inventoryId(inventoryId)
                .exists(exists)
                .build();
    }
}