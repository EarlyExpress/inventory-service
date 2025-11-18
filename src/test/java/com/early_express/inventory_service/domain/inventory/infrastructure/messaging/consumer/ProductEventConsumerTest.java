package com.early_express.inventory_service.domain.inventory.infrastructure.messaging.consumer;

import com.early_express.inventory_service.domain.inventory.application.service.InventoryService;
import com.early_express.inventory_service.domain.inventory.infrastructure.messaging.event.ProductCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * ProductEventConsumer 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductEventConsumer 테스트")
class ProductEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private ProductEventConsumer productEventConsumer;

    @Test
    @DisplayName("ProductCreatedEvent 수신 시 초기 재고 생성 성공")
    void handleProductCreatedEvent_Success() {
        // given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .eventId("event-001")
                .eventType("PRODUCT_CREATED")
                .productId("PROD-001")
                .sellerId("SELLER-001")
                .name("테스트 상품")
                .build();

        willDoNothing().given(inventoryService).createInitialInventories(event.getProductId());
        willDoNothing().given(acknowledgment).acknowledge();

        // when
        productEventConsumer.handleProductCreatedEvent(event, 0, 100L, acknowledgment);

        // then
        verify(inventoryService).createInitialInventories(event.getProductId());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("ProductCreatedEvent 처리 중 예외 발생 시 RuntimeException으로 래핑")
    void handleProductCreatedEvent_ThrowsException() {
        // given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .eventId("event-002")
                .eventType("PRODUCT_CREATED")
                .productId("PROD-002")
                .sellerId("SELLER-001")
                .name("테스트 상품")
                .build();

        willThrow(new RuntimeException("재고 생성 실패"))
                .given(inventoryService).createInitialInventories(event.getProductId());

        // when & then
        assertThatThrownBy(() ->
                productEventConsumer.handleProductCreatedEvent(event, 0, 100L, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이벤트 처리 실패");

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("ProductCreatedEvent 처리 성공 시 커밋 수행")
    void handleProductCreatedEvent_CommitsOnSuccess() {
        // given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .eventId("event-003")
                .eventType("PRODUCT_CREATED")
                .productId("PROD-003")
                .sellerId("SELLER-001")
                .name("테스트 상품")
                .build();

        willDoNothing().given(inventoryService).createInitialInventories(event.getProductId());

        // when
        productEventConsumer.handleProductCreatedEvent(event, 0, 100L, acknowledgment);

        // then
        verify(acknowledgment).acknowledge();
    }
}