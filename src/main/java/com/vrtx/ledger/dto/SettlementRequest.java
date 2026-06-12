package com.vrtx.ledger.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Merchant settlement: sweep funds from a merchant account into a settlement
 * (bank/payout) account. If {@code amount} is null the merchant's full balance
 * is settled.
 */
public record SettlementRequest(
        @NotNull UUID merchantAccountId,
        @NotNull UUID settlementAccountId,
        @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotBlank String idempotencyKey,
        String description
) {}
