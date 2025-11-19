package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재입고 이벤트
 * - Product 서비스로 전송하여 품절 해제
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRestockedEvent {

    private String eventId;
    private String eventType;
    private String inventoryId;
    private String productId;
    private String hubId;
    private Integer restockedQuantity;
    private Integer currentQuantity;
    private LocalDateTime restockedAt;

    public static InventoryRestockedEvent of(
            String inventoryId,
            String productId,
            String hubId,
            Integer restockedQuantity,
            Integer currentQuantity
    ) {
        return InventoryRestockedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("INVENTORY_RESTOCKED")
                .inventoryId(inventoryId)
                .productId(productId)
                .hubId(hubId)
                .restockedQuantity(restockedQuantity)
                .currentQuantity(currentQuantity)
                .restockedAt(LocalDateTime.now())
                .build();
    }
}
