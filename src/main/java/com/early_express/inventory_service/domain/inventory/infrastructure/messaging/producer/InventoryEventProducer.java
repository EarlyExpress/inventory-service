package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.producer;

import com.early_express.inventory_service.domain.inventory.domain.messaging.InventoryEventPublisher;
import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Inventory 이벤트 발행자 구현체 (어댑터)
 * - 도메인 인터페이스 구현
 * - KafkaTemplate 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventProducer implements InventoryEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.application.name:inventory-service}")
    private String applicationName;

    private static final String EVENTS_TOPIC_SUFFIX = "-events";

    /**
     * 재고 생성 이벤트 발행
     */
    @Override
    public void publishInventoryCreated(Inventory inventory) {
        String topic = applicationName + EVENTS_TOPIC_SUFFIX;

        InventoryCreatedEvent event = InventoryCreatedEvent.of(
                inventory.getInventoryId(),
                inventory.getProductId(),
                inventory.getHubId(),
                inventory.getQuantityInHub().getValue()
        );

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, inventory.getProductId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("InventoryCreatedEvent 발행 성공: inventoryId={}, productId={}, hubId={}, partition={}, offset={}",
                        inventory.getInventoryId(),
                        inventory.getProductId(),
                        inventory.getHubId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("InventoryCreatedEvent 발행 실패: inventoryId={}, error={}",
                        inventory.getInventoryId(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * 재고 부족 이벤트 발행
     */
    @Override
    public void publishInventoryLowStock(Inventory inventory) {
        String topic = applicationName + EVENTS_TOPIC_SUFFIX;

        InventoryLowStockEvent event = InventoryLowStockEvent.of(
                inventory.getInventoryId(),
                inventory.getProductId(),
                inventory.getHubId(),
                inventory.getQuantityInHub().getValue(),
                inventory.getSafetyStock().getValue()
        );

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, inventory.getProductId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("InventoryLowStockEvent 발행 성공: productId={}, hubId={}, currentQuantity={}, safetyStock={}, partition={}, offset={}",
                        inventory.getProductId(),
                        inventory.getHubId(),
                        inventory.getQuantityInHub().getValue(),
                        inventory.getSafetyStock().getValue(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("InventoryLowStockEvent 발행 실패: productId={}, error={}",
                        inventory.getProductId(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * 재입고 이벤트 발행
     */
    @Override
    public void publishInventoryRestocked(Inventory inventory, Integer restockedQuantity) {
        String topic = applicationName + EVENTS_TOPIC_SUFFIX;

        InventoryRestockedEvent event = InventoryRestockedEvent.of(
                inventory.getInventoryId(),
                inventory.getProductId(),
                inventory.getHubId(),
                restockedQuantity,
                inventory.getQuantityInHub().getValue()
        );

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, inventory.getProductId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("InventoryRestockedEvent 발행 성공: productId={}, hubId={}, restockedQuantity={}, currentQuantity={}, partition={}, offset={}",
                        inventory.getProductId(),
                        inventory.getHubId(),
                        restockedQuantity,
                        inventory.getQuantityInHub().getValue(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("InventoryRestockedEvent 발행 실패: productId={}, error={}",
                        inventory.getProductId(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * 재고 예약 이벤트 발행 (→ Order)
     */
    @Override
    public void publishInventoryReserved(Inventory inventory, String orderId, Integer reservedQuantity) {
        String topic = applicationName + EVENTS_TOPIC_SUFFIX;

        InventoryReservedEvent event = InventoryReservedEvent.of(
                inventory.getInventoryId(),
                inventory.getProductId(),
                inventory.getHubId(),
                orderId,
                reservedQuantity,
                inventory.getAvailableQuantity().getValue()
        );

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, orderId, event); // orderId를 키로 사용

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("InventoryReservedEvent 발행 성공: orderId={}, productId={}, reservedQuantity={}, partition={}, offset={}",
                        orderId,
                        inventory.getProductId(),
                        reservedQuantity,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("InventoryReservedEvent 발행 실패: orderId={}, error={}",
                        orderId, ex.getMessage(), ex);
            }
        });
    }

    /**
     * 재고 차감 이벤트 발행 (→ Order)
     */
    @Override
    public void publishStockDecreased(Inventory inventory, String orderId, Integer decreasedQuantity) {
        String topic = applicationName + EVENTS_TOPIC_SUFFIX;

        StockDecreasedEvent event = StockDecreasedEvent.of(
                inventory.getInventoryId(),
                inventory.getProductId(),
                inventory.getHubId(),
                orderId,
                decreasedQuantity,
                inventory.getQuantityInHub().getValue()
        );

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, orderId, event); // orderId를 키로 사용

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("StockDecreasedEvent 발행 성공: orderId={}, productId={}, decreasedQuantity={}, partition={}, offset={}",
                        orderId,
                        inventory.getProductId(),
                        decreasedQuantity,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("StockDecreasedEvent 발행 실패: orderId={}, error={}",
                        orderId, ex.getMessage(), ex);
            }
        });
    }

    /**
     * 재고 복원 이벤트 발행 (→ Order)
     */
    @Override
    public void publishStockRestored(Inventory inventory, String orderId, Integer restoredQuantity) {
        String topic = applicationName + EVENTS_TOPIC_SUFFIX;

        StockRestoredEvent event = StockRestoredEvent.of(
                inventory.getInventoryId(),
                inventory.getProductId(),
                inventory.getHubId(),
                orderId,
                restoredQuantity,
                inventory.getQuantityInHub().getValue()
        );

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, orderId, event); // orderId를 키로 사용

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("StockRestoredEvent 발행 성공: orderId={}, productId={}, restoredQuantity={}, partition={}, offset={}",
                        orderId,
                        inventory.getProductId(),
                        restoredQuantity,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("StockRestoredEvent 발행 실패: orderId={}, error={}",
                        orderId, ex.getMessage(), ex);
            }
        });
    }

    /**
     * 공통 이벤트 발행 헬퍼 메서드
     */
    private void publishEvent(String topic, String key, Object event, String eventName) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("{} 발행 성공: key={}, partition={}, offset={}",
                        eventName,
                        key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("{} 발행 실패: key={}, error={}",
                        eventName, key, ex.getMessage(), ex);
            }
        });
    }
}
