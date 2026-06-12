package com.vrtx.ledger.domain;

public enum TransactionType {
    PAYMENT,
    REFUND,
    TRANSFER,
    SETTLEMENT,
    /** Internal balance adjustment (e.g. opening balances). */
    ADJUSTMENT
}
