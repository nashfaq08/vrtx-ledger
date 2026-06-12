package com.vrtx.ledger.service;

import com.vrtx.ledger.dto.ReconciliationReport;
import com.vrtx.ledger.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final LedgerEntryRepository entryRepository;

    public ReconciliationService(LedgerEntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationReport reconcile() {
        BigDecimal debits = entryRepository.totalDebits();
        BigDecimal credits = entryRepository.totalCredits();
        BigDecimal difference = debits.subtract(credits);
        List<UUID> unbalanced = entryRepository.findUnbalancedTransactionIds();

        boolean balanced = difference.signum() == 0 && unbalanced.isEmpty();
        return new ReconciliationReport(Instant.now(), debits, credits, difference, balanced, unbalanced);
    }
}
