package com.early_express.inventory_service.domain.inventory.presentation.internal.dto.request;

import com.early_express.inventory_service.domain.inventory.application.dto.command.ReservationCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 재고 예약 요청 DTO (Presentation Layer)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockRequest {

    @NotBlank(message = "주문 ID는 필수입니다.")
    private String orderId;

    @NotEmpty(message = "예약할 상품 목록은 비어있을 수 없습니다.")
    @Valid
    private List<ReservationItem> items;

    /**
     * Presentation DTO → Application Command 변환
     */
    public ReservationCommand toCommand() {
        return ReservationCommand.builder()
                .orderId(orderId)
                .items(items.stream()
                        .map(item -> ReservationCommand.ReservationItem.builder()
                                .productId(item.getProductId())
                                .hubId(item.getHubId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationItem {
        @NotBlank(message = "상품 ID는 필수입니다.")
        private String productId;

        @NotBlank(message = "허브 ID는 필수입니다.")
        private String hubId;

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        private Integer quantity;
    }
}