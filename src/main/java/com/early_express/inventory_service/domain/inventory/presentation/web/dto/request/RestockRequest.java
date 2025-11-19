package com.early_express.inventory_service.domain.inventory.presentation.web.dto.request;

import com.early_express.inventory_service.domain.inventory.application.dto.command.RestockCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재입고 요청 DTO (Presentation Layer)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockRequest {

    @NotBlank(message = "상품 ID는 필수입니다.")
    private String productId;

    @NotBlank(message = "허브 ID는 필수입니다.")
    private String hubId;

    @NotNull(message = "재입고 수량은 필수입니다.")
    @Min(value = 1, message = "재입고 수량은 1 이상이어야 합니다.")
    private Integer quantity;

    /**
     * Presentation DTO → Application Command 변환
     */
    public RestockCommand toCommand() {
        return RestockCommand.builder()
                .productId(productId)
                .hubId(hubId)
                .quantity(quantity)
                .build();
    }
}