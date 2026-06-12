package com.vrtx.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Refund / reversal of a prior payment. If {@code amount} is null the full
 * remaining (un-refunded) principal is reversed; otherwise a partial refund.
 */
public record RefundRequest(
        @NotNull UUID originalTransactionId,
        @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotBlank String idempotencyKey,
        String description
) {}
