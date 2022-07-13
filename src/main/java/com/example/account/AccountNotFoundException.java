package com.example.account;

import java.io.Serial;

public final class AccountNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -257599570322416690L;
    private final Long accountId;

    public AccountNotFoundException(Long accountId) {
        super("Account not found", null);
        this.accountId = accountId;
    }

    public Long getAccountId() {
        return accountId;
    }
}
