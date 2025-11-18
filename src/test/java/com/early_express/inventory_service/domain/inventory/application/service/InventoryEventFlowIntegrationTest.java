package com.early_express.inventory_service.domain.inventory.application.service;

import com.early_express.inventory_service.domain.inventory.application.service.InventoryService;
import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.domain.repository.InventoryRepository;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.consumer.ProductEventConsumer;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryCreatedEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryLowStockEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryRestockedEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.ProductCreatedEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.producer.InventoryEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

/**
 * 이벤트 플로우 통합 테스트
 * - Product ↔ Inventory 서비스 간 이벤트 흐름 검증
 */
@SpringBootTest
@Transactional
@DisplayName("이벤트 플로우 통합 테스트")
class InventoryEventFlowIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductEventConsumer productEventConsumer;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private Acknowledgment acknowledgment;

    @MockBean
    private SendResult<String, Object> sendResult;

    private static final String TEST_PRODUCT_ID = "PROD-INTEGRATION-001";
    private static final String TEST_SELLER_ID = "SELLER-001";

    @BeforeEach
    void setUp() {
        // Kafka 발행 성공으로 Mock 설정
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        given(kafkaTemplate.send(any(), any(), any())).willReturn(future);
        willDoNothing().given(acknowledgment).acknowledge();
    }

    @Nested
    @DisplayName("상품 생성 → 재고 생성 플로우")
    class ProductCreatedFlowTest {

        @Test
        @DisplayName("ProductCreatedEvent 수신 시 모든 허브에 초기 재고 생성")
        void productCreated_CreatesInitialInventories() {
            // given
            ProductCreatedEvent event = ProductCreatedEvent.builder()
                    .eventId("event-001")
                    .eventType("PRODUCT_CREATED")
                    .productId(TEST_PRODUCT_ID)
                    .sellerId(TEST_SELLER_ID)
                    .name("통합 테스트 상품")
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            productEventConsumer.handleProductCreatedEvent(event, 0, 100L, acknowledgment);

            // then
            List<Inventory> inventories = inventoryRepository.findByProductId(TEST_PRODUCT_ID);
            assertThat(inventories).hasSize(4); // 4개 허브
            assertThat(inventories)
                    .allMatch(inv -> inv.getProductId().equals(TEST_PRODUCT_ID))
                    .allMatch(inv -> inv.getQuantityInHub().getValue() == 0) // 초기 수량 0
                    .allMatch(inv -> inv.getSafetyStock().getValue() == 10); // 기본 안전재고 10

            // InventoryCreatedEvent 발행 확인
            verify(kafkaTemplate, times(4)).send(any(), eq(TEST_PRODUCT_ID), any(InventoryCreatedEvent.class));
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("이미 재고가 존재하는 허브는 건너뛴다")
        void productCreated_SkipsExistingInventories() {
            // given - 기존 재고 생성
            Inventory existingInventory = Inventory.create(
                    null,
                    TEST_PRODUCT_ID,
                    "HUB-SEOUL",
                    50,
                    10,
                    "A-1-1"
            );
            inventoryRepository.save(existingInventory);

            ProductCreatedEvent event = ProductCreatedEvent.builder()
                    .eventId("event-002")
                    .eventType("PRODUCT_CREATED")
                    .productId(TEST_PRODUCT_ID)
                    .sellerId(TEST_SELLER_ID)
                    .name("통합 테스트 상품")
                    .createdAt(LocalDateTime.now())
                    .build();

            // when
            productEventConsumer.handleProductCreatedEvent(event, 0, 100L, acknowledgment);

            // then
            List<Inventory> inventories = inventoryRepository.findByProductId(TEST_PRODUCT_ID);
            assertThat(inventories).hasSize(4); // 기존 1개 + 신규 3개

            // 서울 허브 재고는 기존 수량 유지
            Inventory seoulInventory = inventories.stream()
                    .filter(inv -> inv.getHubId().equals("HUB-SEOUL"))
                    .findFirst()
                    .orElseThrow();
            assertThat(seoulInventory.getQuantityInHub().getValue()).isEqualTo(50);

            // 나머지 허브는 0으로 생성
            assertThat(inventories.stream()
                    .filter(inv -> !inv.getHubId().equals("HUB-SEOUL"))
                    .allMatch(inv -> inv.getQuantityInHub().getValue() == 0)
            ).isTrue();
        }
    }

    @Nested
    @DisplayName("재고 부족 → 품절 처리 플로우")
    class LowStockFlowTest {

        private Inventory testInventory;

        @BeforeEach
        void setUp() {
            testInventory = Inventory.create(
                    null,
                    TEST_PRODUCT_ID,
                    "HUB-SEOUL",
                    100,
                    10,
                    "A-1-1"
            );
            testInventory = inventoryRepository.save(testInventory);
        }

        @Test
        @DisplayName("재고 예약 후 안전재고 이하면 LowStockEvent 발행")
        void reserve_PublishesLowStockEvent() {
            // given
            int reserveQuantity = 91; // 예약 후 9개 남음 (안전재고 10 이하)

            // when
            inventoryService.reserveStock(testInventory.getInventoryId(), reserveQuantity);

            // then
            verify(kafkaTemplate).send(any(), eq(TEST_PRODUCT_ID), any(InventoryLowStockEvent.class));
        }

        @Test
        @DisplayName("출고 확정 후 안전재고 이하면 LowStockEvent 발행")
        void confirmShipment_PublishesLowStockEvent() {
            // given
            testInventory.reserve(92);
            testInventory = inventoryRepository.save(testInventory);

            // when
            inventoryService.confirmShipment(testInventory.getInventoryId(), 92); // 출고 후 8개 남음

            // then
            verify(kafkaTemplate).send(any(), eq(TEST_PRODUCT_ID), any(InventoryLowStockEvent.class));
        }

        @Test
        @DisplayName("안전재고 초과 시 LowStockEvent 발행하지 않음")
        void reserve_DoesNotPublishLowStockEvent() {
            // given
            int reserveQuantity = 50; // 예약 후 50개 남음 (안전재고 10 초과)

            // when
            inventoryService.reserveStock(testInventory.getInventoryId(), reserveQuantity);

            // then
            verify(kafkaTemplate, never()).send(any(), any(), any(InventoryLowStockEvent.class));
        }
    }

    @Nested
    @DisplayName("재입고 → 품절 해제 플로우")
    class RestockFlowTest {

        private Inventory testInventory;

        @BeforeEach
        void setUp() {
            testInventory = Inventory.create(
                    null,
                    TEST_PRODUCT_ID,
                    "HUB-SEOUL",
                    5, // 안전재고 이하
                    10,
                    "A-1-1"
            );
            testInventory = inventoryRepository.save(testInventory);
        }

        @Test
        @DisplayName("재입고 시 InventoryRestockedEvent 발행")
        void restock_PublishesRestockedEvent() {
            // given
            int restockQuantity = 100;

            // when
            inventoryService.restock(testInventory.getInventoryId(), restockQuantity);

            // then
            verify(kafkaTemplate).send(any(), eq(TEST_PRODUCT_ID), any(InventoryRestockedEvent.class));

            // 재입고 후 재고 확인
            Inventory updatedInventory = inventoryRepository.findById(testInventory.getInventoryId())
                    .orElseThrow();
            assertThat(updatedInventory.getQuantityInHub().getValue()).isEqualTo(105); // 5 + 100
            assertThat(updatedInventory.getLastRestockedAt()).isNotNull();
        }

        @Test
        @DisplayName("품절 상품 재입고 시 RestockedEvent로 품절 해제 신호")
        void restock_OutOfStockProduct_PublishesRestockedEvent() {
            // given - 품절 상태 (재고 0)
            Inventory outOfStockInventory = Inventory.create(
                    null,
                    TEST_PRODUCT_ID,
                    "HUB-BUSAN",
                    0,
                    10,
                    "B-1-1"
            );
            outOfStockInventory = inventoryRepository.save(outOfStockInventory);

            int restockQuantity = 50;

            // when
            inventoryService.restock(outOfStockInventory.getInventoryId(), restockQuantity);

            // then
            verify(kafkaTemplate).send(any(), eq(TEST_PRODUCT_ID), any(InventoryRestockedEvent.class));

            Inventory updatedInventory = inventoryRepository.findById(outOfStockInventory.getInventoryId())
                    .orElseThrow();
            assertThat(updatedInventory.getQuantityInHub().getValue()).isEqualTo(50);
            assertThat(updatedInventory.isOutOfStock()).isFalse();
        }
    }

    @Nested
    @DisplayName("복합 시나리오 테스트")
    class ComplexScenarioTest {

        @Test
        @DisplayName("상품 생성 → 재입고 → 예약 → 출고 전체 플로우")
        void fullInventoryLifecycle() {
            // 1. 상품 생성 이벤트 수신
            ProductCreatedEvent productEvent = ProductCreatedEvent.builder()
                    .eventId("event-full-001")
                    .eventType("PRODUCT_CREATED")
                    .productId(TEST_PRODUCT_ID)
                    .sellerId(TEST_SELLER_ID)
                    .name("전체 플로우 테스트 상품")
                    .createdAt(LocalDateTime.now())
                    .build();

            productEventConsumer.handleProductCreatedEvent(productEvent, 0, 100L, acknowledgment);

            // 초기 재고 확인
            List<Inventory> inventories = inventoryRepository.findByProductId(TEST_PRODUCT_ID);
            assertThat(inventories).hasSize(4);
            Inventory seoulInventory = inventories.stream()
                    .filter(inv -> inv.getHubId().equals("HUB-SEOUL"))
                    .findFirst()
                    .orElseThrow();

            // 2. 재입고
            inventoryService.restock(seoulInventory.getInventoryId(), 100);
            verify(kafkaTemplate, atLeastOnce()).send(any(), eq(TEST_PRODUCT_ID), any(InventoryRestockedEvent.class));

            // 3. 재고 예약
            inventoryService.reserveStock(seoulInventory.getInventoryId(), 50);
            Inventory afterReserve = inventoryRepository.findById(seoulInventory.getInventoryId())
                    .orElseThrow();
            assertThat(afterReserve.getReservedQuantity().getValue()).isEqualTo(50);
            assertThat(afterReserve.getAvailableQuantity().getValue()).isEqualTo(50);

            // 4. 출고 확정
            inventoryService.confirmShipment(seoulInventory.getInventoryId(), 50);
            Inventory afterShipment = inventoryRepository.findById(seoulInventory.getInventoryId())
                    .orElseThrow();
            assertThat(afterShipment.getQuantityInHub().getValue()).isEqualTo(50);
            assertThat(afterShipment.getReservedQuantity().getValue()).isEqualTo(0);

            // 5. 대량 출고로 안전재고 이하 만들기
            inventoryService.reserveStock(seoulInventory.getInventoryId(), 45);
            inventoryService.confirmShipment(seoulInventory.getInventoryId(), 45);

            // LowStockEvent 발행 확인
            verify(kafkaTemplate, atLeastOnce()).send(any(), eq(TEST_PRODUCT_ID), any(InventoryLowStockEvent.class));
        }

        @Test
        @DisplayName("여러 허브에서 동시에 재고 부족 발생 시 각각 이벤트 발행")
        void multipleHubs_LowStock() {
            // given - 모든 허브 초기 재고 생성
            ProductCreatedEvent event = ProductCreatedEvent.builder()
                    .eventId("event-multi-001")
                    .eventType("PRODUCT_CREATED")
                    .productId(TEST_PRODUCT_ID)
                    .sellerId(TEST_SELLER_ID)
                    .name("멀티 허브 테스트")
                    .createdAt(LocalDateTime.now())
                    .build();

            productEventConsumer.handleProductCreatedEvent(event, 0, 100L, acknowledgment);

            List<Inventory> inventories = inventoryRepository.findByProductId(TEST_PRODUCT_ID);

            // when - 각 허브 재입고 후 대량 출고
            for (Inventory inventory : inventories) {
                inventoryService.restock(inventory.getInventoryId(), 100);
                inventoryService.reserveStock(inventory.getInventoryId(), 95);
                inventoryService.confirmShipment(inventory.getInventoryId(), 95);
            }

            // then - 각 허브마다 LowStockEvent 발행 확인
            verify(kafkaTemplate, atLeast(4)).send(any(), eq(TEST_PRODUCT_ID), any(InventoryLowStockEvent.class));
        }
    }
}
