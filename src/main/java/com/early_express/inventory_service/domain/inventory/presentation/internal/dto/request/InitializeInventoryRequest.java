package com.early_express.inventory_service.domain.inventory.presentation.internal.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 초기 재고 생성 요청 DTO (Internal API)
 * Product Service에서 상품 생성 시 호출
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeInventoryRequest {

    @NotBlank(message = "상품 ID는 필수입니다.")
    private String productId;

    @NotBlank(message = "판매자 ID는 필수입니다.")
    private String sellerId;
}