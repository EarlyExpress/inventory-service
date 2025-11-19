package com.early_express.inventory_service.domain.inventory.application.dto.result;

import lombok.Builder;
import lombok.Getter;

/**
 * 재고 가용성 정보 (Application Layer)
 * - Service에서 반환
 * - Controller에서 Response DTO로 변환
 */
@Getter
@Builder
public class AvailabilityInfo {

    private final String productId;
    private final String hubId;
    private final boolean isAvailable;
    private final Integer availableQuantity;
    private final Integer reservedQuantity;
    private final Integer totalQuantity;
    private final String error;
}