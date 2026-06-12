package com.vrtx.ledger.dto;

import com.vrtx.ledger.domain.EntryDirection;
import com.vrtx.ledger.domain.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EntryResponse(
        UUID id,
        UUID accountId,
        EntryDirection direction,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {
    public static EntryResponse from(LedgerEntry e) {
        return new EntryResponse(e.getId(), e.getAccount().getId(), e.getDirection(),
                e.getAmount(), e.getCurrency(), e.getCreatedAt());
    }
}
