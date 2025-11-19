package com.early_express.inventory_service.domain.inventory.application.service;

import com.early_express.inventory_service.domain.inventory.application.dto.command.RestockCommand;
import com.early_express.inventory_service.domain.inventory.application.dto.command.ReservationCommand;
import com.early_express.inventory_service.domain.inventory.application.dto.result.ReservationInfo;
import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.domain.repository.InventoryRepository;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.consumer.ProductEventConsumer;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryCreatedEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryLowStockEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryRestockedEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.ProductCreatedEvent;
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
 * 이벤트 플로우 통합 테스트 (개선 버전)
 * - Application Layer DTO 사용
 * - Product ↔ Inventory 서비스 간 이벤트 흐름 검증
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
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
        @DisplayName("ProductCreatedEvent 수신 시 해당 허브에 초기 재고 생성")
        void productCreated_CreatesInventoryForSpecificHub() {
            // given
            ProductCreatedEvent event = ProductCreatedEvent.builder()
                    .eventId("event-001")
                    .hubId("HUB-SEOUL")
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
            assertThat(inventories).hasSize(1); // 1개 허브만 생성

            Inventory inventory = inventories.get(0);
            assertThat(inventory.getProductId()).isEqualTo(TEST_PRODUCT_ID);
            assertThat(inventory.getHubId()).isEqualTo("HUB-SEOUL");
            assertThat(inventory.getQuantityInHub().getValue()).isEqualTo(0); // 초기 수량 0
            assertThat(inventory.getSafetyStock().getValue()).isEqualTo(10); // 기본 안전재고 10

            // InventoryCreatedEvent 발행 확인
            verify(kafkaTemplate, times(1)).send(any(), eq(TEST_PRODUCT_ID), any(InventoryCreatedEvent.class));
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("이미 재고가 존재하는 허브는 건너뛴다")
        void productCreated_SkipsExistingInventory() {
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
                    .hubId("HUB-SEOUL")
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
            assertThat(inventories).hasSize(1); // 여전히 1개만 존재

            // 서울 허브 재고는 기존 수량 유지
            Inventory seoulInventory = inventories.get(0);
            assertThat(seoulInventory.getHubId()).isEqualTo("HUB-SEOUL");
            assertThat(seoulInventory.getQuantityInHub().getValue()).isEqualTo(50);

            // 새로운 InventoryCreatedEvent는 발행되지 않음 (이미 존재하므로)
            verify(kafkaTemplate, never()).send(any(), any(), any(InventoryCreatedEvent.class));
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("여러 허브에서 동일 상품 생성 시 각각 재고 생성")
        void productCreated_MultipleHubs_CreatesMultipleInventories() {
            // given & when - 서울 허브
            ProductCreatedEvent seoulEvent = ProductCreatedEvent.builder()
                    .eventId("event-seoul")
                    .hubId("HUB-SEOUL")
                    .eventType("PRODUCT_CREATED")
                    .productId(TEST_PRODUCT_ID)
                    .sellerId(TEST_SELLER_ID)
                    .name("통합 테스트 상품")
                    .createdAt(LocalDateTime.now())
                    .build();
            productEventConsumer.handleProductCreatedEvent(seoulEvent, 0, 100L, acknowledgment);

            // when - 부산 허브
            ProductCreatedEvent busanEvent = ProductCreatedEvent.builder()
                    .eventId("event-busan")
                    .hubId("HUB-BUSAN")
                    .eventType("PRODUCT_CREATED")
                    .productId(TEST_PRODUCT_ID)
                    .sellerId(TEST_SELLER_ID)
                    .name("통합 테스트 상품")
                    .createdAt(LocalDateTime.now())
                    .build();
            productEventConsumer.handleProductCreatedEvent(busanEvent, 0, 101L, acknowledgment);

            // then
            List<Inventory> inventories = inventoryRepository.findByProductId(TEST_PRODUCT_ID);
            assertThat(inventories).hasSize(2); // 2개 허브에 생성
            assertThat(inventories)
                    .extracting(Inventory::getHubId)
                    .containsExactlyInAnyOrder("HUB-SEOUL", "HUB-BUSAN");

            verify(kafkaTemplate, times(2)).send(any(), eq(TEST_PRODUCT_ID), any(InventoryCreatedEvent.class));
            verify(acknowledgment, times(2)).acknowledge();
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
        @DisplayName("재고 예약 후 출고 확정하면 안전재고 이하일 때 LowStockEvent 발행")
        void reserve_AndConfirmShipment_PublishesLowStockEvent() {
            // given
            int reserveQuantity = 91; // 출고 후 9개 남음 (안전재고 10 이하)
            String orderId = "ORDER-001";

            ReservationCommand command = ReservationCommand.builder()
                    .orderId(orderId)
                    .items(List.of(
                            ReservationCommand.ReservationItem.builder()
                                    .productId(TEST_PRODUCT_ID)
                                    .hubId("HUB-SEOUL")
                                    .quantity(reserveQuantity)
                                    .build()
                    ))
                    .build();

            // when - 예약
            ReservationInfo result = inventoryService.reserveStock(command);
            assertThat(result.isAllSuccess()).isTrue();

            // then - 예약 시점에는 LowStockEvent 발행되지 않음 (quantityInHub가 여전히 100)
            verify(kafkaTemplate, never()).send(any(), any(), any(InventoryLowStockEvent.class));

            // when - 출고 확정 (실제 재고 감소)
            inventoryService.confirmShipment(TEST_PRODUCT_ID, "HUB-SEOUL", reserveQuantity, orderId);

            // then - 출고 확정 후 LowStockEvent 발행됨 (quantityInHub가 9로 감소)
            verify(kafkaTemplate).send(any(), eq(TEST_PRODUCT_ID), any(InventoryLowStockEvent.class));
        }

        @Test
        @DisplayName("출고 확정 후 안전재고 이하면 LowStockEvent 발행")
        void confirmShipment_PublishesLowStockEvent() {
            // given - 먼저 예약
            testInventory.reserve(92);
            testInventory = inventoryRepository.save(testInventory);
            String orderId = "ORDER-002";

            // when - 출고 확정 (출고 후 8개 남음)
            inventoryService.confirmShipment(TEST_PRODUCT_ID, "HUB-SEOUL", 92, orderId);

            // then - LowStockEvent 발행 확인
            verify(kafkaTemplate).send(any(), eq(TEST_PRODUCT_ID), any(InventoryLowStockEvent.class));

            // 재고 확인
            Inventory updatedInventory = inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, "HUB-SEOUL")
                    .orElseThrow();
            assertThat(updatedInventory.getQuantityInHub().getValue()).isEqualTo(8);
            assertThat(updatedInventory.getReservedQuantity().getValue()).isEqualTo(0);
        }

        @Test
        @DisplayName("예약 + 출고 후에도 안전재고 초과 시 LowStockEvent 발행하지 않음")
        void reserve_AndConfirmShipment_DoesNotPublishLowStockEvent() {
            // given
            int reserveQuantity = 50; // 출고 후 50개 남음 (안전재고 10 초과)
            String orderId = "ORDER-003";

            ReservationCommand command = ReservationCommand.builder()
                    .orderId(orderId)
                    .items(List.of(
                            ReservationCommand.ReservationItem.builder()
                                    .productId(TEST_PRODUCT_ID)
                                    .hubId("HUB-SEOUL")
                                    .quantity(reserveQuantity)
                                    .build()
                    ))
                    .build();

            // when - 예약 및 출고 확정
            inventoryService.reserveStock(command);
            inventoryService.confirmShipment(TEST_PRODUCT_ID, "HUB-SEOUL", reserveQuantity, orderId);

            // then - LowStockEvent 발행되지 않음
            verify(kafkaTemplate, never()).send(any(), any(), any(InventoryLowStockEvent.class));

            // 재고 확인
            Inventory updatedInventory = inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, "HUB-SEOUL")
                    .orElseThrow();
            assertThat(updatedInventory.getQuantityInHub().getValue()).isEqualTo(50);
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

            RestockCommand command = RestockCommand.builder()
                    .productId(TEST_PRODUCT_ID)
                    .hubId("HUB-SEOUL")
                    .quantity(restockQuantity)
                    .build();

            // when
            inventoryService.restock(command);

            // then
            verify(kafkaTemplate).send(any(), eq(TEST_PRODUCT_ID), any(InventoryRestockedEvent.class));

            // 재입고 후 재고 확인
            Inventory updatedInventory = inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, "HUB-SEOUL")
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

            RestockCommand command = RestockCommand.builder()
                    .productId(TEST_PRODUCT_ID)
                    .hubId("HUB-BUSAN")
                    .quantity(restockQuantity)
                    .build();

            // when
            inventoryService.restock(command);

            // then
            verify(kafkaTemplate).send(any(), eq(TEST_PRODUCT_ID), any(InventoryRestockedEvent.class));

            Inventory updatedInventory = inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, "HUB-BUSAN")
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
                    .hubId("HUB-SEOUL")
                    .eventType("PRODUCT_CREATED")
                    .productId(TEST_PRODUCT_ID)
                    .sellerId(TEST_SELLER_ID)
                    .name("전체 플로우 테스트 상품")
                    .createdAt(LocalDateTime.now())
                    .build();

            productEventConsumer.handleProductCreatedEvent(productEvent, 0, 100L, acknowledgment);

            // 초기 재고 확인
            Inventory seoulInventory = inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, "HUB-SEOUL")
                    .orElseThrow();
            assertThat(seoulInventory.getQuantityInHub().getValue()).isEqualTo(0);

            // 2. 재입고
            RestockCommand restockCommand = RestockCommand.builder()
                    .productId(TEST_PRODUCT_ID)
                    .hubId("HUB-SEOUL")
                    .quantity(100)
                    .build();

            inventoryService.restock(restockCommand);
            verify(kafkaTemplate, atLeastOnce()).send(any(), eq(TEST_PRODUCT_ID), any(InventoryRestockedEvent.class));

            // 3. 재고 예약
            ReservationCommand reserveCommand = ReservationCommand.builder()
                    .orderId("ORDER-001")
                    .items(List.of(
                            ReservationCommand.ReservationItem.builder()
                                    .productId(TEST_PRODUCT_ID)
                                    .hubId("HUB-SEOUL")
                                    .quantity(50)
                                    .build()
                    ))
                    .build();

            ReservationInfo reservationInfo = inventoryService.reserveStock(reserveCommand);
            assertThat(reservationInfo.isAllSuccess()).isTrue();

            Inventory afterReserve = inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, "HUB-SEOUL")
                    .orElseThrow();
            assertThat(afterReserve.getReservedQuantity().getValue()).isEqualTo(50);
            assertThat(afterReserve.getAvailableQuantity().getValue()).isEqualTo(50);

            // 4. 출고 확정
            inventoryService.confirmShipment(TEST_PRODUCT_ID, "HUB-SEOUL", 50, "ORDER-001");

            Inventory afterShipment = inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, "HUB-SEOUL")
                    .orElseThrow();
            assertThat(afterShipment.getQuantityInHub().getValue()).isEqualTo(50);
            assertThat(afterShipment.getReservedQuantity().getValue()).isEqualTo(0);

            // 5. 대량 출고로 안전재고 이하 만들기
            ReservationCommand largeReserveCommand = ReservationCommand.builder()
                    .orderId("ORDER-002")
                    .items(List.of(
                            ReservationCommand.ReservationItem.builder()
                                    .productId(TEST_PRODUCT_ID)
                                    .hubId("HUB-SEOUL")
                                    .quantity(45)
                                    .build()
                    ))
                    .build();

            inventoryService.reserveStock(largeReserveCommand);
            inventoryService.confirmShipment(TEST_PRODUCT_ID, "HUB-SEOUL", 45, "ORDER-002");

            // LowStockEvent 발행 확인
            verify(kafkaTemplate, atLeastOnce()).send(any(), eq(TEST_PRODUCT_ID), any(InventoryLowStockEvent.class));
        }

        @Test
        @DisplayName("여러 허브에서 동시에 재고 부족 발생 시 각각 이벤트 발행")
        void multipleHubs_LowStock() {
            // given - 여러 허브에 재고 생성
            List<String> hubIds = List.of("HUB-SEOUL", "HUB-BUSAN", "HUB-INCHEON", "HUB-DAEGU");

            for (String hubId : hubIds) {
                ProductCreatedEvent event = ProductCreatedEvent.builder()
                        .eventId("event-" + hubId)
                        .hubId(hubId)
                        .eventType("PRODUCT_CREATED")
                        .productId(TEST_PRODUCT_ID)
                        .sellerId(TEST_SELLER_ID)
                        .name("멀티 허브 테스트")
                        .createdAt(LocalDateTime.now())
                        .build();
                productEventConsumer.handleProductCreatedEvent(event, 0, 100L, acknowledgment);
            }

            List<Inventory> inventories = inventoryRepository.findByProductId(TEST_PRODUCT_ID);
            assertThat(inventories).hasSize(4);

            // when - 각 허브 재입고 후 대량 출고
            for (Inventory inventory : inventories) {
                RestockCommand restockCommand = RestockCommand.builder()
                        .productId(inventory.getProductId())
                        .hubId(inventory.getHubId())
                        .quantity(100)
                        .build();

                inventoryService.restock(restockCommand);

                ReservationCommand reserveCommand = ReservationCommand.builder()
                        .orderId("ORDER-" + inventory.getHubId())
                        .items(List.of(
                                ReservationCommand.ReservationItem.builder()
                                        .productId(inventory.getProductId())
                                        .hubId(inventory.getHubId())
                                        .quantity(95)
                                        .build()
                        ))
                        .build();

                inventoryService.reserveStock(reserveCommand);
                inventoryService.confirmShipment(
                        inventory.getProductId(),
                        inventory.getHubId(),
                        95,
                        "ORDER-" + inventory.getHubId()
                );
            }

            // then - 각 허브마다 LowStockEvent 발행 확인
            verify(kafkaTemplate, atLeast(4)).send(any(), eq(TEST_PRODUCT_ID), any(InventoryLowStockEvent.class));
        }
    }
}