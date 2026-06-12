package com.vrtx.ledger.service;

import com.vrtx.ledger.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class BalanceService {

    private final LedgerEntryRepository entryRepository;

    public BalanceService(LedgerEntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal balanceOf(UUID accountId) {
        return entryRepository.computeBalance(accountId);
    }
}
