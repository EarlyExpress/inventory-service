package com.early_express.inventory_service.domain.inventory.presentation.web.dto.request;

import com.early_express.inventory_service.domain.inventory.application.dto.command.AdjustCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 조정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustInventoryRequest {

    @NotNull(message = "조정 수량은 필수입니다.")
    private Integer adjustmentQuantity;  // 음수면 차감, 양수면 증가

    @NotBlank(message = "조정 사유는 필수입니다.")
    private String reason;

    /**
     * Request DTO → Domain Command Object
     */
    public AdjustCommand toCommand() {
        return AdjustCommand.builder()
                .adjustmentQuantity(adjustmentQuantity)
                .reason(reason)
                .build();
    }
}