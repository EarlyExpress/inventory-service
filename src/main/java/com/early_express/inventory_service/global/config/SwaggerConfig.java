package com.early_express.inventory_service.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정 (프록시 환경용)
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Default Server API Documentation")
                        .description("Early Express Default Server REST API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Early Express Team")
                                .email("dev@early-express.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:4000")
                                .description("로컬 개발 서버 (프록시)"),
                        new Server()
                                .url("/")
                                .description("현재 서버 (상대 경로)")
                ));
    }
}