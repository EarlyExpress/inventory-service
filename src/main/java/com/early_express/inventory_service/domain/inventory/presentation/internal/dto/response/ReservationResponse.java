package com.early_express.inventory_service.domain.inventory.presentation.internal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 재고 예약 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private String reservationId;  // UUID 생성
    private String orderId;
    private boolean allSuccess;
    private List<ReservedItem> reservedItems;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservedItem {
        private String productId;
        private String hubId;
        private Integer quantity;
        private boolean success;
        private String errorMessage;
    }

    public static ReservationResponse of(
            String reservationId,
            String orderId,
            boolean allSuccess,
            List<ReservedItem> reservedItems
    ) {
        return ReservationResponse.builder()
                .reservationId(reservationId)
                .orderId(orderId)
                .allSuccess(allSuccess)
                .reservedItems(reservedItems)
                .build();
    }
}