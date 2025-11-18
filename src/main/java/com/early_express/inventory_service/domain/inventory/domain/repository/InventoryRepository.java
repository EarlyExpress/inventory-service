package com.early_express.inventory_service.domain.inventory.domain.repository;

import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Inventory Repository 인터페이스 (포트)
 * - 도메인 계층에서 정의
 */
public interface InventoryRepository {

    /**
     * 재고 저장
     * - ID가 있으면 업데이트 (더티 체킹, 낙관적 락)
     * - ID가 없으면 신규 저장
     */
    Inventory save(Inventory inventory);

    /**
     * ID로 재고 조회 (삭제된 재고 제외)
     */
    Optional<Inventory> findById(String inventoryId);

    /**
     * 전체 재고 조회 (삭제된 재고 제외)
     */
    List<Inventory> findAll();

    /**
     * 상품별 재고 조회 (삭제된 재고 제외)
     */
    List<Inventory> findByProductId(String productId);

    /**
     * 허브별 재고 조회 (삭제된 재고 제외)
     */
    List<Inventory> findByHubId(String hubId);

    /**
     * 상품-허브 조합으로 재고 조회 (삭제된 재고 제외)
     */
    Optional<Inventory> findByProductIdAndHubId(String productId, String hubId);

    /**
     * 안전 재고 이하 재고 조회 (삭제된 재고 제외)
     */
    List<Inventory> findLowStock();

    /**
     * 페이징 조회 (삭제된 재고 제외)
     */
    Page<Inventory> findAllWithPaging(Pageable pageable);

    /**
     * 허브별 페이징 조회 (삭제된 재고 제외)
     */
    Page<Inventory> findByHubIdWithPaging(String hubId, Pageable pageable);

    /**
     * 소프트 삭제
     */
    void delete(String inventoryId);

    /**
     * 재고 존재 여부 확인 (삭제된 재고 제외)
     */
    boolean existsById(String inventoryId);

    /**
     * 상품-허브 조합 존재 여부 확인 (삭제된 재고 제외)
     */
    boolean existsByProductIdAndHubId(String productId, String hubId);
}
