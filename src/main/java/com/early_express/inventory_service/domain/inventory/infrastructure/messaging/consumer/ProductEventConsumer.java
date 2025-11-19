package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.consumer;

import com.early_express.inventory_service.domain.inventory.application.service.InventoryService;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.ProductCreatedEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.ProductDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Product 이벤트 구독자
 * - Product 서비스에서 발행하는 이벤트 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final InventoryService inventoryService;

    /**
     * 상품 생성 이벤트 처리
     * - 각 허브별 초기 재고 생성
     */
    @KafkaListener(
            topics = "product-service-events",
            groupId = "inventory-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProductCreatedEvent(
            @Payload ProductCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("ProductCreatedEvent 수신: productId={}, sellerId={}, name={}, hubId={}, partition={}, offset={}",
                    event.getProductId(),
                    event.getSellerId(),
                    event.getName(),
                    event.getHubId(),
                    partition,
                    offset);

            // 특정 허브에만 초기 재고 생성
            inventoryService.createInitialInventory(event.getProductId(), event.getHubId());

            // 수동 커밋
            acknowledgment.acknowledge();

            log.info("ProductCreatedEvent 처리 완료: productId={}, hubId={}",
                    event.getProductId(), event.getHubId());

        } catch (Exception e) {
            log.error("ProductCreatedEvent 처리 실패: productId={}, hubId={}, error={}",
                    event.getProductId(), event.getHubId(), e.getMessage(), e);
            // 예외 발생 시 재시도
            throw new RuntimeException("이벤트 처리 실패", e);
        }
    }

    /**
     * 상품 삭제 이벤트 처리
     * - 해당 상품의 모든 재고 삭제 (소프트 삭제)
     */
    @KafkaListener(
            topics = "product-service-events",
            groupId = "inventory-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProductDeletedEvent(
            @Payload ProductDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("ProductDeletedEvent 수신: productId={}, sellerId={}, partition={}, offset={}",
                    event.getProductId(),
                    event.getSellerId(),
                    partition,
                    offset);

            // 해당 상품의 모든 재고 삭제
            inventoryService.deleteInventoriesByProduct(event.getProductId());

            // 수동 커밋
            acknowledgment.acknowledge();

            log.info("ProductDeletedEvent 처리 완료: productId={}", event.getProductId());

        } catch (Exception e) {
            log.error("ProductDeletedEvent 처리 실패: productId={}, error={}",
                    event.getProductId(), e.getMessage(), e);
            throw new RuntimeException("이벤트 처리 실패", e);
        }
    }
}
