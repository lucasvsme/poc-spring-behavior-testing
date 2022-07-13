package com.example.account.api;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public final class AccountResponse {

    @NotNull
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private BigDecimal balance;
}
