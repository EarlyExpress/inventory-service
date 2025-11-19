package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.producer;

import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryCreatedEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryLowStockEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryRestockedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

/**
 * InventoryEventProducer 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryEventProducer 테스트")
class InventoryEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SendResult<String, Object> sendResult;

    @InjectMocks
    private InventoryEventProducer eventProducer;

    private Inventory testInventory;
    private static final String TEST_INVENTORY_ID = "INV-001";
    private static final String TEST_PRODUCT_ID = "PROD-001";
    private static final String TEST_HUB_ID = "HUB-SEOUL";
    private static final String TOPIC_NAME = "inventory-service-events";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventProducer, "applicationName", "inventory-service");

        testInventory = Inventory.create(
                TEST_INVENTORY_ID,
                TEST_PRODUCT_ID,
                TEST_HUB_ID,
                100,
                10,
                "A-1-1"
        );
    }

    @Nested
    @DisplayName("InventoryCreatedEvent 발행 테스트")
    class PublishInventoryCreatedTest {

        @Test
        @DisplayName("재고 생성 이벤트 발행 성공")
        void publishInventoryCreated_Success() {
            // given
            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
            given(kafkaTemplate.send(eq(TOPIC_NAME), eq(TEST_PRODUCT_ID), any(InventoryCreatedEvent.class)))
                    .willReturn(future);

            // when
            eventProducer.publishInventoryCreated(testInventory);

            // then
            ArgumentCaptor<InventoryCreatedEvent> eventCaptor = ArgumentCaptor.forClass(InventoryCreatedEvent.class);
            verify(kafkaTemplate).send(eq(TOPIC_NAME), eq(TEST_PRODUCT_ID), eventCaptor.capture());

            InventoryCreatedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("INVENTORY_CREATED");
            assertThat(capturedEvent.getInventoryId()).isEqualTo(TEST_INVENTORY_ID);
            assertThat(capturedEvent.getProductId()).isEqualTo(TEST_PRODUCT_ID);
            assertThat(capturedEvent.getHubId()).isEqualTo(TEST_HUB_ID);
            assertThat(capturedEvent.getQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("재고 생성 이벤트 발행 실패 시 로그 기록")
        void publishInventoryCreated_Failure() {
            // given
            CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Kafka 발행 실패"));
            given(kafkaTemplate.send(eq(TOPIC_NAME), eq(TEST_PRODUCT_ID), any(InventoryCreatedEvent.class)))
                    .willReturn(future);

            // when
            eventProducer.publishInventoryCreated(testInventory);

            // then
            verify(kafkaTemplate).send(eq(TOPIC_NAME), eq(TEST_PRODUCT_ID), any(InventoryCreatedEvent.class));
            // 실패 시 로그만 기록하고 예외를 던지지 않음
        }
    }

    @Nested
    @DisplayName("InventoryLowStockEvent 발행 테스트")
    class PublishInventoryLowStockTest {

        @Test
        @DisplayName("재고 부족 이벤트 발행 성공")
        void publishInventoryLowStock_Success() {
            // given
            testInventory.reserve(95); // 재고 5개 남음 (안전재고 10 이하)
            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
            given(kafkaTemplate.send(eq(TOPIC_NAME), eq(TEST_PRODUCT_ID), any(InventoryLowStockEvent.class)))
                    .willReturn(future);

            // when
            eventProducer.publishInventoryLowStock(testInventory);

            // then
            ArgumentCaptor<InventoryLowStockEvent> eventCaptor = ArgumentCaptor.forClass(InventoryLowStockEvent.class);
            verify(kafkaTemplate).send(eq(TOPIC_NAME), eq(TEST_PRODUCT_ID), eventCaptor.capture());

            InventoryLowStockEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("INVENTORY_LOW_STOCK");
            assertThat(capturedEvent.getInventoryId()).isEqualTo(TEST_INVENTORY_ID);
            assertThat(capturedEvent.getProductId()).isEqualTo(TEST_PRODUCT_ID);
            assertThat(capturedEvent.getHubId()).isEqualTo(TEST_HUB_ID);
            assertThat(capturedEvent.getCurrentQuantity()).isEqualTo(100);
            assertThat(capturedEvent.getSafetyStock()).isEqualTo(10);
            assertThat(capturedEvent.getDetectedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("InventoryRestockedEvent 발행 테스트")
    class PublishInventoryRestockedTest {

        @Test
        @DisplayName("재입고 이벤트 발행 성공")
        void publishInventoryRestocked_Success() {
            // given
            int restockedQuantity = 50;
            testInventory.restock(restockedQuantity);
            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
            given(kafkaTemplate.send(eq(TOPIC_NAME), eq(TEST_PRODUCT_ID), any(InventoryRestockedEvent.class)))
                    .willReturn(future);

            // when
            eventProducer.publishInventoryRestocked(testInventory, restockedQuantity);

            // then
            ArgumentCaptor<InventoryRestockedEvent> eventCaptor = ArgumentCaptor.forClass(InventoryRestockedEvent.class);
            verify(kafkaTemplate).send(eq(TOPIC_NAME), eq(TEST_PRODUCT_ID), eventCaptor.capture());

            InventoryRestockedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getEventType()).isEqualTo("INVENTORY_RESTOCKED");
            assertThat(capturedEvent.getInventoryId()).isEqualTo(TEST_INVENTORY_ID);
            assertThat(capturedEvent.getProductId()).isEqualTo(TEST_PRODUCT_ID);
            assertThat(capturedEvent.getHubId()).isEqualTo(TEST_HUB_ID);
            assertThat(capturedEvent.getRestockedQuantity()).isEqualTo(restockedQuantity);
            assertThat(capturedEvent.getCurrentQuantity()).isEqualTo(150); // 100 + 50
            assertThat(capturedEvent.getRestockedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("이벤트 발행 공통 테스트")
    class CommonEventPublishTest {

        @Test
        @DisplayName("모든 이벤트는 productId를 키로 사용한다")
        void allEvents_UseProductIdAsKey() {
            // given
            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
            given(kafkaTemplate.send(eq(TOPIC_NAME), eq(TEST_PRODUCT_ID), any()))
                    .willReturn(future);

            // when
            eventProducer.publishInventoryCreated(testInventory);
            eventProducer.publishInventoryLowStock(testInventory);
            eventProducer.publishInventoryRestocked(testInventory, 50);

            // then
            verify(kafkaTemplate, times(3)).send(eq(TOPIC_NAME), eq(TEST_PRODUCT_ID), any());
        }

        @Test
        @DisplayName("모든 이벤트는 동일한 토픽으로 발행된다")
        void allEvents_PublishToSameTopic() {
            // given
            CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
            given(kafkaTemplate.send(eq(TOPIC_NAME), any(), any()))
                    .willReturn(future);

            // when
            eventProducer.publishInventoryCreated(testInventory);
            eventProducer.publishInventoryLowStock(testInventory);
            eventProducer.publishInventoryRestocked(testInventory, 50);

            // then
            verify(kafkaTemplate, times(3)).send(eq(TOPIC_NAME), any(), any());
        }
    }
}