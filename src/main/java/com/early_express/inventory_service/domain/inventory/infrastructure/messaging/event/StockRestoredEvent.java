package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 복원 이벤트
 * - 주문 취소 시 예약 해제 알림 (→ Order)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRestoredEvent {

    private String eventId;
    private String eventType;
    private String inventoryId;
    private String productId;
    private String hubId;
    private String orderId;
    private Integer restoredQuantity;
    private Integer currentQuantity;
    private LocalDateTime restoredAt;

    public static StockRestoredEvent of(
            String inventoryId,
            String productId,
            String hubId,
            String orderId,
            Integer restoredQuantity,
            Integer currentQuantity
    ) {
        return StockRestoredEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("STOCK_RESTORED")
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .orderId(orderId)
                .restoredQuantity(restoredQuantity)
                .currentQuantity(currentQuantity)
                .restoredAt(LocalDateTime.now())
                .build();
    }
}