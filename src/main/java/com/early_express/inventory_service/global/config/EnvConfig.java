package com.early_express.inventory_service.global.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EnvConfig {

    @PostConstruct
    public void init() {
        // 환경별 .env 파일 로드
        String environment = System.getProperty("spring.profiles.active", "local");

        // 기본 .env 파일 로드
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .filename(".env")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        // 환경별 .env 파일 추가 로드
        Dotenv envSpecific = Dotenv.configure()
                .directory("./")
                .filename(".env." + environment)
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        // 기본 .env 설정 적용
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        // 환경별 .env 설정으로 덮어쓰기
        envSpecific.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        log.info("Environment configuration loaded: {}", environment);
    }
}