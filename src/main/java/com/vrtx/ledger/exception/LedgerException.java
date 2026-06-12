package com.vrtx.ledger.exception;

/** Base type for all domain-level ledger errors. */
public abstract class LedgerException extends RuntimeException {
    protected LedgerException(String message) {
        super(message);
    }
}
