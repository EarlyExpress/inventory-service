package com.early_express.inventory_service.domain.inventory.application.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 재고 예약 Command (Application Layer)
 */
@Getter
@Builder
public class ReservationCommand {

    private final String orderId;
    private final List<ReservationItem> items;

    @Getter
    @Builder
    public static class ReservationItem {
        private final String productId;
        private final String hubId;
        private final Integer quantity;
    }
}