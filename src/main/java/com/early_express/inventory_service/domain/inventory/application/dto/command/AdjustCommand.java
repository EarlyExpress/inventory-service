package com.early_express.inventory_service.domain.inventory.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 재고 조정 Command (Application Layer)
 */
@Getter
@Builder
public class AdjustCommand {

    private final Integer adjustmentQuantity;
    private final String reason;
}