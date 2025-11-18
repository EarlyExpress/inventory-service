package com.early_express.inventory_service.domain.inventory.infrastructure.persistence.repository;

import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.domain.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("InventoryRepository 통합 테스트")
class InventoryRepositoryImplTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("재고 저장 - 신규")
    void save_newInventory() {
        // given
        Inventory inventory = createTestInventory(null, "PROD-001", "HUB-SEOUL");

        // when
        Inventory saved = inventoryRepository.save(inventory);

        // then
        assertThat(saved.getInventoryId()).isNotNull();
        assertThat(saved.getProductId()).isEqualTo("PROD-001");
        assertThat(saved.getHubId()).isEqualTo("HUB-SEOUL");
    }

    @Test
    @DisplayName("재고 저장 - 업데이트 (더티 체킹)")
    void save_updateInventory() {
        // given
        Inventory inventory = createTestInventory(null, "PROD-001", "HUB-SEOUL");
        Inventory saved = inventoryRepository.save(inventory);

        // when - 도메인 모델 수정
        Inventory updated = inventoryRepository.findById(saved.getInventoryId()).get();
        updated.restock(50);
        updated.reserve(30);

        // 저장 (더티 체킹)
        Inventory result = inventoryRepository.save(updated);

        // then
        assertThat(result.getInventoryId()).isEqualTo(saved.getInventoryId());
        assertThat(result.getQuantityInHub().getValue()).isEqualTo(150); // 100 + 50
        assertThat(result.getReservedQuantity().getValue()).isEqualTo(30);
    }

    @Test
    @DisplayName("ID로 재고 조회")
    void findById() {
        // given
        Inventory inventory = createTestInventory(null, "PROD-001", "HUB-SEOUL");
        Inventory saved = inventoryRepository.save(inventory);

        // when
        Optional<Inventory> found = inventoryRepository.findById(saved.getInventoryId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getInventoryId()).isEqualTo(saved.getInventoryId());
        assertThat(found.get().getProductId()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional 반환")
    void findById_notFound() {
        // when
        Optional<Inventory> found = inventoryRepository.findById("NOT-EXIST");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("상품별 재고 조회")
    void findByProductId() {
        // given
        inventoryRepository.save(createTestInventory(null, "PROD-001", "HUB-SEOUL"));
        inventoryRepository.save(createTestInventory(null, "PROD-001", "HUB-BUSAN"));
        inventoryRepository.save(createTestInventory(null, "PROD-002", "HUB-SEOUL"));

        // when
        List<Inventory> inventories = inventoryRepository.findByProductId("PROD-001");

        // then
        assertThat(inventories).hasSize(2);
        assertThat(inventories).allMatch(inv -> inv.getProductId().equals("PROD-001"));
    }

    @Test
    @DisplayName("허브별 재고 조회")
    void findByHubId() {
        // given
        inventoryRepository.save(createTestInventory(null, "PROD-001", "HUB-SEOUL"));
        inventoryRepository.save(createTestInventory(null, "PROD-002", "HUB-SEOUL"));
        inventoryRepository.save(createTestInventory(null, "PROD-003", "HUB-BUSAN"));

        // when
        List<Inventory> inventories = inventoryRepository.findByHubId("HUB-SEOUL");

        // then
        assertThat(inventories).hasSize(2);
        assertThat(inventories).allMatch(inv -> inv.getHubId().equals("HUB-SEOUL"));
    }

    @Test
    @DisplayName("상품-허브 조합으로 재고 조회")
    void findByProductIdAndHubId() {
        // given
        inventoryRepository.save(createTestInventory(null, "PROD-001", "HUB-SEOUL"));
        inventoryRepository.save(createTestInventory(null, "PROD-001", "HUB-BUSAN"));

        // when
        Optional<Inventory> found = inventoryRepository.findByProductIdAndHubId("PROD-001", "HUB-SEOUL");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo("PROD-001");
        assertThat(found.get().getHubId()).isEqualTo("HUB-SEOUL");
    }

    @Test
    @DisplayName("안전 재고 이하 재고 조회")
    void findLowStock() {
        // given
        Inventory inventory1 = createTestInventory(null, "PROD-001", "HUB-SEOUL");
        inventory1.adjust(5, "테스트"); // 안전재고(10) 이하
        inventoryRepository.save(inventory1);

        Inventory inventory2 = createTestInventory(null, "PROD-002", "HUB-SEOUL");
        inventoryRepository.save(inventory2); // 100개 (안전재고 이상)

        // when
        List<Inventory> lowStockInventories = inventoryRepository.findLowStock();

        // then
        assertThat(lowStockInventories).hasSize(1);
        assertThat(lowStockInventories.get(0).getProductId()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("페이징 조회")
    void findAllWithPaging() {
        // given
        for (int i = 0; i < 15; i++) {
            inventoryRepository.save(createTestInventory(null, "PROD-00" + i, "HUB-SEOUL"));
        }

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inventory> page = inventoryRepository.findAllWithPaging(pageable);

        // then
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(15);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("허브별 페이징 조회")
    void findByHubIdWithPaging() {
        // given
        for (int i = 0; i < 15; i++) {
            inventoryRepository.save(createTestInventory(null, "PROD-00" + i, "HUB-SEOUL"));
        }
        for (int i = 0; i < 5; i++) {
            inventoryRepository.save(createTestInventory(null, "PROD-10" + i, "HUB-BUSAN"));
        }

        // when
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inventory> page = inventoryRepository.findByHubIdWithPaging("HUB-SEOUL", pageable);

        // then
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(15);
        assertThat(page.getContent()).allMatch(inv -> inv.getHubId().equals("HUB-SEOUL"));
    }

    @Test
    @DisplayName("소프트 삭제")
    void delete() {
        // given
        Inventory inventory = createTestInventory(null, "PROD-001", "HUB-SEOUL");
        Inventory saved = inventoryRepository.save(inventory);
        String inventoryId = saved.getInventoryId();

        // when
        inventoryRepository.delete(inventoryId);

        // then
        Optional<Inventory> found = inventoryRepository.findById(inventoryId);
        assertThat(found).isEmpty(); // 소프트 삭제로 조회 안됨
    }

    @Test
    @DisplayName("삭제된 재고는 목록 조회에서 제외")
    void findAll_excludeDeleted() {
        // given
        Inventory inventory1 = createTestInventory(null, "PROD-001", "HUB-SEOUL");
        Inventory saved1 = inventoryRepository.save(inventory1);

        Inventory inventory2 = createTestInventory(null, "PROD-002", "HUB-SEOUL");
        inventoryRepository.save(inventory2);

        // when
        inventoryRepository.delete(saved1.getInventoryId());
        List<Inventory> all = inventoryRepository.findAll();

        // then
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getProductId()).isEqualTo("PROD-002");
    }

    @Test
    @DisplayName("재고 존재 여부 확인")
    void existsById() {
        // given
        Inventory inventory = createTestInventory(null, "PROD-001", "HUB-SEOUL");
        Inventory saved = inventoryRepository.save(inventory);

        // when
        boolean exists = inventoryRepository.existsById(saved.getInventoryId());
        boolean notExists = inventoryRepository.existsById("NOT-EXIST");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("상품-허브 조합 존재 여부 확인")
    void existsByProductIdAndHubId() {
        // given
        inventoryRepository.save(createTestInventory(null, "PROD-001", "HUB-SEOUL"));

        // when
        boolean exists = inventoryRepository.existsByProductIdAndHubId("PROD-001", "HUB-SEOUL");
        boolean notExists = inventoryRepository.existsByProductIdAndHubId("PROD-001", "HUB-BUSAN");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("삭제된 재고는 존재하지 않는 것으로 판단")
    void existsById_deletedInventory() {
        // given
        Inventory inventory = createTestInventory(null, "PROD-001", "HUB-SEOUL");
        Inventory saved = inventoryRepository.save(inventory);

        // when
        inventoryRepository.delete(saved.getInventoryId());
        boolean exists = inventoryRepository.existsById(saved.getInventoryId());

        // then
        assertThat(exists).isFalse();
    }

    private Inventory createTestInventory(String inventoryId, String productId, String hubId) {
        return Inventory.create(
                inventoryId,
                productId,
                hubId,
                100,
                10,
                "A-1-3"
        );
    }
}