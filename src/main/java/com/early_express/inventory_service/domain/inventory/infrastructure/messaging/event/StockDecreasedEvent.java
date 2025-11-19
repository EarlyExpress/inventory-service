package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 차감 이벤트
 * - 주문 확정 시 재고 차감 알림 (→ Order)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDecreasedEvent {

    private String eventId;
    private String eventType;
    private String inventoryId;
    private String productId;
    private String hubId;
    private String orderId;
    private Integer decreasedQuantity;
    private Integer remainingQuantity;
    private LocalDateTime decreasedAt;

    public static StockDecreasedEvent of(
            String inventoryId,
            String productId,
            String hubId,
            String orderId,
            Integer decreasedQuantity,
            Integer remainingQuantity
    ) {
        return StockDecreasedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("STOCK_DECREASED")
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .orderId(orderId)
                .decreasedQuantity(decreasedQuantity)
                .remainingQuantity(remainingQuantity)
                .decreasedAt(LocalDateTime.now())
                .build();
    }
}