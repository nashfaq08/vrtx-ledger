package com.vrtx.ledger.exception;

import java.math.BigDecimal;

public class UnbalancedTransactionException extends LedgerException {
    public UnbalancedTransactionException(BigDecimal debits, BigDecimal credits) {
        super("Unbalanced transaction: debits=" + debits + " credits=" + credits);
    }
}
