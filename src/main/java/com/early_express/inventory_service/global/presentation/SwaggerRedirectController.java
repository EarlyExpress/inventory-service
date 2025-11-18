package com.early_express.inventory_service.global.presentation;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트 경로를 Swagger UI로 리다이렉트하는 컨트롤러
 */
@Controller
@Hidden // Swagger 문서에서 숨김
public class SwaggerRedirectController {

    /**
     * 루트 경로(/) 접근 시 Swagger UI로 리다이렉트
     */
    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui/index.html";
    }
}