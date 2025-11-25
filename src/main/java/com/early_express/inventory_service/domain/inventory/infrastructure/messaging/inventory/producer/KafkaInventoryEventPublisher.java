package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.inventory.producer;

import com.early_express.inventory_service.domain.inventory.domain.messaging.InventoryEventPublisher;
import com.early_express.inventory_service.domain.inventory.domain.messaging.dto.*;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.inventory.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Inventory 이벤트 발행자 구현체 (Kafka Adapter)
 * 도메인 EventData → Kafka Event 변환 후 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaInventoryEventPublisher implements InventoryEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.inventory-created:inventory-created}")
    private String inventoryCreatedTopic;

    @Value("${spring.kafka.topic.inventory-low-stock:inventory-low-stock}")
    private String inventoryLowStockTopic;

    @Value("${spring.kafka.topic.inventory-restocked:inventory-restocked}")
    private String inventoryRestockedTopic;

    @Value("${spring.kafka.topic.inventory-reserved:inventory-reserved}")
    private String inventoryReservedTopic;

    @Value("${spring.kafka.topic.stock-decreased:stock-decreased}")
    private String stockDecreasedTopic;

    @Value("${spring.kafka.topic.stock-restored:stock-restored}")
    private String stockRestoredTopic;

    /**
     * 재고 생성 이벤트 발행
     */
    @Override
    public void publishInventoryCreated(InventoryCreatedEventData eventData) {
        log.info("InventoryCreated 이벤트 발행 준비 - inventoryId: {}, productId: {}",
                eventData.getInventoryId(), eventData.getProductId());

        InventoryCreatedEvent event = InventoryCreatedEvent.from(eventData);

        sendEvent(inventoryCreatedTopic, eventData.getProductId(), event, "InventoryCreated");
    }

    /**
     * 재고 부족 이벤트 발행
     * Inventory Service → Product Service
     */
    @Override
    public void publishInventoryLowStock(InventoryLowStockEventData eventData) {
        log.info("InventoryLowStock 이벤트 발행 준비 - productId: {}, hubId: {}, currentQuantity: {}",
                eventData.getProductId(), eventData.getHubId(), eventData.getCurrentQuantity());

        InventoryLowStockEvent event = InventoryLowStockEvent.from(eventData);

        sendEvent(inventoryLowStockTopic, eventData.getProductId(), event, "InventoryLowStock");
    }

    /**
     * 재입고 이벤트 발행
     * Inventory Service → Product Service
     */
    @Override
    public void publishInventoryRestocked(InventoryRestockedEventData eventData) {
        log.info("InventoryRestocked 이벤트 발행 준비 - productId: {}, hubId: {}, restockedQuantity: {}",
                eventData.getProductId(), eventData.getHubId(), eventData.getRestockedQuantity());

        InventoryRestockedEvent event = InventoryRestockedEvent.from(eventData);

        sendEvent(inventoryRestockedTopic, eventData.getProductId(), event, "InventoryRestocked");
    }

    /**
     * 재고 예약 이벤트 발행
     * Inventory Service → Order Service
     */
    @Override
    public void publishInventoryReserved(InventoryReservedEventData eventData) {
        log.info("InventoryReserved 이벤트 발행 준비 - orderId: {}, productId: {}, reservedQuantity: {}",
                eventData.getOrderId(), eventData.getProductId(), eventData.getReservedQuantity());

        InventoryReservedEvent event = InventoryReservedEvent.from(eventData);

        // orderId를 키로 사용 (Order Service 파티셔닝)
        sendEvent(inventoryReservedTopic, eventData.getOrderId(), event, "InventoryReserved");
    }

    /**
     * 재고 차감 이벤트 발행
     * Inventory Service → Order Service
     */
    @Override
    public void publishStockDecreased(StockDecreasedEventData eventData) {
        log.info("StockDecreased 이벤트 발행 준비 - orderId: {}, productId: {}, decreasedQuantity: {}",
                eventData.getOrderId(), eventData.getProductId(), eventData.getDecreasedQuantity());

        StockDecreasedEvent event = StockDecreasedEvent.from(eventData);

        // orderId를 키로 사용
        sendEvent(stockDecreasedTopic, eventData.getOrderId(), event, "StockDecreased");
    }

    /**
     * 재고 복원 이벤트 발행
     * Inventory Service → Order Service
     */
    @Override
    public void publishStockRestored(StockRestoredEventData eventData) {
        log.info("StockRestored 이벤트 발행 준비 - orderId: {}, productId: {}, restoredQuantity: {}",
                eventData.getOrderId(), eventData.getProductId(), eventData.getRestoredQuantity());

        StockRestoredEvent event = StockRestoredEvent.from(eventData);

        // orderId를 키로 사용
        sendEvent(stockRestoredTopic, eventData.getOrderId(), event, "StockRestored");
    }

    /**
     * 공통 이벤트 발행 헬퍼 메서드
     */
    private void sendEvent(String topic, String key, Object event, String eventName) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("{} 이벤트 발행 성공 - key: {}, topic: {}, partition: {}, offset: {}",
                        eventName,
                        key,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("{} 이벤트 발행 실패 - key: {}, error: {}",
                        eventName, key, ex.getMessage(), ex);
            }
        });
    }
}