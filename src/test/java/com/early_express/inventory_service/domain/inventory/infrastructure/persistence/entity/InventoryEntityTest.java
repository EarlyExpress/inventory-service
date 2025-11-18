package com.early_express.inventory_service.domain.inventory.infrastructure.persistence.entity;

import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.domain.model.vo.StockQuantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InventoryEntity 변환 테스트")
class InventoryEntityTest {

    @Test
    @DisplayName("Domain → Entity 변환")
    void fromDomain() {
        // given
        Inventory inventory = createTestInventory();

        // when
        InventoryEntity entity = InventoryEntity.fromDomain(inventory);

        // then
        assertThat(entity.getInventoryId()).isEqualTo(inventory.getInventoryId());
        assertThat(entity.getProductId()).isEqualTo(inventory.getProductId());
        assertThat(entity.getHubId()).isEqualTo(inventory.getHubId());
        assertThat(entity.getQuantityInHub()).isEqualTo(inventory.getQuantityInHub().getValue());
        assertThat(entity.getReservedQuantity()).isEqualTo(inventory.getReservedQuantity().getValue());
        assertThat(entity.getSafetyStock()).isEqualTo(inventory.getSafetyStock().getValue());
        assertThat(entity.getReorderPoint()).isEqualTo(inventory.getReorderPoint().getValue());
        assertThat(entity.getLocation()).isEqualTo(inventory.getLocation());
    }

    @Test
    @DisplayName("Entity → Domain 변환")
    void toDomain() {
        // given
        InventoryEntity entity = createTestEntity();

        // when
        Inventory inventory = entity.toDomain();

        // then
        assertThat(inventory.getInventoryId()).isEqualTo(entity.getInventoryId());
        assertThat(inventory.getProductId()).isEqualTo(entity.getProductId());
        assertThat(inventory.getHubId()).isEqualTo(entity.getHubId());
        assertThat(inventory.getQuantityInHub().getValue()).isEqualTo(entity.getQuantityInHub());
        assertThat(inventory.getReservedQuantity().getValue()).isEqualTo(entity.getReservedQuantity());
        assertThat(inventory.getSafetyStock().getValue()).isEqualTo(entity.getSafetyStock());
        assertThat(inventory.getReorderPoint().getValue()).isEqualTo(entity.getReorderPoint());
        assertThat(inventory.getLocation()).isEqualTo(entity.getLocation());
    }

    @Test
    @DisplayName("Domain → Entity → Domain 변환 후 데이터 일치")
    void domainToEntityToDomain_dataConsistency() {
        // given
        Inventory originalInventory = createTestInventory();

        // when
        InventoryEntity entity = InventoryEntity.fromDomain(originalInventory);
        Inventory convertedInventory = entity.toDomain();

        // then
        assertThat(convertedInventory.getInventoryId()).isEqualTo(originalInventory.getInventoryId());
        assertThat(convertedInventory.getProductId()).isEqualTo(originalInventory.getProductId());
        assertThat(convertedInventory.getHubId()).isEqualTo(originalInventory.getHubId());
        assertThat(convertedInventory.getQuantityInHub()).isEqualTo(originalInventory.getQuantityInHub());
        assertThat(convertedInventory.getLocation()).isEqualTo(originalInventory.getLocation());
    }

    @Test
    @DisplayName("Domain 변경사항을 Entity에 반영")
    void updateFromDomain() {
        // given
        Inventory inventory = createTestInventory();
        InventoryEntity entity = InventoryEntity.fromDomain(inventory);

        // when
        inventory.restock(50);
        inventory.reserve(30);
        inventory.changeLocation("B-2-5");
        entity.updateFromDomain(inventory);

        // then
        assertThat(entity.getQuantityInHub()).isEqualTo(150); // 100 + 50
        assertThat(entity.getReservedQuantity()).isEqualTo(30);
        assertThat(entity.getLocation()).isEqualTo("B-2-5");
    }

    @Test
    @DisplayName("Domain의 삭제 상태를 Entity에 반영")
    void updateFromDomain_withDeletedStatus() {
        // given
        Inventory inventory = createTestInventory();
        InventoryEntity entity = InventoryEntity.fromDomain(inventory);

        // when
        inventory.delete("ADMIN-001");
        entity.updateFromDomain(inventory);

        // then
        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedBy()).isEqualTo("ADMIN-001");
        assertThat(entity.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Domain의 복구 상태를 Entity에 반영")
    void updateFromDomain_withRestoredStatus() {
        // given
        Inventory inventory = createTestInventory();
        inventory.delete("ADMIN-001");
        InventoryEntity entity = InventoryEntity.fromDomain(inventory);
        entity.updateFromDomain(inventory);

        // when
        inventory.restore();
        entity.updateFromDomain(inventory);

        // then
        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getDeletedBy()).isNull();
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("Entity의 가용 재고 계산")
    void getAvailableQuantity() {
        // given
        InventoryEntity entity = createTestEntity();

        // when
        Integer available = entity.getAvailableQuantity();

        // then
        assertThat(available).isEqualTo(100); // 100 - 0

        // 예약 후
        Inventory inventory = entity.toDomain();
        inventory.reserve(30);
        entity.updateFromDomain(inventory);

        assertThat(entity.getAvailableQuantity()).isEqualTo(70); // 100 - 30
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

    private InventoryEntity createTestEntity() {
        return InventoryEntity.builder()
                .inventoryId("INV-001")
                .productId("PROD-001")
                .hubId("HUB-SEOUL")
                .quantityInHub(100)
                .reservedQuantity(0)
                .safetyStock(10)
                .reorderPoint(10)
                .location("A-1-3")
                .lastRestockedAt(LocalDateTime.now())
                .version(0L)
                .build();
    }
}