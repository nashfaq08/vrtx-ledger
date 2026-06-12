package com.vrtx.ledger.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends LedgerException {
    public InsufficientFundsException(UUID accountId, BigDecimal balance, BigDecimal attempted) {
        super("Insufficient funds in account " + accountId
                + ": balance=" + balance + " attempted debit=" + attempted);
    }
}
