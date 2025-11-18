package com.early_express.inventory_service.domain.inventory.domain.model.vo;

import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryErrorCode;
import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StockQuantity Value Object 테스트")
class StockQuantityTest {

    @Test
    @DisplayName("유효한 수량으로 StockQuantity 생성")
    void createStockQuantity_withValidValue() {
        // given
        Integer value = 100;

        // when
        StockQuantity quantity = StockQuantity.of(value);

        // then
        assertThat(quantity.getValue()).isEqualTo(value);
    }

    @Test
    @DisplayName("0으로 StockQuantity 생성")
    void createStockQuantity_withZero() {
        // given & when
        StockQuantity quantity = StockQuantity.zero();

        // then
        assertThat(quantity.getValue()).isZero();
        assertThat(quantity.isZero()).isTrue();
    }

    @Test
    @DisplayName("음수로 생성 시 예외 발생")
    void createStockQuantity_withNegative_throwsException() {
        // given
        Integer negative = -10;

        // when & then
        assertThatThrownBy(() -> StockQuantity.of(negative))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("0 이상이어야 합니다")
                .extracting(e -> ((InventoryException) e).getErrorCode())
                .isEqualTo(InventoryErrorCode.INVALID_QUANTITY);
    }

    @Test
    @DisplayName("null로 생성 시 예외 발생")
    void createStockQuantity_withNull_throwsException() {
        // when & then
        assertThatThrownBy(() -> StockQuantity.of(null))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("null일 수 없습니다");
    }

    @Test
    @DisplayName("수량 증가")
    void increase() {
        // given
        StockQuantity quantity = StockQuantity.of(100);
        Integer amount = 50;

        // when
        StockQuantity increased = quantity.increase(amount);

        // then
        assertThat(increased.getValue()).isEqualTo(150);
        assertThat(quantity.getValue()).isEqualTo(100); // 원본 불변
    }

    @Test
    @DisplayName("음수 증가 시 예외 발생")
    void increase_withNegative_throwsException() {
        // given
        StockQuantity quantity = StockQuantity.of(100);

        // when & then
        assertThatThrownBy(() -> quantity.increase(-10))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("증가량은 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("수량 감소")
    void decrease() {
        // given
        StockQuantity quantity = StockQuantity.of(100);
        Integer amount = 50;

        // when
        StockQuantity decreased = quantity.decrease(amount);

        // then
        assertThat(decreased.getValue()).isEqualTo(50);
        assertThat(quantity.getValue()).isEqualTo(100); // 원본 불변
    }

    @Test
    @DisplayName("재고 부족 시 감소 예외 발생")
    void decrease_insufficientStock_throwsException() {
        // given
        StockQuantity quantity = StockQuantity.of(100);
        Integer amount = 150;

        // when & then
        assertThatThrownBy(() -> quantity.decrease(amount))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("재고 부족")
                .extracting(e -> ((InventoryException) e).getErrorCode())
                .isEqualTo(InventoryErrorCode.NEGATIVE_STOCK_NOT_ALLOWED);
    }

    @Test
    @DisplayName("음수 감소 시 예외 발생")
    void decrease_withNegative_throwsException() {
        // given
        StockQuantity quantity = StockQuantity.of(100);

        // when & then
        assertThatThrownBy(() -> quantity.decrease(-10))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("감소량은 0 이상이어야 합니다");
    }

    @Test
    @DisplayName("수량 비교 - isGreaterThan")
    void isGreaterThan() {
        // given
        StockQuantity quantity1 = StockQuantity.of(100);
        StockQuantity quantity2 = StockQuantity.of(50);

        // when & then
        assertThat(quantity1.isGreaterThan(quantity2)).isTrue();
        assertThat(quantity2.isGreaterThan(quantity1)).isFalse();
    }

    @Test
    @DisplayName("수량 비교 - isLessThan")
    void isLessThan() {
        // given
        StockQuantity quantity1 = StockQuantity.of(50);
        StockQuantity quantity2 = StockQuantity.of(100);

        // when & then
        assertThat(quantity1.isLessThan(quantity2)).isTrue();
        assertThat(quantity2.isLessThan(quantity1)).isFalse();
    }

    @Test
    @DisplayName("수량 비교 - isGreaterThanOrEqual")
    void isGreaterThanOrEqual() {
        // given
        StockQuantity quantity1 = StockQuantity.of(100);
        StockQuantity quantity2 = StockQuantity.of(100);
        StockQuantity quantity3 = StockQuantity.of(50);

        // when & then
        assertThat(quantity1.isGreaterThanOrEqual(quantity2)).isTrue();
        assertThat(quantity1.isGreaterThanOrEqual(quantity3)).isTrue();
        assertThat(quantity3.isGreaterThanOrEqual(quantity1)).isFalse();
    }

    @Test
    @DisplayName("동일한 수량은 equals와 hashCode가 같음")
    void equality() {
        // given
        StockQuantity quantity1 = StockQuantity.of(100);
        StockQuantity quantity2 = StockQuantity.of(100);

        // when & then
        assertThat(quantity1).isEqualTo(quantity2);
        assertThat(quantity1.hashCode()).isEqualTo(quantity2.hashCode());
    }
}