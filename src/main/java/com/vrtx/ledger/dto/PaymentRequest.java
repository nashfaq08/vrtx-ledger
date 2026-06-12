package com.vrtx.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Card payment: user pays a merchant; the platform takes a fee.
 * If {@code fee} is omitted it is computed from the configurable fee strategy.
 */
public record PaymentRequest(
        @NotNull UUID payerAccountId,
        @NotNull UUID payeeAccountId,
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @Digits(integer = 15, fraction = 4) BigDecimal fee,
        @NotBlank String idempotencyKey,
        String description
) {}
