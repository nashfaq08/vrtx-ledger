package com.vrtx.ledger.service;

import com.vrtx.ledger.domain.LedgerTransaction;
import com.vrtx.ledger.dto.PaymentRequest;
import com.vrtx.ledger.dto.RefundRequest;
import com.vrtx.ledger.dto.SettlementRequest;
import com.vrtx.ledger.dto.TransactionResponse;
import com.vrtx.ledger.dto.TransferRequest;
import com.vrtx.ledger.repository.LedgerTransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class LedgerService {

    private final TransactionPoster poster;
    private final LedgerTransactionRepository transactionRepository;

    public LedgerService(TransactionPoster poster, LedgerTransactionRepository transactionRepository) {
        this.poster = poster;
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse pay(PaymentRequest req) {
        return idempotent(req.idempotencyKey(), () -> poster.createPayment(req));
    }

    public TransactionResponse transfer(TransferRequest req) {
        return idempotent(req.idempotencyKey(), () -> poster.createTransfer(req));
    }

    public TransactionResponse refund(RefundRequest req) {
        return idempotent(req.idempotencyKey(), () -> poster.createRefund(req));
    }

    public TransactionResponse settle(SettlementRequest req) {
        return idempotent(req.idempotencyKey(), () -> poster.createSettlement(req));
    }

    private TransactionResponse idempotent(String key, Supplier<LedgerTransaction> work) {
        LedgerTransaction existing = transactionRepository.findByIdempotencyKeyWithEntries(key).orElse(null);
        if (existing != null) {
            return TransactionResponse.from(existing);
        }
        try {
            return TransactionResponse.from(work.get());
        } catch (DataIntegrityViolationException race) {
            // Lost a concurrent race on the unique idempotency_key: return the winner.
            LedgerTransaction winner = transactionRepository.findByIdempotencyKeyWithEntries(key).orElse(null);
            if (winner != null) {
                return TransactionResponse.from(winner);
            }
            throw race;
        }
    }
}
