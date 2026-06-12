package com.vrtx.ledger.dto;

import com.vrtx.ledger.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Helper endpoint to create accounts (useful for testing / multi-currency). */
public record CreateAccountRequest(
        @NotBlank String name,
        @NotNull AccountType type,
        @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "[A-Z]{3}") String currency
) {}
