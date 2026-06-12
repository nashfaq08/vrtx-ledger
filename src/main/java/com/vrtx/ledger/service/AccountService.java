package com.vrtx.ledger.service;

import com.vrtx.ledger.domain.Account;
import com.vrtx.ledger.domain.LedgerEntry;
import com.vrtx.ledger.dto.AccountLedgerResponse;
import com.vrtx.ledger.dto.AccountResponse;
import com.vrtx.ledger.dto.CreateAccountRequest;
import com.vrtx.ledger.dto.LedgerLineResponse;
import com.vrtx.ledger.exception.AccountNotFoundException;
import com.vrtx.ledger.repository.AccountRepository;
import com.vrtx.ledger.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;

    public AccountService(AccountRepository accountRepository, LedgerEntryRepository entryRepository) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest req) {
        Account account = accountRepository.save(new Account(req.name(), req.type(), req.currency()));
        return AccountResponse.of(account, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return AccountResponse.of(account, entryRepository.computeBalance(id));
    }

    @Transactional(readOnly = true)
    public AccountLedgerResponse ledger(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        List<LedgerEntry> entries = entryRepository.findByAccountOrdered(id);
        List<LedgerLineResponse> lines = new ArrayList<>(entries.size());
        BigDecimal running = BigDecimal.ZERO;
        for (LedgerEntry e : entries) {
            running = running.add(e.signedAmount());
            lines.add(new LedgerLineResponse(
                    e.getId(),
                    e.getTransaction().getId(),
                    e.getDirection(),
                    e.getAmount(),
                    running,
                    e.getCurrency(),
                    e.getCreatedAt()));
        }
        return new AccountLedgerResponse(account.getId(), account.getName(), account.getType(),
                account.getCurrency(), running, lines);
    }
}
