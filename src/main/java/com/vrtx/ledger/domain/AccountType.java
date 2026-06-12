package com.vrtx.ledger.domain;

/**
 * The kinds of accounts the ledger supports.
 * USER and MERCHANT balances are real obligations and may not go negative.
 * FEES and SETTLEMENT are internal/system accounts and may run negative.
 */
public enum AccountType {
    USER,
    MERCHANT,
    FEES,
    SETTLEMENT;

    /** Whether this account is allowed to hold a negative balance. */
    public boolean allowsOverdraft() {
        return this == FEES || this == SETTLEMENT;
    }
}
