package com.early_express.inventory_service.domain.inventory.application.service;

import com.early_express.inventory_service.domain.inventory.application.dto.command.*;
import com.early_express.inventory_service.domain.inventory.application.dto.result.*;
import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryErrorCode;
import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryException;
import com.early_express.inventory_service.domain.inventory.domain.messaging.InventoryEventPublisher;
import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.domain.model.vo.StockQuantity;
import com.early_express.inventory_service.domain.inventory.domain.repository.InventoryRepository;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.producer.InventoryEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Inventory Application Service (최종 개선 버전)
 * - Application Layer DTO 사용
 * - Presentation Layer와 완전히 독립
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    private final InventoryEventPublisher eventPublisher;


    private static final List<String> AVAILABLE_HUBS = Arrays.asList(
            "HUB-SEOUL", "HUB-BUSAN", "HUB-INCHEON", "HUB-DAEGU"
    );


    // ==================== 명령(Command) 메서드 ====================

    /**
     * 특정 허브에 초기 재고 생성
     */
    @Transactional
    public Inventory createInitialInventory(String productId, String hubId) {
        log.info("초기 재고 생성 시작: productId={}, hubId={}", productId, hubId);

        if (inventoryRepository.existsByProductIdAndHubId(productId, hubId)) {
            log.info("재고가 이미 존재함: productId={}, hubId={}", productId, hubId);
            return inventoryRepository.findByProductIdAndHubId(productId, hubId)
                    .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND));
        }

        Inventory inventory = Inventory.create(null, productId, hubId, 0, 10, "A-1-1");
        Inventory savedInventory = inventoryRepository.save(inventory);
        eventPublisher.publishInventoryCreated(savedInventory);

        log.info("초기 재고 생성 완료: inventoryId={}", savedInventory.getInventoryId());

        return savedInventory;
    }

    // 기존 메서드는 유지 (다른 용도로 사용될 수 있음)
    @Transactional
    public List<Inventory> createInitialInventories(String productId) {
        log.info("모든 허브에 초기 재고 생성 시작: productId={}", productId);

        List<Inventory> createdInventories = new ArrayList<>();

        for (String hubId : AVAILABLE_HUBS) {

            if (inventoryRepository.existsByProductIdAndHubId(productId, hubId)) {
                log.info("재고가 이미 존재함: productId={}, hubId={}", productId, hubId);
                continue;
            }


            Inventory inventory = Inventory.create(null, productId, hubId, 0, 10, "A-1-1");
            Inventory savedInventory = inventoryRepository.save(inventory);
            createdInventories.add(savedInventory);
            eventPublisher.publishInventoryCreated(savedInventory);

            log.info("초기 재고 생성 완료: inventoryId={}", savedInventory.getInventoryId());
        }

        return createdInventories;
    }

    /**
     * 상품 삭제 시 재고 삭제
     */
    @Transactional
    public void deleteInventoriesByProduct(String productId) {
        log.info("상품 재고 삭제 시작: productId={}", productId);

        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        inventories.forEach(inv -> inventoryRepository.delete(inv.getInventoryId()));

        log.info("상품 재고 삭제 완료: productId={}, 삭제 개수={}", productId, inventories.size());

    }

    /**
     * 재입고
     */
    @Transactional
    public Inventory restock(RestockCommand command) {
        log.info("재입고 시작: productId={}, hubId={}, quantity={}",
                command.getProductId(), command.getHubId(), command.getQuantity());

        Inventory inventory = getInventoryByProductAndHub(command.getProductId(), command.getHubId());
        Integer previousQuantity = inventory.getQuantityInHub().getValue();

        inventory.restock(command.getQuantity());
        Inventory savedInventory = inventoryRepository.save(inventory);

        eventPublisher.publishInventoryRestocked(savedInventory, command.getQuantity());

        log.info("재입고 완료: inventoryId={}, 이전={}, 현재={}",
                savedInventory.getInventoryId(), previousQuantity, savedInventory.getQuantityInHub().getValue());

        return savedInventory;
    }

    /**
     * 재고 예약
     */
    @Transactional
    public ReservationInfo reserveStock(ReservationCommand command) {
        log.info("재고 예약 시작: orderId={}, itemCount={}", command.getOrderId(), command.getItems().size());

        List<ReservationInfo.ReservedItemInfo> reservedItems = new ArrayList<>();
        boolean allSuccess = true;

        for (ReservationCommand.ReservationItem item : command.getItems()) {
            try {
                Inventory inventory = getInventoryByProductAndHub(item.getProductId(), item.getHubId());
                inventory.reserve(item.getQuantity());
                inventoryRepository.save(inventory);

                eventPublisher.publishInventoryReserved(inventory, command.getOrderId(), item.getQuantity());
                checkAndPublishLowStockEvent(inventory);

                reservedItems.add(ReservationInfo.ReservedItemInfo.builder()
                        .productId(item.getProductId())
                        .hubId(item.getHubId())
                        .quantity(item.getQuantity())
                        .success(true)
                        .build());

            } catch (Exception e) {
                log.error("재고 예약 실패: productId={}, error={}", item.getProductId(), e.getMessage());
                allSuccess = false;

                reservedItems.add(ReservationInfo.ReservedItemInfo.builder()
                        .productId(item.getProductId())
                        .hubId(item.getHubId())
                        .quantity(item.getQuantity())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        log.info("재고 예약 완료: orderId={}, allSuccess={}", command.getOrderId(), allSuccess);

        return ReservationInfo.builder()
                .orderId(command.getOrderId())
                .allSuccess(allSuccess)
                .reservedItems(reservedItems)
                .build();
    }

    /**
     * 예약 해제
     */
    @Transactional
    public Inventory releaseReservation(String productId, String hubId, Integer quantity, String orderId) {
        log.info("예약 해제: productId={}, hubId={}, quantity={}, orderId={}",
                productId, hubId, quantity, orderId);

        Inventory inventory = getInventoryByProductAndHub(productId, hubId);
        inventory.releaseReservation(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        eventPublisher.publishStockRestored(savedInventory, orderId, quantity);

        log.info("예약 해제 완료: inventoryId={}", savedInventory.getInventoryId());

        return savedInventory;
    }

    /**
     * 출고 확정
     */
    @Transactional
    public Inventory confirmShipment(String productId, String hubId, Integer quantity, String orderId) {
        log.info("출고 확정: productId={}, hubId={}, quantity={}, orderId={}",
                productId, hubId, quantity, orderId);

        Inventory inventory = getInventoryByProductAndHub(productId, hubId);
        inventory.confirmShipment(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);

        eventPublisher.publishStockDecreased(savedInventory, orderId, quantity);
        checkAndPublishLowStockEvent(savedInventory);

        log.info("출고 확정 완료: inventoryId={}", savedInventory.getInventoryId());

        return savedInventory;
    }

    /**
     * 재고 조정
     */
    @Transactional
    public Inventory adjustInventory(String inventoryId, AdjustCommand command) {
        log.info("재고 조정: inventoryId={}, adjustment={}, reason={}",
                inventoryId, command.getAdjustmentQuantity(), command.getReason());

        Inventory inventory = findById(inventoryId);
        Integer previousQuantity = inventory.getQuantityInHub().getValue();
        int newQuantity = previousQuantity + command.getAdjustmentQuantity();

        inventory.adjust(newQuantity, command.getReason());
        Inventory savedInventory = inventoryRepository.save(inventory);

        log.info("재고 조정 완료: inventoryId={}", inventoryId);

        return savedInventory;
    }

    /**
     * 안전 재고 설정
     */
    @Transactional
    public Inventory updateSafetyStock(String inventoryId, Integer safetyStock) {
        log.info("안전 재고 설정: inventoryId={}, safetyStock={}", inventoryId, safetyStock);

        Inventory inventory = findById(inventoryId);
        inventory.setSafetyStock(safetyStock);

        return inventoryRepository.save(inventory);
    }

    /**
     * 위치 변경
     */
    @Transactional
    public Inventory updateLocation(String inventoryId, String newLocation) {
        log.info("위치 변경: inventoryId={}, newLocation={}", inventoryId, newLocation);

        Inventory inventory = findById(inventoryId);
        inventory.changeLocation(newLocation);

        return inventoryRepository.save(inventory);
    }

    // ==================== 조회(Query) 메서드 ====================

    public Inventory getInventory(String inventoryId) {
        return findById(inventoryId);
    }

    public List<Inventory> getInventoriesByProduct(String productId) {
        return inventoryRepository.findByProductId(productId);
    }

    public Page<Inventory> getInventoriesByHub(String hubId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return inventoryRepository.findByHubIdWithPaging(hubId, pageable);
    }

    public Page<Inventory> getAllInventories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return inventoryRepository.findAllWithPaging(pageable);
    }

    public List<Inventory> getOutOfStockInventories() {
        return inventoryRepository.findAll().stream()
                .filter(Inventory::isOutOfStock)
                .toList();
    }

    public List<Inventory> getLowStockInventories() {
        return inventoryRepository.findLowStock();
    }

    public Inventory getInventoryByProductAndHub(String productId, String hubId) {
        return inventoryRepository.findByProductIdAndHubId(productId, hubId)
                .orElseThrow(() -> new InventoryException(
                        InventoryErrorCode.INVENTORY_NOT_FOUND,
                        String.format("재고를 찾을 수 없습니다. productId=%s, hubId=%s", productId, hubId)
                ));
    }

    /**
     * 재고 가용성 확인
     */
    public AvailabilityInfo checkAvailability(String productId, String hubId) {
        log.info("재고 가용성 확인: productId={}, hubId={}", productId, hubId);

        try {
            Inventory inventory = getInventoryByProductAndHub(productId, hubId);
            StockQuantity available = inventory.getAvailableQuantity();

            return AvailabilityInfo.builder()
                    .productId(productId)
                    .hubId(hubId)
                    .isAvailable(!available.isZero())
                    .availableQuantity(available.getValue())
                    .reservedQuantity(inventory.getReservedQuantity().getValue())
                    .totalQuantity(inventory.getQuantityInHub().getValue())
                    .build();

        } catch (InventoryException e) {
            return AvailabilityInfo.builder()
                    .productId(productId)
                    .hubId(hubId)
                    .isAvailable(false)
                    .availableQuantity(0)
                    .reservedQuantity(0)
                    .totalQuantity(0)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 대량 재고 가용성 확인
     */
    public BulkAvailabilityInfo checkBulkAvailability(BulkCheckCommand command) {
        log.info("대량 재고 확인: hubId={}, itemCount={}", command.getHubId(), command.getItems().size());

        List<BulkAvailabilityInfo.ItemAvailabilityInfo> results = new ArrayList<>();
        boolean allAvailable = true;

        for (BulkCheckCommand.CheckItem item : command.getItems()) {
            AvailabilityInfo availabilityInfo = checkAvailability(item.getProductId(), command.getHubId());
            Integer availableQuantity = availabilityInfo.getAvailableQuantity();
            boolean isAvailable = availableQuantity >= item.getQuantity();

            results.add(BulkAvailabilityInfo.ItemAvailabilityInfo.builder()
                    .productId(item.getProductId())
                    .requiredQuantity(item.getQuantity())
                    .availableQuantity(availableQuantity)
                    .isAvailable(isAvailable)
                    .build());

            if (!isAvailable) {
                allAvailable = false;
            }
        }

        return BulkAvailabilityInfo.builder()
                .hubId(command.getHubId())
                .allAvailable(allAvailable)
                .results(results)
                .build();
    }

    public boolean existsInventory(String inventoryId) {
        return inventoryRepository.existsById(inventoryId);
    }

    // ==================== 내부 헬퍼 메서드 ====================

    private Inventory findById(String inventoryId) {
        return inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new InventoryException(InventoryErrorCode.INVENTORY_NOT_FOUND));
    }

    private void checkAndPublishLowStockEvent(Inventory inventory) {
        if (inventory.isBelowSafetyStock()) {
            log.warn("안전 재고 이하 감지: inventoryId={}", inventory.getInventoryId());
            eventPublisher.publishInventoryLowStock(inventory);
        }
    }
}

