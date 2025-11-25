package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.product.consumer;

import com.early_express.inventory_service.domain.inventory.application.service.InventoryService;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.product.event.ProductCreatedEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.product.event.ProductDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Product 이벤트 Consumer
 * Product Service → Inventory Service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final InventoryService inventoryService;

    /**
     * 상품 생성 이벤트 처리
     * 각 허브별 초기 재고 생성
     * Topic: product-created
     */
    @KafkaListener(
            topics = "${spring.kafka.topic.product-created:product-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProductCreated(
            @Payload ProductCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("[Product] Created 이벤트 수신 - productId: {}, hubId: {}, partition: {}, offset: {}",
                event.getProductId(),
                event.getHubId(),
                partition,
                offset);

        try {
            // 특정 허브에 초기 재고 생성
            inventoryService.createInitialInventory(event.getProductId(), event.getHubId());

            // 수동 커밋
            ack.acknowledge();

            log.info("[Product] Created 이벤트 처리 완료 - productId: {}, hubId: {}",
                    event.getProductId(), event.getHubId());

        } catch (Exception e) {
            log.error("[Product] Created 이벤트 처리 실패 - productId: {}, hubId: {}, error: {}",
                    event.getProductId(), event.getHubId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 상품 삭제 이벤트 처리
     * 해당 상품의 모든 재고 삭제 (소프트 삭제)
     * Topic: product-deleted
     */
    @KafkaListener(
            topics = "${spring.kafka.topic.product-deleted:product-deleted}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProductDeleted(
            @Payload ProductDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        log.info("[Product] Deleted 이벤트 수신 - productId: {}, sellerId: {}, partition: {}, offset: {}",
                event.getProductId(),
                event.getSellerId(),
                partition,
                offset);

        try {
            // 해당 상품의 모든 재고 삭제
            inventoryService.deleteInventoriesByProduct(event.getProductId());

            // 수동 커밋
            ack.acknowledge();

            log.info("[Product] Deleted 이벤트 처리 완료 - productId: {}", event.getProductId());

        } catch (Exception e) {
            log.error("[Product] Deleted 이벤트 처리 실패 - productId: {}, error: {}",
                    event.getProductId(), e.getMessage(), e);
            throw e;
        }
    }
}