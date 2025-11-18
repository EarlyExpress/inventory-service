package com.early_express.inventory_service.global.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableFeignClients(basePackages = "com.early_express.default_server")
public class FeignConfig {

    /**
     * Feign 로깅 레벨 설정
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Request 옵션 설정
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                10, TimeUnit.SECONDS,  // connectTimeout
                30, TimeUnit.SECONDS,  // readTimeout
                true                   // followRedirects
        );
    }

    /**
     * 재시도 설정
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
                100,         // 재시도 간격 (ms)
                1000,        // 최대 재시도 간격 (ms)
                3            // 최대 재시도 횟수
        );
    }

    /**
     * 에러 디코더
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new ErrorDecoder.Default();
    }
}