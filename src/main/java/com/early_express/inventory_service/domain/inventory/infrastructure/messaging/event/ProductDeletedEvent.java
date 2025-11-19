package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품 삭제 이벤트 (Product 서비스에서 발행)
 * - Inventory 서비스에서 구독하여 해당 상품의 모든 재고 삭제
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDeletedEvent {

    private String eventId;
    private String eventType;
    private String productId;
    private String sellerId;
    private LocalDateTime deletedAt;
}