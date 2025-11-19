package com.early_express.inventory_service.domain.inventory.application.service;

import com.early_express.inventory_service.domain.inventory.application.dto.command.*;
import com.early_express.inventory_service.domain.inventory.application.dto.result.*;
import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryErrorCode;
import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryException;
import com.early_express.inventory_service.domain.inventory.domain.messaging.InventoryEventPublisher;
import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.domain.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

/**
 * InventoryService 단위 테스트
 * - Application Layer DTO 사용
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService 테스트")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryEventPublisher eventPublisher;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory testInventory;
    private static final String TEST_INVENTORY_ID = "INV-001";
    private static final String TEST_PRODUCT_ID = "PROD-001";
    private static final String TEST_HUB_ID = "HUB-SEOUL";

    @BeforeEach
    void setUp() {
        testInventory = Inventory.create(
                TEST_INVENTORY_ID,
                TEST_PRODUCT_ID,
                TEST_HUB_ID,
                100, // 초기 수량
                10,  // 안전 재고
                "A-1-1"
        );
    }

    @Nested
    @DisplayName("초기 재고 생성 테스트")
    class CreateInitialInventoriesTest {

        @Test
        @DisplayName("상품 생성 시 모든 허브에 초기 재고가 생성된다")
        void createInitialInventories_Success() {
            // given
            String productId = "PROD-NEW-001";
            given(inventoryRepository.existsByProductIdAndHubId(eq(productId), anyString()))
                    .willReturn(false);
            given(inventoryRepository.save(any(Inventory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            List<Inventory> result = inventoryService.createInitialInventories(productId);

            // then
            assertThat(result).hasSize(4);
            verify(inventoryRepository, times(4)).save(any(Inventory.class));
            verify(eventPublisher, times(4)).publishInventoryCreated(any(Inventory.class));
        }

        @Test
        @DisplayName("이미 존재하는 재고는 생성하지 않는다")
        void createInitialInventories_SkipExisting() {
            // given
            String productId = "PROD-EXISTING-001";
            given(inventoryRepository.existsByProductIdAndHubId(productId, "HUB-SEOUL"))
                    .willReturn(true);
            given(inventoryRepository.existsByProductIdAndHubId(eq(productId), argThat(hubId -> !hubId.equals("HUB-SEOUL"))))
                    .willReturn(false);
            given(inventoryRepository.save(any(Inventory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            List<Inventory> result = inventoryService.createInitialInventories(productId);

            // then
            assertThat(result).hasSize(3); // 3개만 생성
            verify(inventoryRepository, times(3)).save(any(Inventory.class));
            verify(eventPublisher, times(3)).publishInventoryCreated(any(Inventory.class));
        }

        @Test
        @DisplayName("생성된 재고의 초기 수량은 0이다")
        void createInitialInventories_InitialQuantityIsZero() {
            // given
            String productId = "PROD-NEW-002";
            given(inventoryRepository.existsByProductIdAndHubId(eq(productId), anyString()))
                    .willReturn(false);
            ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
            given(inventoryRepository.save(inventoryCaptor.capture()))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            inventoryService.createInitialInventories(productId);

            // then
            List<Inventory> savedInventories = inventoryCaptor.getAllValues();
            assertThat(savedInventories).hasSize(4);
            assertThat(savedInventories)
                    .allMatch(inv -> inv.getQuantityInHub().getValue() == 0);
        }
    }

    @Nested
    @DisplayName("재입고 테스트")
    class RestockTest {

        @Test
        @DisplayName("재입고 성공 시 재고가 증가하고 이벤트가 발행된다")
        void restock_Success() {
            // given
            int restockQuantity = 50;
            int expectedQuantity = 150; // 100 + 50

            RestockCommand command = RestockCommand.builder()
                    .productId(TEST_PRODUCT_ID)
                    .hubId(TEST_HUB_ID)
                    .quantity(restockQuantity)
                    .build();

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Inventory result = inventoryService.restock(command);

            // then
            assertThat(result.getQuantityInHub().getValue()).isEqualTo(expectedQuantity);
            assertThat(result.getLastRestockedAt()).isNotNull();
            verify(eventPublisher).publishInventoryRestocked(any(Inventory.class), eq(restockQuantity));
        }

        @Test
        @DisplayName("존재하지 않는 재고에 재입고 시도하면 예외 발생")
        void restock_InventoryNotFound() {
            // given
            RestockCommand command = RestockCommand.builder()
                    .productId(TEST_PRODUCT_ID)
                    .hubId(TEST_HUB_ID)
                    .quantity(50)
                    .build();

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> inventoryService.restock(command))
                    .isInstanceOf(InventoryException.class)
                    .hasFieldOrPropertyWithValue("errorCode", InventoryErrorCode.INVENTORY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("재고 예약 테스트")
    class ReserveStockTest {

        @Test
        @DisplayName("단일 상품 예약 성공")
        void reserveStock_SingleItem_Success() {
            // given
            int reserveQuantity = 30;
            String orderId = "ORDER-001";

            ReservationCommand command = ReservationCommand.builder()
                    .orderId(orderId)
                    .items(List.of(
                            ReservationCommand.ReservationItem.builder()
                                    .productId(TEST_PRODUCT_ID)
                                    .hubId(TEST_HUB_ID)
                                    .quantity(reserveQuantity)
                                    .build()
                    ))
                    .build();

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ReservationInfo result = inventoryService.reserveStock(command);

            // then
            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.isAllSuccess()).isTrue();
            assertThat(result.getReservedItems()).hasSize(1);
            assertThat(result.getReservedItems().get(0).isSuccess()).isTrue();
            assertThat(result.getReservedItems().get(0).getQuantity()).isEqualTo(reserveQuantity);

            verify(inventoryRepository).save(any(Inventory.class));
            verify(eventPublisher).publishInventoryReserved(any(Inventory.class), eq(orderId), eq(reserveQuantity));
        }

        @Test
        @DisplayName("다중 상품 예약 - 모두 성공")
        void reserveStock_MultipleItems_AllSuccess() {
            // given
            String orderId = "ORDER-002";
            Inventory testInventory2 = Inventory.create(
                    "INV-002",
                    "PROD-002",
                    TEST_HUB_ID,
                    100,
                    10,
                    "A-2-1"
            );

            ReservationCommand command = ReservationCommand.builder()
                    .orderId(orderId)
                    .items(List.of(
                            ReservationCommand.ReservationItem.builder()
                                    .productId(TEST_PRODUCT_ID)
                                    .hubId(TEST_HUB_ID)
                                    .quantity(30)
                                    .build(),
                            ReservationCommand.ReservationItem.builder()
                                    .productId("PROD-002")
                                    .hubId(TEST_HUB_ID)
                                    .quantity(20)
                                    .build()
                    ))
                    .build();

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));
            given(inventoryRepository.findByProductIdAndHubId("PROD-002", TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory2));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ReservationInfo result = inventoryService.reserveStock(command);

            // then
            assertThat(result.isAllSuccess()).isTrue();
            assertThat(result.getReservedItems()).hasSize(2);
            assertThat(result.getReservedItems()).allMatch(ReservationInfo.ReservedItemInfo::isSuccess);
        }

        @Test
        @DisplayName("다중 상품 예약 - 일부 실패")
        void reserveStock_MultipleItems_PartialFailure() {
            // given
            String orderId = "ORDER-003";

            ReservationCommand command = ReservationCommand.builder()
                    .orderId(orderId)
                    .items(List.of(
                            ReservationCommand.ReservationItem.builder()
                                    .productId(TEST_PRODUCT_ID)
                                    .hubId(TEST_HUB_ID)
                                    .quantity(30)
                                    .build(),
                            ReservationCommand.ReservationItem.builder()
                                    .productId("PROD-NOT-EXIST")
                                    .hubId(TEST_HUB_ID)
                                    .quantity(20)
                                    .build()
                    ))
                    .build();

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));
            given(inventoryRepository.findByProductIdAndHubId("PROD-NOT-EXIST", TEST_HUB_ID))
                    .willReturn(Optional.empty());
            given(inventoryRepository.save(any(Inventory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ReservationInfo result = inventoryService.reserveStock(command);

            // then
            assertThat(result.isAllSuccess()).isFalse();
            assertThat(result.getReservedItems()).hasSize(2);
            assertThat(result.getReservedItems().get(0).isSuccess()).isTrue();
            assertThat(result.getReservedItems().get(1).isSuccess()).isFalse();
            assertThat(result.getReservedItems().get(1).getErrorMessage()).isNotNull();
        }

        @Test
        @DisplayName("재고 예약 시 안전 재고 이하면 LowStock 이벤트 발행")
        void reserveStock_PublishLowStockEvent() {
            // given
            int reserveQuantity = 91; // 예약 후 9개 남음 (안전재고 10 이하)
            String orderId = "ORDER-004";

            ReservationCommand command = ReservationCommand.builder()
                    .orderId(orderId)
                    .items(List.of(
                            ReservationCommand.ReservationItem.builder()
                                    .productId(TEST_PRODUCT_ID)
                                    .hubId(TEST_HUB_ID)
                                    .quantity(reserveQuantity)
                                    .build()
                    ))
                    .build();

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            inventoryService.reserveStock(command);

            // then
            verify(eventPublisher).publishInventoryLowStock(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("예약 해제 테스트")
    class ReleaseReservationTest {

        @Test
        @DisplayName("예약 해제 성공")
        void releaseReservation_Success() {
            // given
            testInventory.reserve(50); // 50개 예약
            int releaseQuantity = 30;
            String orderId = "ORDER-001";

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Inventory result = inventoryService.releaseReservation(
                    TEST_PRODUCT_ID,
                    TEST_HUB_ID,
                    releaseQuantity,
                    orderId
            );

            // then
            assertThat(result.getReservedQuantity().getValue()).isEqualTo(20); // 50 - 30
            assertThat(result.getAvailableQuantity().getValue()).isEqualTo(80); // 100 - 20
        }

        @Test
        @DisplayName("예약된 수량보다 많이 해제 시도하면 예외 발생")
        void releaseReservation_ExceedReservedQuantity() {
            // given
            testInventory.reserve(30);
            int releaseQuantity = 50; // 예약된 30개보다 많음

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));

            // when & then
            assertThatThrownBy(() -> inventoryService.releaseReservation(
                    TEST_PRODUCT_ID,
                    TEST_HUB_ID,
                    releaseQuantity,
                    "ORDER-001"
            ))
                    .isInstanceOf(InventoryException.class)
                    .hasFieldOrPropertyWithValue("errorCode", InventoryErrorCode.CANNOT_RELEASE_MORE_THAN_RESERVED);
        }
    }

    @Nested
    @DisplayName("출고 확정 테스트")
    class ConfirmShipmentTest {

        @Test
        @DisplayName("출고 확정 성공")
        void confirmShipment_Success() {
            // given
            testInventory.reserve(50);
            int shipmentQuantity = 30;
            String orderId = "ORDER-001";

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Inventory result = inventoryService.confirmShipment(
                    TEST_PRODUCT_ID,
                    TEST_HUB_ID,
                    shipmentQuantity,
                    orderId
            );

            // then
            assertThat(result.getQuantityInHub().getValue()).isEqualTo(70); // 100 - 30
            assertThat(result.getReservedQuantity().getValue()).isEqualTo(20); // 50 - 30
            assertThat(result.getAvailableQuantity().getValue()).isEqualTo(50); // 70 - 20
        }

        @Test
        @DisplayName("출고 확정 후 안전 재고 이하면 LowStock 이벤트 발행")
        void confirmShipment_PublishLowStockEvent() {
            // given
            testInventory.reserve(92);
            int shipmentQuantity = 92; // 출고 후 8개 남음 (안전재고 10 이하)

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            inventoryService.confirmShipment(TEST_PRODUCT_ID, TEST_HUB_ID, shipmentQuantity, "ORDER-001");

            // then
            verify(eventPublisher).publishInventoryLowStock(any(Inventory.class));
        }
    }

    @Nested
    @DisplayName("재고 가용성 확인 테스트")
    class CheckAvailabilityTest {

        @Test
        @DisplayName("재고 가용성 확인 성공")
        void checkAvailability_Success() {
            // given
            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));

            // when
            AvailabilityInfo result = inventoryService.checkAvailability(TEST_PRODUCT_ID, TEST_HUB_ID);

            // then
            assertThat(result.getProductId()).isEqualTo(TEST_PRODUCT_ID);
            assertThat(result.getHubId()).isEqualTo(TEST_HUB_ID);
            assertThat(result.isAvailable()).isTrue();
            assertThat(result.getAvailableQuantity()).isEqualTo(100);
            assertThat(result.getReservedQuantity()).isEqualTo(0);
            assertThat(result.getTotalQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("재고 없는 경우 가용성 false")
        void checkAvailability_NotAvailable() {
            // given
            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.empty());

            // when
            AvailabilityInfo result = inventoryService.checkAvailability(TEST_PRODUCT_ID, TEST_HUB_ID);

            // then
            assertThat(result.isAvailable()).isFalse();
            assertThat(result.getAvailableQuantity()).isEqualTo(0);
            assertThat(result.getError()).isNotNull();
        }

        @Test
        @DisplayName("대량 재고 확인 - 모두 가용")
        void checkBulkAvailability_AllAvailable() {
            // given
            Inventory inventory2 = Inventory.create(
                    "INV-002",
                    "PROD-002",
                    TEST_HUB_ID,
                    100,
                    10,
                    "A-2-1"
            );

            BulkCheckCommand command = BulkCheckCommand.builder()
                    .hubId(TEST_HUB_ID)
                    .items(List.of(
                            BulkCheckCommand.CheckItem.builder()
                                    .productId(TEST_PRODUCT_ID)
                                    .quantity(30)
                                    .build(),
                            BulkCheckCommand.CheckItem.builder()
                                    .productId("PROD-002")
                                    .quantity(50)
                                    .build()
                    ))
                    .build();

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));
            given(inventoryRepository.findByProductIdAndHubId("PROD-002", TEST_HUB_ID))
                    .willReturn(Optional.of(inventory2));

            // when
            BulkAvailabilityInfo result = inventoryService.checkBulkAvailability(command);

            // then
            assertThat(result.getHubId()).isEqualTo(TEST_HUB_ID);
            assertThat(result.isAllAvailable()).isTrue();
            assertThat(result.getResults()).hasSize(2);
            assertThat(result.getResults()).allMatch(BulkAvailabilityInfo.ItemAvailabilityInfo::isAvailable);
        }

        @Test
        @DisplayName("대량 재고 확인 - 일부 부족")
        void checkBulkAvailability_PartiallyAvailable() {
            // given
            BulkCheckCommand command = BulkCheckCommand.builder()
                    .hubId(TEST_HUB_ID)
                    .items(List.of(
                            BulkCheckCommand.CheckItem.builder()
                                    .productId(TEST_PRODUCT_ID)
                                    .quantity(30)
                                    .build(),
                            BulkCheckCommand.CheckItem.builder()
                                    .productId("PROD-NOT-EXIST")
                                    .quantity(50)
                                    .build()
                    ))
                    .build();

            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));
            given(inventoryRepository.findByProductIdAndHubId("PROD-NOT-EXIST", TEST_HUB_ID))
                    .willReturn(Optional.empty());

            // when
            BulkAvailabilityInfo result = inventoryService.checkBulkAvailability(command);

            // then
            assertThat(result.isAllAvailable()).isFalse();
            assertThat(result.getResults()).hasSize(2);
            assertThat(result.getResults().get(0).isAvailable()).isTrue();
            assertThat(result.getResults().get(1).isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("재고 조회 테스트")
    class GetInventoryTest {

        @Test
        @DisplayName("ID로 재고 조회 성공")
        void getInventory_Success() {
            // given
            given(inventoryRepository.findById(TEST_INVENTORY_ID))
                    .willReturn(Optional.of(testInventory));

            // when
            Inventory result = inventoryService.getInventory(TEST_INVENTORY_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getInventoryId()).isEqualTo(TEST_INVENTORY_ID);
            assertThat(result.getProductId()).isEqualTo(TEST_PRODUCT_ID);
        }

        @Test
        @DisplayName("상품-허브 조합으로 재고 조회 성공")
        void getInventoryByProductAndHub_Success() {
            // given
            given(inventoryRepository.findByProductIdAndHubId(TEST_PRODUCT_ID, TEST_HUB_ID))
                    .willReturn(Optional.of(testInventory));

            // when
            Inventory result = inventoryService.getInventoryByProductAndHub(TEST_PRODUCT_ID, TEST_HUB_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getProductId()).isEqualTo(TEST_PRODUCT_ID);
            assertThat(result.getHubId()).isEqualTo(TEST_HUB_ID);
        }

        @Test
        @DisplayName("상품별 전체 재고 조회")
        void getInventoriesByProduct_Success() {
            // given
            List<Inventory> inventories = List.of(
                    testInventory,
                    Inventory.create("INV-002", TEST_PRODUCT_ID, "HUB-BUSAN", 50, 10, "B-1-1")
            );
            given(inventoryRepository.findByProductId(TEST_PRODUCT_ID))
                    .willReturn(inventories);

            // when
            List<Inventory> result = inventoryService.getInventoriesByProduct(TEST_PRODUCT_ID);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(inv -> inv.getProductId().equals(TEST_PRODUCT_ID));
        }
    }

    @Nested
    @DisplayName("재고 조정 테스트")
    class AdjustInventoryTest {

        @Test
        @DisplayName("재고 조정 성공")
        void adjustInventory_Success() {
            // given
            AdjustCommand command = AdjustCommand.builder()
                    .adjustmentQuantity(50)
                    .reason("재고 정정")
                    .build();

            given(inventoryRepository.findById(TEST_INVENTORY_ID))
                    .willReturn(Optional.of(testInventory));
            given(inventoryRepository.save(any(Inventory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            Inventory result = inventoryService.adjustInventory(TEST_INVENTORY_ID, command);

            // then
            assertThat(result.getQuantityInHub().getValue()).isEqualTo(150); // 100 + 50
        }
    }
}