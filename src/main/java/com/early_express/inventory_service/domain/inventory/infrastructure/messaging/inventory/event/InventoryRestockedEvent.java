package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.inventory.event;

import com.early_express.inventory_service.domain.inventory.domain.messaging.dto.InventoryRestockedEventData;
import com.early_express.inventory_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 재입고 이벤트 (Kafka 메시지)
 * Inventory Service → Product Service
 * Topic: inventory-restocked
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class InventoryRestockedEvent extends BaseEvent {

    private String inventoryId;
    private String productId;
    private String hubId;
    private Integer restockedQuantity;
    private Integer currentQuantity;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime restockedAt;

    public static InventoryRestockedEvent from(InventoryRestockedEventData data) {
        InventoryRestockedEvent event = InventoryRestockedEvent.builder()
                .inventoryId(data.getInventoryId())
                .productId(data.getProductId())
                .hubId(data.getHubId())
                .restockedQuantity(data.getRestockedQuantity())
                .currentQuantity(data.getCurrentQuantity())
                .restockedAt(data.getRestockedAt())
                .build();

        event.initBaseEvent("INVENTORY_RESTOCKED", "inventory-service");

        return event;
    }
}