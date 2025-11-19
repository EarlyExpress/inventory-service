package com.early_express.inventory_service.domain.inventory.presentation.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 안전 재고 설정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSafetyStockRequest {

    @NotNull(message = "안전 재고는 필수입니다.")
    @Min(value = 0, message = "안전 재고는 0 이상이어야 합니다.")
    private Integer safetyStock;
}