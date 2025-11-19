package com.early_express.inventory_service.domain.inventory.presentation.web;

import com.early_express.inventory_service.domain.inventory.application.dto.command.AdjustCommand;
import com.early_express.inventory_service.domain.inventory.application.service.InventoryService;
import com.early_express.inventory_service.domain.inventory.domain.model.Inventory;
import com.early_express.inventory_service.domain.inventory.presentation.web.dto.request.AdjustInventoryRequest;
import com.early_express.inventory_service.domain.inventory.presentation.web.dto.request.RestockRequest;
import com.early_express.inventory_service.domain.inventory.presentation.web.dto.request.UpdateLocationRequest;
import com.early_express.inventory_service.domain.inventory.presentation.web.dto.request.UpdateSafetyStockRequest;
import com.early_express.inventory_service.domain.inventory.presentation.web.dto.response.AdjustmentResponse;
import com.early_express.inventory_service.domain.inventory.presentation.web.dto.response.InventoryResponse;
import com.early_express.inventory_service.global.common.utils.PageUtils;
import com.early_express.inventory_service.global.presentation.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 생산업체용 재고 컨트롤러 (최종 개선 버전)
 */
@Slf4j
@RestController
@RequestMapping("/v1/inventory/web/producer")
@RequiredArgsConstructor
public class ProducerInventoryController {

    private final InventoryService inventoryService;

    /**
     * 재입고
     */
    @PostMapping("/restock")
    public ResponseEntity<InventoryResponse> restock(
            @RequestHeader("X-User-Id") String sellerId,
            @Valid @RequestBody RestockRequest request
    ) {
        log.info("재입고 요청: sellerId={}, productId={}, hubId={}",
                sellerId, request.getProductId(), request.getHubId());

        Inventory inventory = inventoryService.restock(request.toCommand());

        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    /**
     * 내 상품의 재고 현황 조회 (전체 허브)
     */
    @GetMapping("/products/{productId}/inventories")
    public ResponseEntity<List<InventoryResponse>> getProductInventories(
            @RequestHeader("X-User-Id") String sellerId,
            @PathVariable String productId
    ) {
        log.info("상품별 재고 조회: sellerId={}, productId={}", sellerId, productId);

        List<InventoryResponse> response = inventoryService.getInventoriesByProduct(productId)
                .stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 허브의 내 재고 조회
     */
    @GetMapping("/hubs/{hubId}/inventories")
    public ResponseEntity<PageResponse<InventoryResponse>> getMyInventoriesInHub(
            @RequestHeader("X-User-Id") String sellerId,
            @PathVariable String hubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("허브별 내 재고 조회: sellerId={}, hubId={}", sellerId, hubId);

        Page<Inventory> inventoryPage = inventoryService.getInventoriesByHub(hubId, page, size);

        return ResponseEntity.ok(
                PageUtils.toPageResponse(inventoryPage, InventoryResponse::from)
        );
    }

    /**
     * 재고 조정
     */
    @PutMapping("/inventories/{inventoryId}/adjust")
    public ResponseEntity<AdjustmentResponse> adjustInventory(
            @RequestHeader("X-User-Id") String sellerId,
            @PathVariable String inventoryId,
            @Valid @RequestBody AdjustInventoryRequest request
    ) {
        log.info("재고 조정 요청: sellerId={}, inventoryId={}", sellerId, inventoryId);

        Inventory beforeInventory = inventoryService.getInventory(inventoryId);
        Integer previousQuantity = beforeInventory.getQuantityInHub().getValue();

        AdjustCommand command = request.toCommand();
        Inventory adjustedInventory = inventoryService.adjustInventory(inventoryId, command);

        return ResponseEntity.ok(
                AdjustmentResponse.of(
                        adjustedInventory.getInventoryId(),
                        adjustedInventory.getProductId(),
                        adjustedInventory.getHubId(),
                        previousQuantity,
                        command.getAdjustmentQuantity(),
                        adjustedInventory.getQuantityInHub().getValue(),
                        command.getReason()
                )
        );
    }

    /**
     * 안전 재고 설정
     */
    @PutMapping("/inventories/{inventoryId}/safety-stock")
    public ResponseEntity<InventoryResponse> updateSafetyStock(
            @RequestHeader("X-User-Id") String sellerId,
            @PathVariable String inventoryId,
            @Valid @RequestBody UpdateSafetyStockRequest request
    ) {
        log.info("안전 재고 설정: sellerId={}, inventoryId={}", sellerId, inventoryId);

        Inventory inventory = inventoryService.updateSafetyStock(
                inventoryId,
                request.getSafetyStock()
        );

        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    /**
     * 위치 변경
     */
    @PutMapping("/inventories/{inventoryId}/location")
    public ResponseEntity<InventoryResponse> updateLocation(
            @RequestHeader("X-User-Id") String sellerId,
            @PathVariable String inventoryId,
            @Valid @RequestBody UpdateLocationRequest request
    ) {
        log.info("위치 변경: sellerId={}, inventoryId={}", sellerId, inventoryId);

        Inventory inventory = inventoryService.updateLocation(
                inventoryId,
                request.getLocation()
        );

        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }

    /**
     * 재고 상세 조회
     */
    @GetMapping("/inventories/{inventoryId}")
    public ResponseEntity<InventoryResponse> getInventory(
            @RequestHeader("X-User-Id") String sellerId,
            @PathVariable String inventoryId
    ) {
        log.info("재고 상세 조회: sellerId={}, inventoryId={}", sellerId, inventoryId);

        Inventory inventory = inventoryService.getInventory(inventoryId);

        return ResponseEntity.ok(InventoryResponse.from(inventory));
    }
}