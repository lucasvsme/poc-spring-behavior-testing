package com.example.account.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

@Configuration
public class AccountConfiguration {

    @Bean("balanceFormat")
    DecimalFormat decimalFormat() {
        final var decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance());
        decimalFormat.setParseBigDecimal(true);

        return decimalFormat;
    }
}
