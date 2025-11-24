package com.early_express.inventory_service.global.infrastructure.event.base;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이벤트 기본 클래스
 * 모든 도메인 이벤트의 부모 클래스
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {

    /**
     * 이벤트 ID (멱등성 체크용)
     */
    private String eventId;

    /**
     * 이벤트 타입
     */
    private String eventType;

    /**
     * 이벤트 발생 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 이벤트 버전 (스키마 버전)
     */
    private String version;

    /**
     * 이벤트 발행자
     */
    private String publisher;

    /**
     * 초기화 (자식 클래스 생성자에서 호출)
     */
    protected void initBaseEvent(String eventType, String publisher) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.version = "1.0";
        this.publisher = publisher;
    }
}
