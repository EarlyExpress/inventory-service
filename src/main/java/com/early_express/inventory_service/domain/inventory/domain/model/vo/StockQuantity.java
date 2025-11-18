package com.early_express.inventory_service.domain.inventory.domain.model.vo;

import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryErrorCode;
import com.early_express.inventory_service.domain.inventory.domain.exception.InventoryException;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 수량 Value Object
 * - 불변 객체
 * - 음수 방지 검증 포함
 */
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockQuantity {

    private Integer value;

    private StockQuantity(Integer value) {
        validate(value);
        this.value = value;
    }

    public static StockQuantity of(Integer value) {
        return new StockQuantity(value);
    }

    public static StockQuantity zero() {
        return new StockQuantity(0);
    }

    private void validate(Integer value) {
        if (value == null) {
            throw new InventoryException(
                    InventoryErrorCode.INVALID_QUANTITY,
                    "수량은 null일 수 없습니다."
            );
        }
        if (value < 0) {
            throw new InventoryException(
                    InventoryErrorCode.INVALID_QUANTITY,
                    "수량은 0 이상이어야 합니다. 입력값: " + value
            );
        }
    }

    /**
     * 증가
     */
    public StockQuantity increase(Integer amount) {
        if (amount < 0) {
            throw new InventoryException(
                    InventoryErrorCode.INVALID_QUANTITY,
                    "증가량은 0 이상이어야 합니다."
            );
        }
        return StockQuantity.of(this.value + amount);
    }

    /**
     * 감소
     */
    public StockQuantity decrease(Integer amount) {
        if (amount < 0) {
            throw new InventoryException(
                    InventoryErrorCode.INVALID_QUANTITY,
                    "감소량은 0 이상이어야 합니다."
            );
        }

        int result = this.value - amount;
        if (result < 0) {
            throw new InventoryException(
                    InventoryErrorCode.NEGATIVE_STOCK_NOT_ALLOWED,
                    String.format("재고 부족: 현재 %d, 요청 %d", this.value, amount)
            );
        }

        return StockQuantity.of(result);
    }

    /**
     * 비교
     */
    public boolean isGreaterThan(StockQuantity other) {
        return this.value > other.value;
    }

    public boolean isGreaterThanOrEqual(StockQuantity other) {
        return this.value >= other.value;
    }

    public boolean isLessThan(StockQuantity other) {
        return this.value < other.value;
    }

    public boolean isLessThanOrEqual(StockQuantity other) {
        return this.value <= other.value;
    }

    public boolean isZero() {
        return this.value == 0;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
