package com.example.account.api;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class TransferResponse {

    @NotNull
    private String sourceAccountBalance;

    @NotNull
    private String targetAccountBalance;
}
