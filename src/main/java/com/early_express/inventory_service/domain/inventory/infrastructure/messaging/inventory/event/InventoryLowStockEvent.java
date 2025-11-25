package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.inventory.event;

import com.early_express.inventory_service.domain.inventory.domain.messaging.dto.InventoryLowStockEventData;
import com.early_express.inventory_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 재고 부족 이벤트 (Kafka 메시지)
 * Inventory Service → Product Service
 * Topic: inventory-low-stock
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class InventoryLowStockEvent extends BaseEvent {

    private String inventoryId;
    private String productId;
    private String hubId;
    private Integer currentQuantity;
    private Integer safetyStock;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime detectedAt;

    public static InventoryLowStockEvent from(InventoryLowStockEventData data) {
        InventoryLowStockEvent event = InventoryLowStockEvent.builder()
                .inventoryId(data.getInventoryId())
                .productId(data.getProductId())
                .hubId(data.getHubId())
                .currentQuantity(data.getCurrentQuantity())
                .safetyStock(data.getSafetyStock())
                .detectedAt(data.getDetectedAt())
                .build();

        event.initBaseEvent("INVENTORY_LOW_STOCK", "inventory-service");

        return event;
    }
}