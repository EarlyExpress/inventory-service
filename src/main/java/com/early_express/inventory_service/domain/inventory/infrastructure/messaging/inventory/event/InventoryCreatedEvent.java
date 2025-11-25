package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.inventory.event;

import com.early_express.inventory_service.domain.inventory.domain.messaging.dto.InventoryCreatedEventData;
import com.early_express.inventory_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 재고 생성 이벤트 (Kafka 메시지)
 * Topic: inventory-created
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class InventoryCreatedEvent extends BaseEvent {

    private String inventoryId;
    private String productId;
    private String hubId;
    private Integer quantity;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static InventoryCreatedEvent from(InventoryCreatedEventData data) {
        InventoryCreatedEvent event = InventoryCreatedEvent.builder()
                .inventoryId(data.getInventoryId())
                .productId(data.getProductId())
                .hubId(data.getHubId())
                .quantity(data.getQuantity())
                .createdAt(data.getCreatedAt())
                .build();

        event.initBaseEvent("INVENTORY_CREATED", "inventory-service");

        return event;
    }
}