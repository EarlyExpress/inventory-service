package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 부족 이벤트
 * - Product 서비스로 전송하여 품절 처리
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLowStockEvent {

    private String eventId;
    private String eventType;
    private String inventoryId;
    private String productId;
    private String hubId;
    private Integer currentQuantity;
    private Integer safetyStock;
    private LocalDateTime detectedAt;

    public static InventoryLowStockEvent of(
            String inventoryId,
            String productId,
            String hubId,
            Integer currentQuantity,
            Integer safetyStock
    ) {
        return InventoryLowStockEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("INVENTORY_LOW_STOCK")
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .currentQuantity(currentQuantity)
                .safetyStock(safetyStock)
                .detectedAt(LocalDateTime.now())
                .build();
    }
}
