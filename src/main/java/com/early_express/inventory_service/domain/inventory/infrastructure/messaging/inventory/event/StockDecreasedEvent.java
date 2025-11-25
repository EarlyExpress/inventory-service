package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.inventory.event;

import com.early_express.inventory_service.domain.inventory.domain.messaging.dto.StockDecreasedEventData;
import com.early_express.inventory_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 재고 차감 이벤트 (Kafka 메시지)
 * Inventory Service → Order Service
 * Topic: stock-decreased
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public class StockDecreasedEvent extends BaseEvent {

    private String inventoryId;
    private String productId;
    private String hubId;
    private String orderId;
    private Integer decreasedQuantity;
    private Integer remainingQuantity;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime decreasedAt;

    public static StockDecreasedEvent from(StockDecreasedEventData data) {
        StockDecreasedEvent event = StockDecreasedEvent.builder()
                .inventoryId(data.getInventoryId())
                .productId(data.getProductId())
                .hubId(data.getHubId())
                .orderId(data.getOrderId())
                .decreasedQuantity(data.getDecreasedQuantity())
                .remainingQuantity(data.getRemainingQuantity())
                .decreasedAt(data.getDecreasedAt())
                .build();

        event.initBaseEvent("STOCK_DECREASED", "inventory-service");

        return event;
    }
}