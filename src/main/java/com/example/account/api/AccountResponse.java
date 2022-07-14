package com.example.account.api;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AccountResponse {

    @NotNull
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private String balance;
}
