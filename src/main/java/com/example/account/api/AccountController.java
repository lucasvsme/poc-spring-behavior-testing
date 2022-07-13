package com.example.account.api;

import com.example.account.Account;
import com.example.account.AccountNotFoundException;
import com.example.account.AccountRepository;
import com.example.account.AccountTransferException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/accounts")
@ControllerAdvice
public class AccountController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Void> create(@Valid AccountRequest createAccount,
                                       UriComponentsBuilder uriComponentsBuilder) {
        final var account = new Account();
        account.setName(createAccount.getName());
        account.setBalance(BigDecimal.ZERO);

        final var accountCreated = accountRepository.save(account);
        LOGGER.info("Account created (account={})", accountCreated);

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(uriComponentsBuilder.path("/{accountId}")
                        .build(accountCreated.getId()))
                .build();
    }

    @GetMapping("/{accountId}")
    @Transactional(readOnly = true)
    public ResponseEntity<AccountResponse> findOne(@PathVariable Long accountId) {
        final var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        final var accountResponse = AccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .balance(account.getBalance())
                .build();
        LOGGER.info("Found account by ID (account={})", accountResponse);

        return ResponseEntity.status(HttpStatus.OK)
                .body(accountResponse);
    }

    @PostMapping("/{accountId}/transfer")
    @Transactional
    public ResponseEntity<TransferResponse> transfer(@PathVariable Long accountId,
                                                     @Valid TransferRequest transferRequest) {
        LOGGER.info("Trying to execute money transfer (request={})", transferRequest);
        final var sourceAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        final var targetAccount = accountRepository.findById(transferRequest.getTargetAccountId())
                .orElseThrow(() -> new AccountNotFoundException(transferRequest.getTargetAccountId()));

        if (sourceAccount.getBalance().compareTo(transferRequest.getAmount()) < 0) {
            throw new AccountTransferException(sourceAccount.getId(), targetAccount.getId());
        }

        sourceAccount.setBalance(sourceAccount.getBalance().min(transferRequest.getAmount()));
        targetAccount.setBalance(targetAccount.getBalance().add(transferRequest.getAmount()));
        accountRepository.saveAll(Set.of(sourceAccount, targetAccount));
        LOGGER.info("Money transferred between accounts (source={}, target={})", sourceAccount, targetAccount);

        final var transferResponse = TransferResponse.builder()
                .sourceAccountBalance(sourceAccount.getBalance())
                .targetAccountBalance(targetAccount.getBalance())
                .build();
        LOGGER.info("Money transaction finished successfully (response={})", transferResponse);

        return ResponseEntity.status(HttpStatus.OK)
                .body(transferResponse);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    private ResponseEntity<Void> handleAccountNotFoundException(AccountNotFoundException exception) {
        LOGGER.info("Account {} not found by ID", exception.getAccountId(), exception);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();
    }

    @ExceptionHandler(AccountTransferException.class)
    private ResponseEntity<Object> handleAccountTransferException(AccountTransferException exception) {
        LOGGER.info(
                "Account {} does not have enough balance to transfer {} to account {}",
                exception.getSourceAccountId(),
                exception.getTargetAccountId(),
                exception
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", exception.getMessage()));
    }
}