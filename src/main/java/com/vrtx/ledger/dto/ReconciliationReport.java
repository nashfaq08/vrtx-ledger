package com.vrtx.ledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Result of a reconciliation run. In a healthy ledger {@code balanced} is true,
 * the debit/credit totals are equal and {@code unbalancedTransactionIds} is empty.
 */
public record ReconciliationReport(
        Instant generatedAt,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        BigDecimal difference,
        boolean balanced,
        List<UUID> unbalancedTransactionIds
) {}
