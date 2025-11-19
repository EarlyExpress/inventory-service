package com.early_express.inventory_service.domain.inventory.infrastructure.persistence.entity;

import com.early_express.inventory_service.global.common.utils.UuidUtils;
import com.early_express.inventory_service.global.infrastructure.entity.BaseEntity;
import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.domain.model.vo.StockQuantity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Inventory JPA Entity
 * - BaseEntity 상속 (Audit 필드)
 * - Domain Model과 완전 분리
 * - @Version을 통한 낙관적 락 지원
 */
@Entity
@Table(
        name = "p_inventories",
        indexes = {
                @Index(name = "idx_product_hub", columnList = "product_id, hub_id"),
                @Index(name = "idx_hub_id", columnList = "hub_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryEntity extends BaseEntity {

    @Id
    @Column(name = "inventory_id", length = 36, nullable = false)
    private String inventoryId;

    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Column(name = "hub_id", length = 36, nullable = false)
    private String hubId;

    @Column(name = "quantity_in_hub", nullable = false)
    private Integer quantityInHub;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock;

    @Column(name = "reorder_point", nullable = false)
    private Integer reorderPoint;

    @Column(name = "location", length = 20, nullable = false)
    private String location;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Builder
    private InventoryEntity(
            String inventoryId,
            String productId,
            String hubId,
            Integer quantityInHub,
            Integer reservedQuantity,
            Integer safetyStock,
            Integer reorderPoint,
            String location,
            LocalDateTime lastRestockedAt,
            Long version
    ) {
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.hubId = hubId;
        this.quantityInHub = quantityInHub;
        this.reservedQuantity = reservedQuantity;
        this.safetyStock = safetyStock;
        this.reorderPoint = reorderPoint;
        this.location = location;
        this.lastRestockedAt = lastRestockedAt;
        this.version = version;
    }

    /**
     * Domain Model -> Entity 변환 (신규 생성)
     * ID가 없으면 자동 생성
     */
    public static InventoryEntity fromDomain(Inventory inventory) {
        String inventoryId = inventory.getInventoryId();
        if (inventoryId == null || inventoryId.isBlank()) {
            inventoryId = UuidUtils.generate();
        }

        return InventoryEntity.builder()
                .inventoryId(inventoryId)
                .productId(inventory.getProductId())
                .hubId(inventory.getHubId())
                .quantityInHub(inventory.getQuantityInHub().getValue())
                .reservedQuantity(inventory.getReservedQuantity().getValue())
                .safetyStock(inventory.getSafetyStock().getValue())
                .reorderPoint(inventory.getReorderPoint().getValue())
                .location(inventory.getLocation())
                .lastRestockedAt(inventory.getLastRestockedAt())
//                .version(inventory.getVersion())  //jpa레벨에서 버전을 0으로 넣으면 문제가 발생 -> 머지가 발생.
                .build();
    }

    /**
     * Domain Model -> Entity 변환 (업데이트 시 ID 포함)
     */
    public static InventoryEntity fromDomainWithId(Inventory inventory) {
        InventoryEntity entity = fromDomain(inventory);

        // BaseEntity의 삭제 정보 수동 설정
        if (inventory.isDeleted()) {
            entity.delete(inventory.getDeletedBy());
        }

        return entity;
    }

    /**
     * Entity -> Domain Model 변환
     */
    public Inventory toDomain() {
        return Inventory.reconstruct(
                this.inventoryId,
                this.productId,
                this.hubId,
                StockQuantity.of(this.quantityInHub),
                StockQuantity.of(this.reservedQuantity),
                StockQuantity.of(this.safetyStock),
                StockQuantity.of(this.reorderPoint),
                this.location,
                this.lastRestockedAt,
                this.version,
                this.getCreatedAt(),
                this.getCreatedBy(),
                this.getUpdatedAt(),
                this.getUpdatedBy(),
                this.getDeletedAt(),
                this.getDeletedBy(),
                this.isDeleted()
        );
    }

    /**
     * Domain Model의 변경사항을 Entity에 반영
     */
    public void updateFromDomain(Inventory inventory) {
        this.quantityInHub = inventory.getQuantityInHub().getValue();
        this.reservedQuantity = inventory.getReservedQuantity().getValue();
        this.safetyStock = inventory.getSafetyStock().getValue();
        this.reorderPoint = inventory.getReorderPoint().getValue();
        this.location = inventory.getLocation();
        this.lastRestockedAt = inventory.getLastRestockedAt();

        // 삭제 상태 동기화
        if (inventory.isDeleted() && !this.isDeleted()) {
            this.delete(inventory.getDeletedBy());
        } else if (!inventory.isDeleted() && this.isDeleted()) {
            this.restore();
        }
    }

    /**
     * 판매 가능 수량 계산 (편의 메서드)
     */
    public Integer getAvailableQuantity() {
        return this.quantityInHub - this.reservedQuantity;
    }
}