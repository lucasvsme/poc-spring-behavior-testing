package com.example.account.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public final class DepositRequest {

    @NotNull(message = "Amount to deposit is required")
    @Positive(message = "Amount to deposit must be a positive number")
    @Digits(integer = 7, fraction = 2, message = "Amount to deposit must be a number with 2 digits of precision")
    private BigDecimal amount;
}
