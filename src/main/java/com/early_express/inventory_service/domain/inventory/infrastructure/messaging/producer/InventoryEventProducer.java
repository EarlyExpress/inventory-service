package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.producer;

import com.early_express.inventory_service.domain.inventory.domain.messaging.InventoryEventPublisher;
import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryCreatedEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryLowStockEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryRestockedEvent;
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
}
