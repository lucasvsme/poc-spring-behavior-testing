package com.example.account.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public final class TransferRequest {

    @NotNull(message = "Target account ID is required")
    @Positive(message = "Target Account ID is a positive integer number")
    private Long targetAccountId;

    @NotNull(message = "Amount to transfer is required")
    @Positive(message = "Amount to transfer must be a positive number")
    @Digits(integer = 7, fraction = 2, message = "Amount to transfer must be a number with 2 digits of precision")
    private BigDecimal amount;
}
