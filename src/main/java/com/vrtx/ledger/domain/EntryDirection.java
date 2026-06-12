package com.vrtx.ledger.domain;

/**
 * DEBIT  = money leaving the account.
 * CREDIT = money entering the account.
 * Balance is computed as SUM(credits) - SUM(debits).
 */
public enum EntryDirection {
    DEBIT,
    CREDIT
}
