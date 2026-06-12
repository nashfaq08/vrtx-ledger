package com.vrtx.ledger.dto;

import com.vrtx.ledger.domain.Account;
import com.vrtx.ledger.domain.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        String currency,
        BigDecimal balance
) {
    public static AccountResponse of(Account a, BigDecimal balance) {
        return new AccountResponse(a.getId(), a.getName(), a.getType(), a.getCurrency(), balance);
    }
}
