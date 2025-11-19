package com.early_express.inventory_service.domain.inventory.application.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 대량 재고 확인 Command (Application Layer)
 * - Service Layer에서 사용
 * - Presentation Layer와 독립적
 */
@Getter
@Builder
public class BulkCheckCommand {

    private final String hubId;
    private final List<CheckItem> items;

    @Getter
    @Builder
    public static class CheckItem {
        private final String productId;
        private final Integer quantity;
    }
}