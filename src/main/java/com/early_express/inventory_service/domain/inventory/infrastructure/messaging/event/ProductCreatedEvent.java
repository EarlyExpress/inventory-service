package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품 생성 이벤트 (Product 서비스에서 발행)
 * - Inventory 서비스에서 구독하여 초기 재고 생성
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent {

    private String eventId;
    private String eventType;
    private String productId;
    private String sellerId;
    private String name;
    private String hubId;
    private LocalDateTime createdAt;
}
