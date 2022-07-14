package com.example.account.api;

import com.example.account.Account;
import com.example.account.AccountNotFoundException;
import com.example.account.AccountRepository;
import com.example.account.AccountTransferException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Set;

@RestController
@RequestMapping("/accounts")
@ControllerAdvice
public class AccountController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountController.class);

    private final AccountRepository accountRepository;
    private final DecimalFormat balanceFormat;

    public AccountController(AccountRepository accountRepository,
                             @Qualifier("balanceFormat") DecimalFormat decimalFormat) {
        this.accountRepository = accountRepository;
        this.balanceFormat = decimalFormat;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest accountRequest,
                                                  UriComponentsBuilder uriComponentsBuilder) {
        LOGGER.info("Trying to create account (request={})", accountRequest);
        final var account = new Account();
        account.setName(accountRequest.getName());
        account.setBalance(BigDecimal.ZERO);

        final var accountCreated = accountRepository.save(account);
        LOGGER.info("Account created (account={})", accountCreated);

        final var accountUri = uriComponentsBuilder.path("/{accountId}")
                .build(accountCreated.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(accountUri)
                .build();
    }

    @GetMapping("/{accountId}")
    @Transactional(readOnly = true)
    public ResponseEntity<AccountResponse> findOne(@PathVariable Long accountId) {
        final var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        final var accountResponse = new AccountResponse();
        accountResponse.setId(account.getId());
        accountResponse.setName(account.getName());
        accountResponse.setBalance(balanceFormat.format(account.getBalance()));
        LOGGER.info("Found account by ID (account={})", accountResponse);

        return ResponseEntity.status(HttpStatus.OK)
                .body(accountResponse);
    }

    @PostMapping("/{accountId}/deposit")
    @Transactional
    public ResponseEntity<DepositResponse> deposit(@PathVariable Long accountId,
                                                   @Valid @RequestBody DepositRequest depositRequest) {
        LOGGER.info("Trying to deposit money (accountId={}, request={})", accountId, depositRequest);
        final var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        account.setBalance(account.getBalance().add(depositRequest.getAmount()));
        accountRepository.save(account);
        LOGGER.info("Money deposited into account (account={})", account);

        final var depositResponse = new DepositResponse();
        depositResponse.setBalance(balanceFormat.format(account.getBalance()));
        LOGGER.info("Money deposit finished successfully (response={})", depositResponse);

        return ResponseEntity.status(HttpStatus.OK)
                .body(depositResponse);
    }

    @PostMapping("/{accountId}/transfer")
    @Transactional
    public ResponseEntity<TransferResponse> transfer(@PathVariable Long accountId,
                                                     @Valid @RequestBody TransferRequest transferRequest) {
        LOGGER.info("Trying to execute money transfer (accountId={}, request={})", accountId, transferRequest);
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

        final var transferResponse = new TransferResponse();
        transferResponse.setSourceAccountBalance(balanceFormat.format(sourceAccount.getBalance()));
        transferResponse.setTargetAccountBalance(balanceFormat.format(targetAccount.getBalance()));
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
    private ResponseEntity<TransferResponseError> handleAccountTransferException(AccountTransferException exception) {
        LOGGER.info(
                "Account {} does not have enough balance to transfer {} to account {}",
                exception.getSourceAccountId(),
                exception.getTargetAccountId(),
                exception
        );

        final var transferResponseError = new TransferResponseError();
        transferResponseError.setMessage(exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(transferResponseError);
    }
}
