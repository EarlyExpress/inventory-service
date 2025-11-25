package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.product.event;

import com.early_express.inventory_service.global.infrastructure.event.base.BaseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 상품 삭제 이벤트 (수신용)
 * Product Service → Inventory Service
 * Topic: product-deleted
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDeletedEvent extends BaseEvent {

    private String productId;
    private String sellerId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;
}