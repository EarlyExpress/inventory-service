package com.early_express.inventory_service.domain.inventory.presentation.internal.dto.request;

import com.early_express.inventory_service.domain.inventory.application.dto.command.BulkCheckCommand;
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
 * 대량 재고 가용성 확인 요청 DTO (Presentation Layer)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckAvailabilityRequest {

    @NotBlank(message = "허브 ID는 필수입니다.")
    private String hubId;

    @NotEmpty(message = "확인할 상품 목록은 비어있을 수 없습니다.")
    @Valid
    private List<AvailabilityItem> items;

    /**
     * Presentation DTO → Application Command 변환
     */
    public BulkCheckCommand toCommand() {
        return BulkCheckCommand.builder()
                .hubId(hubId)
                .items(items.stream()
                        .map(item -> BulkCheckCommand.CheckItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityItem {
        @NotBlank(message = "상품 ID는 필수입니다.")
        private String productId;

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        private Integer quantity;
    }
}