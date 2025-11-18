package com.early_express.inventory_service.domain.inventory.infrastructure.persistence.repository;

import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.domain.repository.InventoryRepository;
import com.early_express.inventory_service.domain.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.early_express.inventory_service.domain.inventory.infrastructure.persistence.entity.QInventoryEntity;
import com.early_express.inventory_service.domain.inventory.infrastructure.persistence.jpa.InventoryJpaRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Inventory Repository 구현체 (어댑터)
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryRepositoryImpl implements InventoryRepository {

    private final InventoryJpaRepository jpaRepository;
    private final JPAQueryFactory queryFactory;

    private static final QInventoryEntity inventory = QInventoryEntity.inventoryEntity;

    /**
     * 재고 저장
     * - ID가 있으면 업데이트 (더티 체킹, 낙관적 락)
     * - ID가 없으면 신규 저장
     */
    @Override
    @Transactional
    public Inventory save(Inventory domain) {
        String inventoryId = domain.getInventoryId();

        // ID가 있으면 업데이트 (더티 체킹, 낙관적 락)
        if (inventoryId != null && !inventoryId.isBlank()) {
            Optional<InventoryEntity> existingEntity = jpaRepository.findById(inventoryId);
            if (existingEntity.isPresent()) {
                InventoryEntity entity = existingEntity.get();
                entity.updateFromDomain(domain);
                return entity.toDomain();
            }
        }

        // ID가 없거나 존재하지 않으면 신규 저장
        InventoryEntity entity = InventoryEntity.fromDomain(domain);
        InventoryEntity savedEntity = jpaRepository.save(entity);
        return savedEntity.toDomain();
    }

    /**
     * ID로 재고 조회 (삭제된 재고 제외)
     */
    @Override
    public Optional<Inventory> findById(String inventoryId) {
        return Optional.ofNullable(
                        queryFactory
                                .selectFrom(inventory)
                                .where(
                                        inventory.inventoryId.eq(inventoryId),
                                        inventory.isDeleted.eq(false)
                                )
                                .fetchOne()
                )
                .map(InventoryEntity::toDomain);
    }

    /**
     * 전체 재고 조회 (삭제된 재고 제외)
     */
    @Override
    public List<Inventory> findAll() {
        return queryFactory
                .selectFrom(inventory)
                .where(inventory.isDeleted.eq(false))
                .fetch()
                .stream()
                .map(InventoryEntity::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 상품별 재고 조회 (삭제된 재고 제외)
     */
    @Override
    public List<Inventory> findByProductId(String productId) {
        return queryFactory
                .selectFrom(inventory)
                .where(
                        inventory.productId.eq(productId),
                        inventory.isDeleted.eq(false)
                )
                .fetch()
                .stream()
                .map(InventoryEntity::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 허브별 재고 조회 (삭제된 재고 제외)
     */
    @Override
    public List<Inventory> findByHubId(String hubId) {
        return queryFactory
                .selectFrom(inventory)
                .where(
                        inventory.hubId.eq(hubId),
                        inventory.isDeleted.eq(false)
                )
                .fetch()
                .stream()
                .map(InventoryEntity::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 상품-허브 조합으로 재고 조회 (삭제된 재고 제외)
     */
    @Override
    public Optional<Inventory> findByProductIdAndHubId(String productId, String hubId) {
        return Optional.ofNullable(
                        queryFactory
                                .selectFrom(inventory)
                                .where(
                                        inventory.productId.eq(productId),
                                        inventory.hubId.eq(hubId),
                                        inventory.isDeleted.eq(false)
                                )
                                .fetchOne()
                )
                .map(InventoryEntity::toDomain);
    }

    /**
     * 안전 재고 이하 재고 조회 (삭제된 재고 제외)
     */
    @Override
    public List<Inventory> findLowStock() {
        return queryFactory
                .selectFrom(inventory)
                .where(
                        inventory.quantityInHub.loe(inventory.safetyStock),
                        inventory.isDeleted.eq(false)
                )
                .fetch()
                .stream()
                .map(InventoryEntity::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 페이징 조회 (삭제된 재고 제외)
     */
    @Override
    public Page<Inventory> findAllWithPaging(Pageable pageable) {
        List<InventoryEntity> entities = queryFactory
                .selectFrom(inventory)
                .where(inventory.isDeleted.eq(false))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(inventory.createdAt.desc())
                .fetch();

        long total = queryFactory
                .selectFrom(inventory)
                .where(inventory.isDeleted.eq(false))
                .fetchCount();

        List<Inventory> inventories = entities.stream()
                .map(InventoryEntity::toDomain)
                .collect(Collectors.toList());

        return new PageImpl<>(inventories, pageable, total);
    }

    /**
     * 허브별 페이징 조회 (삭제된 재고 제외)
     */
    @Override
    public Page<Inventory> findByHubIdWithPaging(String hubId, Pageable pageable) {
        List<InventoryEntity> entities = queryFactory
                .selectFrom(inventory)
                .where(
                        inventory.hubId.eq(hubId),
                        inventory.isDeleted.eq(false)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(inventory.createdAt.desc())
                .fetch();

        long total = queryFactory
                .selectFrom(inventory)
                .where(
                        inventory.hubId.eq(hubId),
                        inventory.isDeleted.eq(false)
                )
                .fetchCount();

        List<Inventory> inventories = entities.stream()
                .map(InventoryEntity::toDomain)
                .collect(Collectors.toList());

        return new PageImpl<>(inventories, pageable, total);
    }

    /**
     * 소프트 삭제
     */
    @Override
    @Transactional
    public void delete(String inventoryId) {
        InventoryEntity entity = jpaRepository.findById(inventoryId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + inventoryId));

        entity.delete(null); // deletedBy는 Service에서 처리 가능
    }

    /**
     * 재고 존재 여부 확인 (삭제된 재고 제외)
     */
    @Override
    public boolean existsById(String inventoryId) {
        return queryFactory
                .selectFrom(inventory)
                .where(
                        inventory.inventoryId.eq(inventoryId),
                        inventory.isDeleted.eq(false)
                )
                .fetchFirst() != null;
    }

    /**
     * 상품-허브 조합 존재 여부 확인 (삭제된 재고 제외)
     */
    @Override
    public boolean existsByProductIdAndHubId(String productId, String hubId) {
        return queryFactory
                .selectFrom(inventory)
                .where(
                        inventory.productId.eq(productId),
                        inventory.hubId.eq(hubId),
                        inventory.isDeleted.eq(false)
                )
                .fetchFirst() != null;
    }
}
