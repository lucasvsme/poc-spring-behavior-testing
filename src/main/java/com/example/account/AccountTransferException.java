package com.example.account;

import java.io.Serial;

public final class AccountTransferException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 2859624478615443940L;

    private final Long sourceAccountId;
    private final Long targetAccountId;

    public AccountTransferException(Long sourceAccountId, Long targetAccountId) {
        super("Insufficient balance in source account");
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public Long getTargetAccountId() {
        return targetAccountId;
    }
}
