package com.early_express.inventory_service.domain.inventory.infrastructure.persistence.jpa;

import com.early_express.inventory_service.domain.inventory.infrastructure.persistence.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Inventory JPA Repository
 * - Spring Data JPA 기본 인터페이스
 */
public interface InventoryJpaRepository extends JpaRepository<InventoryEntity, String> {
}
