package com.example.account.api;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public final class TransferResponse {

    @NotNull
    private BigDecimal sourceAccountBalance;

    @NotNull
    private BigDecimal targetAccountBalance;
}
