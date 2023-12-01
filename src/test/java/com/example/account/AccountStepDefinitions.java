package com.example.account;

import com.example.account.api.AccountRequest;
import com.example.account.api.AccountResponse;
import com.example.account.api.DepositRequest;
import com.example.account.api.DepositResponse;
import com.example.account.api.TransferRequest;
import com.example.account.api.TransferResponse;
import com.example.account.api.TransferResponseError;
import com.example.testing.DatabaseTestConfiguration;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.ExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DatabaseTestConfiguration.class)
@AutoConfigureWebTestClient
@CucumberContextConfiguration
public class AccountStepDefinitions {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    @Qualifier("balanceFormat")
    private DecimalFormat balanceFormat;

    private AccountResponse sourceAccount, targetAccount;
    private TransferResponse transferResponse;
    private TransferResponseError transferResponseError;

    @Before
    public void before() {
        deleteAllAccounts();
    }

    @After
    public void after() {
        deleteAllAccounts();
    }

    private void deleteAllAccounts() {
        if (sourceAccount != null && targetAccount != null) {
            accountRepository.deleteAllById(List.of(
                    sourceAccount.getId(),
                    targetAccount.getId()
            ));
        }
    }

    @Given("Account {string} exists and has balance {string}")
    public void accountExistsAndHasBalance(String accountName, String expectedInitialBalance) {
        final var accountRequest = new AccountRequest();
        accountRequest.setName(accountName);

        final var exchangeResult = webTestClient.post()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(accountRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectHeader().exists(HttpHeaders.LOCATION)
                .returnResult(Void.class);

        final var accountId = getAccountIdFromLocationHeader(exchangeResult);
        final var accountResponse = findAccountById(accountId);

        setAccountField(accountResponse);
        assertEquals(expectedInitialBalance, accountResponse.getBalance());
    }

    private void setAccountField(AccountResponse accountResponse) {
        if (accountResponse.getName().equals("Main")) {
            sourceAccount = accountResponse;
            return;
        }

        if (accountResponse.getName().equals("Secondary")) {
            targetAccount = accountResponse;
            return;
        }

        Assertions.fail("Created unknown account: " + accountResponse);
    }

    @Given("The {string} account balance is {string}")
    public void theAccountBalanceIs(String account, String balance) throws ParseException {
        final var accountId = getAccountIdByName(account);
        final var depositRequest = new DepositRequest();
        depositRequest.setAmount(((BigDecimal) balanceFormat.parse(balance)));

        final var depositResponse = webTestClient.post()
                .uri("/accounts/{accountId}/deposit", accountId)
                .bodyValue(depositRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(DepositResponse.class)
                .returnResult().getResponseBody();

        setAccountBalanceField(accountId, depositResponse);
        assertEquals(depositResponse.getBalance(), balance);
    }

    private Long getAccountIdByName(String accountName) {
        if (accountName.equals("Main")) {
            return sourceAccount.getId();
        }

        if (accountName.equals("Secondary")) {
            return targetAccount.getId();
        }

        throw new AssertionError("Tried to find unknown account with name " + accountName);
    }

    private void setAccountBalanceField(Long accountId, DepositResponse depositResponse) {
        if (sourceAccount.getId().equals(accountId)) {
            return;
        }

        if (targetAccount.getId().equals(accountId)) {
            return;
        }

        Assertions.fail("Could not assign deposit response: " + accountId);
    }

    @When("I transfer {string} from {string} account to {string} account")
    public void iTransferFromAccountToAccount(String amount, String sourceAccountName, String targetAccountName) {
        this.transferResponse = transfer(amount, sourceAccountName, targetAccountName)
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(TransferResponse.class)
                .returnResult().getResponseBody();
    }

    @When("I try to transfer {string} from {string} account to {string} account")
    public void iTryToTransferFromAccountToAccount(String amount, String sourceAccountName, String targetAccountName) {
        this.transferResponseError = transfer(amount, sourceAccountName, targetAccountName)
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .returnResult(TransferResponseError.class)
                .getResponseBody()
                .blockFirst();
    }

    private WebTestClient.ResponseSpec transfer(String amount, String sourceAccountName, String targetAccountName) {
        final var source = getAccountByName(sourceAccountName);
        final var target = getAccountByName(targetAccountName);

        final var transferRequest = new TransferRequest();
        transferRequest.setTargetAccountId(target.getId());
        transferRequest.setAmount(new BigDecimal(amount));

        return webTestClient.post()
                .uri("/accounts/{accountId}/transfer", source.getId())
                .bodyValue(transferRequest)
                .exchange();
    }

    @Then("The {string} account balance should be {string}")
    public void theAccountBalanceShouldBe(String accountName, String expectedBalance) {
        final var account = getAccountByName(accountName);

        if (account.getName().equals("Main")) {
            assertEquals(expectedBalance, transferResponse.getSourceAccountBalance());
            return;
        }

        if (account.getName().equals("Secondary")) {
            assertEquals(expectedBalance, transferResponse.getTargetAccountBalance());
            return;
        }

        Assertions.fail("Tried to verify balance of unknown account with name " + accountName);
    }

    @Then("The transfer is cancelled with error {string}")
    public void theTransferIsCancelledWithError(String expectedErrorMessage) {
        assertEquals(expectedErrorMessage, transferResponseError.getMessage());
    }

    private Long getAccountIdFromLocationHeader(ExchangeResult exchangeResult) {
        final var responseHeaders = exchangeResult.getResponseHeaders();
        final var location = responseHeaders.getLocation();
        assert location != null;

        final var segments = location.toString().split("/");
        final var lastSegment = segments[segments.length - 1];

        return Long.parseLong(lastSegment);
    }

    private AccountResponse findAccountById(Long accountId) {
        return webTestClient.get()
                .uri("/accounts/{accountId}", accountId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .returnResult(AccountResponse.class)
                .getResponseBody()
                .blockFirst();
    }

    private AccountResponse getAccountByName(String accountName) {
        if (sourceAccount.getName().equals(accountName)) {
            return sourceAccount;
        }

        if (targetAccount.getName().equals(accountName)) {
            return targetAccount;
        }

        throw new AssertionError("Tried to find unknown account with name " + accountName);
    }
}
