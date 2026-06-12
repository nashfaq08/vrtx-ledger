package com.vrtx.ledger.dto;

import com.vrtx.ledger.domain.AccountType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Full ledger statement for an account: identity, current balance, all lines. */
public record AccountLedgerResponse(
        UUID accountId,
        String name,
        AccountType type,
        String currency,
        BigDecimal balance,
        List<LedgerLineResponse> lines
) {}
