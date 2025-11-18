package com.early_express.inventory_service.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${webclient.max-memory-size:10485760}") // 10MB
    private int maxMemorySize;

    /**
     * 기본 WebClient - 외부 API 호출용
     */
    @Bean
    public WebClient webClient() {
        return createWebClient()
                .baseUrl("")
                .build();
    }

    /**
     * LoadBalanced WebClient - 내부 MSA 서비스 호출용
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return createWebClient();
    }

    /**
     * WebClient Builder 생성
     */
    private WebClient.Builder createWebClient() {
        // Exchange Strategies 설정 (메모리 제한)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(maxMemorySize);
                    configurer.defaultCodecs().enableLoggingRequestDetails(true);
                })
                .build();

        return WebClient.builder()
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .filter(handleError());
    }

    /**
     * Request 로깅 필터
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) ->
                    values.forEach(value -> log.debug("Request Header: {}={}", name, value))
            );
            return Mono.just(clientRequest);
        });
    }

    /**
     * Response 로깅 필터
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("Response Status: {}", clientResponse.statusCode());
            clientResponse.headers().asHttpHeaders().forEach((name, values) ->
                    values.forEach(value -> log.debug("Response Header: {}={}", name, value))
            );
            return Mono.just(clientResponse);
        });
    }

    /**
     * 에러 처리 필터
     */
    private ExchangeFilterFunction handleError() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is5xxServerError()) {
                log.error("Server error: {}", clientResponse.statusCode());
            } else if (clientResponse.statusCode().is4xxClientError()) {
                log.warn("Client error: {}", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }
}