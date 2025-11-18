package com.early_express.inventory_service.domain.inventory.application.service;

import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryErrorCode;
import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryException;
import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.domain.repository.InventoryRepository;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryCreatedEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryLowStockEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.InventoryRestockedEvent;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.producer.InventoryEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Inventory Application Service
 * - 유스케이스 처리
 * - 트랜잭션 관리
 * - 이벤트 발행
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer eventProducer;

    // 시스템에 등록된 허브 목록 (실제로는 Hub 서비스에서 가져와야 함)
    private static final List<String> AVAILABLE_HUBS = Arrays.asList(
            "HUB-SEOUL", "HUB-BUSAN", "HUB-INCHEON", "HUB-DAEGU"
    );

    /**
     * 초기 재고 생성 (Product 이벤트 수신 시 호출)
     * - 모든 허브에 초기 재고 0으로 생성
     */
    @Transactional
    public void createInitialInventories(String productId) {
        log.info("초기 재고 생성 시작: productId={}", productId);

        for (String hubId : AVAILABLE_HUBS) {
            // 이미 존재하는지 확인
            if (inventoryRepository.existsByProductIdAndHubId(productId, hubId)) {
                log.info("재고가 이미 존재함: productId={}, hubId={}", productId, hubId);
                continue;
            }

            // 초기 재고 생성
            Inventory inventory = Inventory.create(
                    null, // ID 자동 생성
                    productId,
                    hubId,
                    0, // 초기 수량 0
                    10, // 기본 안전 재고
                    "A-1-1" // 기본 위치
            );

            Inventory savedInventory = inventoryRepository.save(inventory);

            // 이벤트 발행
            eventProducer.publishInventoryCreated(savedInventory);

            log.info("초기 재고 생성 완료: inventoryId={}, productId={}, hubId={}",
                    savedInventory.getInventoryId(), productId, hubId);
        }
    }

    /**
     * 재입고
     */
    @Transactional
    public Inventory restock(String inventoryId, Integer quantity) {
        log.info("재입고 시작: inventoryId={}, quantity={}", inventoryId, quantity);

        Inventory inventory = findById(inventoryId);
        Integer previousQuantity = inventory.getQuantityInHub().getValue();

        // 재입고 처리
        inventory.restock(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 재입고 이벤트 발행
        eventProducer.publishInventoryRestocked(savedInventory,quantity);

        log.info("재입고 완료: inventoryId={}, 이전={}, 현재={}",
                inventoryId, previousQuantity, savedInventory.getQuantityInHub().getValue());

        return savedInventory;
    }

    /**
     * 재고 예약
     */
    @Transactional
    public Inventory reserveStock(String inventoryId, Integer quantity) {
        log.info("재고 예약 시작: inventoryId={}, quantity={}", inventoryId, quantity);

        Inventory inventory = findById(inventoryId);

        // 재고 예약
        inventory.reserve(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 안전 재고 체크
        checkAndPublishLowStockEvent(savedInventory);

        log.info("재고 예약 완료: inventoryId={}, 예약={}, 가용={}",
                inventoryId,
                savedInventory.getReservedQuantity().getValue(),
                savedInventory.getAvailableQuantity().getValue());

        return savedInventory;
    }

    /**
     * 예약 해제
     */
    @Transactional
    public Inventory releaseReservation(String inventoryId, Integer quantity) {
        log.info("예약 해제 시작: inventoryId={}, quantity={}", inventoryId, quantity);

        Inventory inventory = findById(inventoryId);

        // 예약 해제
        inventory.releaseReservation(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        log.info("예약 해제 완료: inventoryId={}, 예약={}, 가용={}",
                inventoryId,
                savedInventory.getReservedQuantity().getValue(),
                savedInventory.getAvailableQuantity().getValue());

        return savedInventory;
    }

    /**
     * 출고 확정
     */
    @Transactional
    public Inventory confirmShipment(String inventoryId, Integer quantity) {
        log.info("출고 확정 시작: inventoryId={}, quantity={}", inventoryId, quantity);

        Inventory inventory = findById(inventoryId);

        // 출고 확정
        inventory.confirmShipment(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 안전 재고 체크
        checkAndPublishLowStockEvent(savedInventory);

        log.info("출고 확정 완료: inventoryId={}, 전체={}, 예약={}, 가용={}",
                inventoryId,
                savedInventory.getQuantityInHub().getValue(),
                savedInventory.getReservedQuantity().getValue(),
                savedInventory.getAvailableQuantity().getValue());

        return savedInventory;
    }

    /**
     * 재고 조회
     */
    public Inventory getInventory(String inventoryId) {
        return findById(inventoryId);
    }

    /**
     * 상품-허브 조합으로 재고 조회
     */
    public Inventory getInventoryByProductAndHub(String productId, String hubId) {
        return inventoryRepository.findByProductIdAndHubId(productId, hubId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND));
    }

    /**
     * 상품별 전체 재고 조회
     */
    public List<Inventory> getInventoriesByProduct(String productId) {
        return inventoryRepository.findByProductId(productId);
    }

    /**
     * 안전 재고 이하 체크 및 이벤트 발행
     */
    private void checkAndPublishLowStockEvent(Inventory inventory) {
        if (inventory.isBelowSafetyStock()) {
            log.warn("안전 재고 이하 감지: inventoryId={}, productId={}, hubId={}, 현재={}, 안전={}",
                    inventory.getInventoryId(),
                    inventory.getProductId(),
                    inventory.getHubId(),
                    inventory.getQuantityInHub().getValue(),
                    inventory.getSafetyStock().getValue());

            // 재고 부족 이벤트 발행
            eventProducer.publishInventoryLowStock(inventory);
        }
    }

    /**
     * 내부 헬퍼 메서드 - 재고 조회
     */
    private Inventory findById(String inventoryId) {
        return inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND));
    }
}
