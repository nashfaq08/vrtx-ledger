package com.vrtx.ledger.dto;

import com.vrtx.ledger.domain.LedgerTransaction;
import com.vrtx.ledger.domain.TransactionStatus;
import com.vrtx.ledger.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        TransactionStatus status,
        String currency,
        BigDecimal amount,
        BigDecimal fee,
        UUID relatedTransactionId,
        String description,
        Instant createdAt,
        List<EntryResponse> entries
) {
    public static TransactionResponse from(LedgerTransaction t) {
        return new TransactionResponse(
                t.getId(), t.getType(), t.getStatus(), t.getCurrency(),
                t.getAmount(), t.getFee(), t.getRelatedTransactionId(),
                t.getDescription(), t.getCreatedAt(),
                t.getEntries().stream().map(EntryResponse::from).toList());
    }
}
