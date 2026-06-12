package com.vrtx.ledger.service.fee;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.UUID;

@ConfigurationProperties(prefix = "ledger.fees")
public class FeeProperties {

    private BigDecimal percentage = new BigDecimal("0.03");

    private BigDecimal fixed = BigDecimal.ZERO;

    private UUID accountId;

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public BigDecimal getFixed() {
        return fixed;
    }

    public void setFixed(BigDecimal fixed) {
        this.fixed = fixed;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }
}
