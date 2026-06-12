package com.vrtx.ledger.dto;

import com.vrtx.ledger.domain.EntryDirection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One line of an account statement, including a running balance. */
public record LedgerLineResponse(
        UUID entryId,
        UUID transactionId,
        EntryDirection direction,
        BigDecimal amount,
        BigDecimal runningBalance,
        String currency,
        Instant createdAt
) {}
