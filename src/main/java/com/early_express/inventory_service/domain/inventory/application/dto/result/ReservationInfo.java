package com.early_express.inventory_service.domain.inventory.application.dto.result;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 재고 예약 결과 정보 (Application Layer)
 */
@Getter
@Builder
public class ReservationInfo {

    private final String orderId;
    private final boolean allSuccess;
    private final List<ReservedItemInfo> reservedItems;

    @Getter
    @Builder
    public static class ReservedItemInfo {
        private final String productId;
        private final String hubId;
        private final Integer quantity;
        private final boolean success;
        private final String errorMessage;
    }
}