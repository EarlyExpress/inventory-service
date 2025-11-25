package com.early_express.inventory_service.domain.inventory.presentation.internal;

import com.early_express.inventory_service.domain.inventory.application.dto.result.AvailabilityInfo;
import com.early_express.inventory_service.domain.inventory.application.dto.result.BulkAvailabilityInfo;
import com.early_express.inventory_service.domain.inventory.application.dto.result.ReservationInfo;
import com.early_express.inventory_service.domain.inventory.application.service.InventoryService;
import com.early_express.inventory_service.domain.inventory.presentation.internal.dto.request.CheckAvailabilityRequest;
import com.early_express.inventory_service.domain.inventory.presentation.internal.dto.request.InitializeInventoryRequest;
import com.early_express.inventory_service.domain.inventory.presentation.internal.dto.request.ReserveStockRequest;
import com.early_express.inventory_service.domain.inventory.presentation.internal.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 내부 API용 재고 컨트롤러 (최종 개선 버전)
 */
@Slf4j
@RestController
@RequestMapping("/v1/inventory/internal")
@RequiredArgsConstructor
public class InternalInventoryController {

    private final InventoryService inventoryService;

    /**
     * 재고 가용성 확인
     */
    @GetMapping("/products/{productId}/hubs/{hubId}/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable String productId,
            @PathVariable String hubId
    ) {
        log.info("재고 가용성 확인: productId={}, hubId={}", productId, hubId);

        AvailabilityInfo info = inventoryService.checkAvailability(productId, hubId);

        return ResponseEntity.ok(
                AvailabilityResponse.of(
                        info.getProductId(),
                        info.getHubId(),
                        info.isAvailable(),
                        info.getAvailableQuantity(),
                        info.getReservedQuantity(),
                        info.getTotalQuantity()
                )
        );
    }

    /**
     * 대량 재고 확인
     */
    @PostMapping("/products/check-availability")
    public ResponseEntity<BulkAvailabilityResponse> checkBulkAvailability(
            @Valid @RequestBody CheckAvailabilityRequest request
    ) {
        log.info("대량 재고 확인: hubId={}, itemCount={}", request.getHubId(), request.getItems().size());

        BulkAvailabilityInfo info = inventoryService.checkBulkAvailability(request.toCommand());

        List<BulkAvailabilityResponse.ItemAvailability> itemAvailabilities =
                info.getResults().stream()
                        .map(result -> BulkAvailabilityResponse.ItemAvailability.builder()
                                .productId(result.getProductId())
                                .requiredQuantity(result.getRequiredQuantity())
                                .availableQuantity(result.getAvailableQuantity())
                                .isAvailable(result.isAvailable())
                                .build())
                        .collect(Collectors.toList());

        return ResponseEntity.ok(
                BulkAvailabilityResponse.of(
                        info.getHubId(),
                        info.isAllAvailable(),
                        itemAvailabilities
                )
        );
    }

    /**
     * 재고 예약
     */
    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserveStock(
            @Valid @RequestBody ReserveStockRequest request
    ) {
        log.info("재고 예약 요청: orderId={}, itemCount={}", request.getOrderId(), request.getItems().size());

        String reservationId = UUID.randomUUID().toString();
        ReservationInfo info = inventoryService.reserveStock(request.toCommand());

        List<ReservationResponse.ReservedItem> reservedItems = info.getReservedItems().stream()
                .map(item -> ReservationResponse.ReservedItem.builder()
                        .productId(item.getProductId())
                        .hubId(item.getHubId())
                        .quantity(item.getQuantity())
                        .success(item.isSuccess())
                        .errorMessage(item.getErrorMessage())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.status(info.isAllSuccess() ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT)
                .body(ReservationResponse.of(reservationId, info.getOrderId(), info.isAllSuccess(), reservedItems));
    }

    /**
     * 재고 예약 해제
     */
    @DeleteMapping("/reservations/{orderId}")
    public ResponseEntity<ReleaseResponse> releaseReservation(
            @PathVariable String orderId,
            @RequestParam String productId,
            @RequestParam String hubId,
            @RequestParam Integer quantity
    ) {
        log.info("재고 예약 해제: orderId={}, productId={}, hubId={}", orderId, productId, hubId);

        inventoryService.releaseReservation(productId, hubId, quantity, orderId);

        return ResponseEntity.ok(
                ReleaseResponse.of(orderId, productId, hubId, quantity, true)
        );
    }

    /**
     * 출고 확정
     */
    @PostMapping("/reservations/{orderId}/confirm")
    public ResponseEntity<ConfirmResponse> confirmShipment(
            @PathVariable String orderId,
            @RequestParam String productId,
            @RequestParam String hubId,
            @RequestParam Integer quantity
    ) {
        log.info("출고 확정: orderId={}, productId={}, hubId={}", orderId, productId, hubId);

        inventoryService.confirmShipment(productId, hubId, quantity, orderId);

        return ResponseEntity.ok(
                ConfirmResponse.of(orderId, productId, hubId, quantity, true)
        );
    }

    /**
     * 상품별 전체 재고 조회
     */
    @GetMapping("/products/{productId}/inventories")
    public ResponseEntity<List<InternalInventoryResponse>> getProductInventories(
            @PathVariable String productId
    ) {
        log.info("상품별 재고 조회 (내부): productId={}", productId);

        List<InternalInventoryResponse> response = inventoryService.getInventoriesByProduct(productId)
                .stream()
                .map(InternalInventoryResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 초기 재고 생성
     */
    @PostMapping("/products/{productId}/initialize")
    public ResponseEntity<InitializeInventoryResponse> initializeInventory(
            @PathVariable String productId,
            @Valid @RequestBody InitializeInventoryRequest request
    ) {
        log.info("초기 재고 생성: productId={}, sellerId={}", productId, request.getSellerId());

        List<InitializeInventoryResponse.HubInventory> hubInventories =
                inventoryService.createInitialInventories(productId)
                        .stream()
                        .map(inv -> InitializeInventoryResponse.HubInventory.builder()
                                .inventoryId(inv.getInventoryId())
                                .hubId(inv.getHubId())
                                .totalQuantity(inv.getQuantityInHub().getValue())
                                .build())
                        .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InitializeInventoryResponse.of(productId, hubInventories));
    }

    /**
     * 재고 존재 확인
     */
    @GetMapping("/inventories/{inventoryId}/exists")
    public ResponseEntity<ExistsResponse> checkInventoryExists(
            @PathVariable String inventoryId
    ) {
        log.info("재고 존재 확인: inventoryId={}", inventoryId);

        boolean exists = inventoryService.existsInventory(inventoryId);

        return ResponseEntity.ok(ExistsResponse.of(inventoryId, exists));
    }
}