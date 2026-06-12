package com.vrtx.ledger.service;

import com.vrtx.ledger.domain.Account;
import com.vrtx.ledger.domain.EntryDirection;

import java.math.BigDecimal;

/**
 * Internal value object describing one posting line while a transaction is
 * being assembled, before it is turned into a persistent LedgerEntry.
 */
public record EntrySpec(Account account, EntryDirection direction, BigDecimal amount) {

    public static EntrySpec debit(Account account, BigDecimal amount) {
        return new EntrySpec(account, EntryDirection.DEBIT, amount);
    }

    public static EntrySpec credit(Account account, BigDecimal amount) {
        return new EntrySpec(account, EntryDirection.CREDIT, amount);
    }

    public BigDecimal signedAmount() {
        return direction == EntryDirection.CREDIT ? amount : amount.negate();
    }
}
