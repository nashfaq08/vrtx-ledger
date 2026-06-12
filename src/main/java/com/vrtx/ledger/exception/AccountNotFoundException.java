package com.vrtx.ledger.exception;

import java.util.UUID;

public class AccountNotFoundException extends LedgerException {
    public AccountNotFoundException(UUID id) {
        super("Account not found: " + id);
    }
}
