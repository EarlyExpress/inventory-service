package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 생성 이벤트
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCreatedEvent {

    private String eventId;
    private String eventType;
    private String inventoryId;
    private String productId;
    private String hubId;
    private Integer quantity;
    private LocalDateTime createdAt;

    public static InventoryCreatedEvent of(
            String inventoryId,
            String productId,
            String hubId,
            Integer quantity
    ) {
        return InventoryCreatedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("INVENTORY_CREATED")
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .quantity(quantity)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
