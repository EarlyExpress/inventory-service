package com.early_express.inventory_service.domain.inventory.presentation.web;

import com.early_express.inventory_service.domain.inventory.application.service.InventoryService;
import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.presentation.web.dto.response.InventoryResponse;
import com.early_express.inventory_service.global.common.utils.PageUtils;
import com.early_express.inventory_service.global.presentation.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 운영자용 재고 컨트롤러
 * - 전체 재고 관리, 모니터링
 *
 * TODO: ADMIN 권한 확인 필요
 */
@Slf4j
@RestController
@RequestMapping("/v1/inventory/web/admin")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    /**
     * 전체 재고 조회 (페이징)
     */
    @GetMapping("/inventories")
    public ResponseEntity<PageResponse<InventoryResponse>> getAllInventories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("전체 재고 조회 요청: page={}, size={}", page, size);

        Page<Inventory> inventoryPage = inventoryService.getAllInventories(page, size);

        PageResponse<InventoryResponse> response = PageUtils.toPageResponse(
                inventoryPage,
                InventoryResponse::from
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 허브별 재고 현황
     */
    @GetMapping("/hubs/{hubId}/inventories")
    public ResponseEntity<PageResponse<InventoryResponse>> getHubInventories(
            @PathVariable String hubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("허브별 재고 조회: hubId={}, page={}, size={}", hubId, page, size);

        Page<Inventory> inventoryPage = inventoryService.getInventoriesByHub(hubId, page, size);

        PageResponse<InventoryResponse> response = PageUtils.toPageResponse(
                inventoryPage,
                InventoryResponse::from
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 품절 상품 목록
     */
    @GetMapping("/out-of-stock")
    public ResponseEntity<List<InventoryResponse>> getOutOfStockInventories() {
        log.info("품절 상품 조회 요청");

        List<Inventory> inventories = inventoryService.getOutOfStockInventories();

        List<InventoryResponse> response = inventories.stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 안전 재고 이하 상품 목록
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockInventories() {
        log.info("안전 재고 이하 상품 조회 요청");

        List<Inventory> inventories = inventoryService.getLowStockInventories();

        List<InventoryResponse> response = inventories.stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 재고 상세 조회 (관리자용)
     */
    @GetMapping("/inventories/{inventoryId}")
    public ResponseEntity<InventoryResponse> getInventory(
            @PathVariable String inventoryId
    ) {
        log.info("재고 상세 조회 (관리자): inventoryId={}", inventoryId);

        Inventory inventory = inventoryService.getInventory(inventoryId);
        InventoryResponse response = InventoryResponse.from(inventory);

        return ResponseEntity.ok(response);
    }

    /**
     * 상품별 재고 현황 (관리자용)
     */
    @GetMapping("/products/{productId}/inventories")
    public ResponseEntity<List<InventoryResponse>> getProductInventories(
            @PathVariable String productId
    ) {
        log.info("상품별 재고 조회 (관리자): productId={}", productId);

        List<Inventory> inventories = inventoryService.getInventoriesByProduct(productId);

        List<InventoryResponse> response = inventories.stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // TODO: 재고 이동 (허브 간) 기능 추가
    // POST /admin/inventories/transfer
    // Request: { productId, fromHubId, toHubId, quantity }
}