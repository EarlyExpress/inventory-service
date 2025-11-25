package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.inventory.event;

import com.early_express.inventory_service.domain.inventory.domain.messaging.dto.StockRestoredEventData;
import com.early_express.inventory_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 재고 복원 이벤트 (Kafka 메시지)
 * Inventory Service → Order Service
 * Topic: stock-restored
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class StockRestoredEvent extends BaseEvent {

    private String inventoryId;
    private String productId;
    private String hubId;
    private String orderId;
    private Integer restoredQuantity;
    private Integer currentQuantity;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime restoredAt;

    public static StockRestoredEvent from(StockRestoredEventData data) {
        StockRestoredEvent event = StockRestoredEvent.builder()
                .inventoryId(data.getInventoryId())
                .productId(data.getProductId())
                .hubId(data.getHubId())
                .orderId(data.getOrderId())
                .restoredQuantity(data.getRestoredQuantity())
                .currentQuantity(data.getCurrentQuantity())
                .restoredAt(data.getRestoredAt())
                .build();

        event.initBaseEvent("STOCK_RESTORED", "inventory-service");

        return event;
    }
}