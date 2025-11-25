package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.inventory.event;

import com.early_express.inventory_service.domain.inventory.domain.messaging.dto.InventoryReservedEventData;
import com.early_express.inventory_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 재고 예약 이벤트 (Kafka 메시지)
 * Inventory Service → Order Service
 * Topic: inventory-reserved
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class InventoryReservedEvent extends BaseEvent {

    private String inventoryId;
    private String productId;
    private String hubId;
    private String orderId;
    private Integer reservedQuantity;
    private Integer availableQuantity;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime reservedAt;

    public static InventoryReservedEvent from(InventoryReservedEventData data) {
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .inventoryId(data.getInventoryId())
                .productId(data.getProductId())
                .hubId(data.getHubId())
                .orderId(data.getOrderId())
                .reservedQuantity(data.getReservedQuantity())
                .availableQuantity(data.getAvailableQuantity())
                .reservedAt(data.getReservedAt())
                .build();

        event.initBaseEvent("INVENTORY_RESERVED", "inventory-service");

        return event;
    }
}