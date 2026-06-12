package com.vrtx.ledger.exception;

import java.util.UUID;

public class TransactionNotFoundException extends LedgerException {
    public TransactionNotFoundException(UUID id) {
        super("Transaction not found: " + id);
    }
}
