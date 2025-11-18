package com.early_express.inventory_service.global.presentation.controller;

import com.early_express.inventory_service.global.infrastructure.security.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping
public class TestController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/test")
    public String test() {
        return "Default Server is working!";
    }

    @GetMapping("/auth/test")
    public void test(@AuthenticationPrincipal UserDetailsImpl userDetails) {
      log.info("userDetails: {}", userDetails);
    }
}
