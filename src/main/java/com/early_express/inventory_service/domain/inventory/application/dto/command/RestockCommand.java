package com.early_express.inventory_service.domain.inventory.application.dto.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 재입고 Command (Application Layer)
 */
@Getter
@Builder
public class RestockCommand {

    private final String productId;
    private final String hubId;
    private final Integer quantity;
}