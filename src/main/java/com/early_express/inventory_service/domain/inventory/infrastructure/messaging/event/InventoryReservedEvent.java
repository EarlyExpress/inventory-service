package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 예약 이벤트
 * - 주문 시 재고 예약 성공 알림 (→ Order)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEvent {

    private String eventId;
    private String eventType;
    private String inventoryId;
    private String productId;
    private String hubId;
    private String orderId;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private LocalDateTime reservedAt;

    public static InventoryReservedEvent of(
            String inventoryId,
            String productId,
            String hubId,
            String orderId,
            Integer reservedQuantity,
            Integer availableQuantity
    ) {
        return InventoryReservedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("INVENTORY_RESERVED")
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .orderId(orderId)
                .reservedQuantity(reservedQuantity)
                .availableQuantity(availableQuantity)
                .reservedAt(LocalDateTime.now())
                .build();
    }
}