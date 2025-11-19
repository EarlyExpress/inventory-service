package com.early_express.inventory_service.domain.inventory.presentation.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 위치 변경 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLocationRequest {

    @NotBlank(message = "위치는 필수입니다.")
    @Pattern(regexp = "^[A-Z]-\\d+-\\d+$", message = "위치 형식이 올바르지 않습니다. (예: A-1-3)")
    private String location;
}