package com.early_express.inventory_service.domain.inventory.domain.model;

import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryErrorCode;
import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryException;
import com.early_express.inventory_service.domain.inventory.domain.model.vo.StockQuantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Inventory 도메인 모델 테스트")
class InventoryTest {

    @Test
    @DisplayName("유효한 정보로 재고 생성")
    void createInventory_withValidInfo() {
        // given
        String inventoryId = "INV-001";
        String productId = "PROD-001";
        String hubId = "HUB-SEOUL";
        Integer initialQuantity = 100;
        Integer safetyStock = 10;
        String location = "A-1-3";

        // when
        Inventory inventory = Inventory.create(
                inventoryId, productId, hubId, initialQuantity, safetyStock, location
        );

        // then
        assertThat(inventory.getInventoryId()).isEqualTo(inventoryId);
        assertThat(inventory.getProductId()).isEqualTo(productId);
        assertThat(inventory.getHubId()).isEqualTo(hubId);
        assertThat(inventory.getQuantityInHub().getValue()).isEqualTo(initialQuantity);
        assertThat(inventory.getReservedQuantity().getValue()).isZero();
        assertThat(inventory.getSafetyStock().getValue()).isEqualTo(safetyStock);
        assertThat(inventory.getLocation()).isEqualTo(location);
        assertThat(inventory.getLastRestockedAt()).isNotNull();
        assertThat(inventory.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("잘못된 위치 형식으로 생성 시 예외 발생")
    void createInventory_withInvalidLocation_throwsException() {
        // given
        String invalidLocation1 = "A1-3";
        String invalidLocation2 = "A-B-3";
        String invalidLocation3 = "";

        // when & then
        assertThatThrownBy(() -> Inventory.create(
                "INV-001", "PROD-001", "HUB-001", 100, 10, invalidLocation1
        ))
                .isInstanceOf(InventoryException.class)
                .extracting(e -> ((InventoryException) e).getErrorCode())
                .isEqualTo(InventoryErrorCode.INVALID_LOCATION_FORMAT);

        assertThatThrownBy(() -> Inventory.create(
                "INV-001", "PROD-001", "HUB-001", 100, 10, invalidLocation2
        ))
                .isInstanceOf(InventoryException.class);

        assertThatThrownBy(() -> Inventory.create(
                "INV-001", "PROD-001", "HUB-001", 100, 10, invalidLocation3
        ))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("위치 정보는 필수입니다");
    }

    @Test
    @DisplayName("입고 처리")
    void restock() {
        // given
        Inventory inventory = createTestInventory();
        Integer restockQuantity = 50;
        Integer originalQuantity = inventory.getQuantityInHub().getValue();

        // when
        inventory.restock(restockQuantity);

        // then
        assertThat(inventory.getQuantityInHub().getValue()).isEqualTo(originalQuantity + restockQuantity);
        assertThat(inventory.getLastRestockedAt()).isNotNull();
    }

    @Test
    @DisplayName("0 이하 수량 입고 시 예외 발생")
    void restock_withInvalidQuantity_throwsException() {
        // given
        Inventory inventory = createTestInventory();

        // when & then
        assertThatThrownBy(() -> inventory.restock(0))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("입고 수량은 0보다 커야 합니다");

        assertThatThrownBy(() -> inventory.restock(-10))
                .isInstanceOf(InventoryException.class);
    }

    @Test
    @DisplayName("재고 예약")
    void reserve() {
        // given
        Inventory inventory = createTestInventory();
        Integer reserveQuantity = 30;

        // when
        inventory.reserve(reserveQuantity);

        // then
        assertThat(inventory.getReservedQuantity().getValue()).isEqualTo(reserveQuantity);
        assertThat(inventory.getAvailableQuantity().getValue()).isEqualTo(70); // 100 - 30
    }

    @Test
    @DisplayName("가용 재고 부족 시 예약 예외 발생")
    void reserve_insufficientStock_throwsException() {
        // given
        Inventory inventory = createTestInventory();
        Integer excessiveQuantity = 150;

        // when & then
        assertThatThrownBy(() -> inventory.reserve(excessiveQuantity))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("가용 재고")
                .extracting(e -> ((InventoryException) e).getErrorCode())
                .isEqualTo(InventoryErrorCode.INSUFFICIENT_AVAILABLE_STOCK);
    }

    @Test
    @DisplayName("예약 해제")
    void releaseReservation() {
        // given
        Inventory inventory = createTestInventory();
        inventory.reserve(30);

        // when
        inventory.releaseReservation(10);

        // then
        assertThat(inventory.getReservedQuantity().getValue()).isEqualTo(20);
        assertThat(inventory.getAvailableQuantity().getValue()).isEqualTo(80); // 100 - 20
    }

    @Test
    @DisplayName("예약된 수량보다 많이 해제 시 예외 발생")
    void releaseReservation_exceedsReserved_throwsException() {
        // given
        Inventory inventory = createTestInventory();
        inventory.reserve(30);

        // when & then
        assertThatThrownBy(() -> inventory.releaseReservation(50))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("예약 수량: 30, 해제 요청: 50")
                .extracting(e -> ((InventoryException) e).getErrorCode())
                .isEqualTo(InventoryErrorCode.CANNOT_RELEASE_MORE_THAN_RESERVED);
    }

    @Test
    @DisplayName("출고 확정")
    void confirmShipment() {
        // given
        Inventory inventory = createTestInventory();
        inventory.reserve(30);
        Integer beforeQuantity = inventory.getQuantityInHub().getValue();

        // when
        inventory.confirmShipment(30);

        // then
        assertThat(inventory.getReservedQuantity().getValue()).isZero();
        assertThat(inventory.getQuantityInHub().getValue()).isEqualTo(beforeQuantity - 30);
        assertThat(inventory.getAvailableQuantity().getValue()).isEqualTo(70);
    }

    @Test
    @DisplayName("예약 없이 출고 확정 시 예외 발생")
    void confirmShipment_withoutReservation_throwsException() {
        // given
        Inventory inventory = createTestInventory();

        // when & then
        assertThatThrownBy(() -> inventory.confirmShipment(30))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("예약된 수량보다");
    }

    @Test
    @DisplayName("재고 조정")
    void adjust() {
        // given
        Inventory inventory = createTestInventory();
        Integer newQuantity = 80;

        // when
        inventory.adjust(newQuantity, "실사 후 조정");

        // then
        assertThat(inventory.getQuantityInHub().getValue()).isEqualTo(newQuantity);
    }

    @Test
    @DisplayName("음수로 재고 조정 시 예외 발생")
    void adjust_withNegative_throwsException() {
        // given
        Inventory inventory = createTestInventory();

        // when & then
        assertThatThrownBy(() -> inventory.adjust(-10, "오류"))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("조정 후 수량은 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("안전 재고 설정")
    void setSafetyStock() {
        // given
        Inventory inventory = createTestInventory();
        Integer newSafetyStock = 20;

        // when
        inventory.setSafetyStock(newSafetyStock);

        // then
        assertThat(inventory.getSafetyStock().getValue()).isEqualTo(newSafetyStock);
    }

    @Test
    @DisplayName("재주문 시점 설정")
    void setReorderPoint() {
        // given
        Inventory inventory = createTestInventory();
        Integer newReorderPoint = 20;

        // when
        inventory.setReorderPoint(newReorderPoint);

        // then
        assertThat(inventory.getReorderPoint().getValue()).isEqualTo(newReorderPoint);
    }

    @Test
    @DisplayName("안전 재고보다 낮은 재주문 시점 설정 시 예외 발생")
    void setReorderPoint_belowSafetyStock_throwsException() {
        // given
        Inventory inventory = createTestInventory();
        inventory.setSafetyStock(20);

        // when & then
        assertThatThrownBy(() -> inventory.setReorderPoint(10))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("재주문 시점은 안전 재고보다 크거나 같아야")
                .extracting(e -> ((InventoryException) e).getErrorCode())
                .isEqualTo(InventoryErrorCode.INVALID_REORDER_POINT);
    }

    @Test
    @DisplayName("위치 변경")
    void changeLocation() {
        // given
        Inventory inventory = createTestInventory();
        String newLocation = "B-2-5";

        // when
        inventory.changeLocation(newLocation);

        // then
        assertThat(inventory.getLocation()).isEqualTo(newLocation);
    }

    @Test
    @DisplayName("가용 재고 계산")
    void getAvailableQuantity() {
        // given
        Inventory inventory = createTestInventory();
        inventory.reserve(30);

        // when
        StockQuantity available = inventory.getAvailableQuantity();

        // then
        assertThat(available.getValue()).isEqualTo(70); // 100 - 30
    }

    @Test
    @DisplayName("안전 재고 이하 확인")
    void isBelowSafetyStock() {
        // given
        Inventory inventory = createTestInventory();

        // when & then
        assertThat(inventory.isBelowSafetyStock()).isFalse(); // 100 > 10

        inventory.adjust(10, "테스트");
        assertThat(inventory.isBelowSafetyStock()).isTrue(); // 10 <= 10

        inventory.adjust(5, "테스트");
        assertThat(inventory.isBelowSafetyStock()).isTrue(); // 5 < 10
    }

    @Test
    @DisplayName("재주문 시점 도달 확인")
    void needsReorder() {
        // given
        Inventory inventory = createTestInventory();

        // when & then
        assertThat(inventory.needsReorder()).isFalse(); // 100 > 10

        inventory.adjust(10, "테스트");
        assertThat(inventory.needsReorder()).isTrue(); // 10 <= 10
    }

    @Test
    @DisplayName("재고 없음 확인")
    void isOutOfStock() {
        // given
        Inventory inventory = createTestInventory();

        // when & then
        assertThat(inventory.isOutOfStock()).isFalse();

        inventory.reserve(100);
        assertThat(inventory.isOutOfStock()).isTrue(); // 가용 재고 0
    }

    @Test
    @DisplayName("소프트 삭제")
    void delete() {
        // given
        Inventory inventory = createTestInventory();
        String deletedBy = "ADMIN-001";

        // when
        inventory.delete(deletedBy);

        // then
        assertThat(inventory.isDeleted()).isTrue();
        assertThat(inventory.getDeletedBy()).isEqualTo(deletedBy);
        assertThat(inventory.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("소프트 삭제 복구")
    void restore() {
        // given
        Inventory inventory = createTestInventory();
        inventory.delete("ADMIN-001");

        // when
        inventory.restore();

        // then
        assertThat(inventory.isDeleted()).isFalse();
        assertThat(inventory.getDeletedBy()).isNull();
        assertThat(inventory.getDeletedAt()).isNull();
    }

    private Inventory createTestInventory() {
        return Inventory.create(
                "INV-001",
                "PROD-001",
                "HUB-SEOUL",
                100,
                10,
                "A-1-3"
        );
    }
}