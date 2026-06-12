package com.vrtx.ledger.service.fee;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FeeCalculator {

    private static final int MONEY_SCALE = 4;

    private final FeeProperties properties;

    public FeeCalculator(FeeProperties properties) {
        this.properties = properties;
    }

    /** fee = round(amount * percentage) + fixed, never exceeding the amount itself. */
    public BigDecimal calculate(BigDecimal amount) {
        BigDecimal fee = amount.multiply(properties.getPercentage())
                .add(properties.getFixed())
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return fee.min(amount);
    }
}
