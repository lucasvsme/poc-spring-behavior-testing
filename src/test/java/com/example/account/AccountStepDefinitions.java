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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    private final Map<String, AccountResponse> accounts = new HashMap<>();
    private TransferResponseError transferResponseError;

    @Before
    public void before() {
        deleteAllAccounts();
    }

    @After
    public void after() {
        deleteAllAccounts();
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
        accounts.put(accountName, accountResponse);

        assertEquals(expectedInitialBalance, accountResponse.getBalance());
    }

    @Given("The {string} account balance is {string}")
    public void theAccountBalanceIs(String accountName, String balance) throws ParseException {
        final var accountId = accounts.get(accountName).getId();
        final var depositRequest = new DepositRequest();
        depositRequest.setAmount(((BigDecimal) balanceFormat.parse(balance)));

        final var depositResponse = webTestClient.post()
                .uri("/accounts/{accountId}/deposit", accountId)
                .bodyValue(depositRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(DepositResponse.class)
                .returnResult().getResponseBody();

        assertNotNull(depositResponse);
        assertEquals(depositResponse.getBalance(), balance);
    }

    @When("I transfer {string} from {string} account to {string} account")
    public void iTransferFromAccountToAccount(String amount, String sourceAccountName, String targetAccountName) {
        final var transferResponse = transfer(amount, sourceAccountName, targetAccountName)
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(TransferResponse.class)
                .returnResult().getResponseBody();

        assertNotNull(transferResponse);
    }

    @When("I try to transfer {string} from {string} account to {string} account")
    public void iTryToTransferFromAccountToAccount(String amount, String sourceAccountName, String targetAccountName) {
        final var transferResponseError = transfer(amount, sourceAccountName, targetAccountName)
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(TransferResponseError.class)
                .returnResult().getResponseBody();

        assertNotNull(transferResponseError);
        this.transferResponseError = transferResponseError;
    }

    @Then("The {string} account balance should be {string}")
    public void theAccountBalanceShouldBe(String accountName, String expectedBalance) {
        final var accountId = accounts.get(accountName).getId();
        final var account = findAccountById(accountId);
        assertEquals(expectedBalance, account.getBalance());
    }

    @Then("The transfer is cancelled with error {string}")
    public void theTransferIsCancelledWithError(String expectedErrorMessage) {
        assertEquals(expectedErrorMessage, transferResponseError.getMessage());
    }

    private WebTestClient.ResponseSpec transfer(String amount, String sourceAccountName, String targetAccountName) {
        final var source = accounts.get(sourceAccountName);
        final var target = accounts.get(targetAccountName);

        final var transferRequest = new TransferRequest();
        transferRequest.setTargetAccountId(target.getId());
        transferRequest.setAmount(new BigDecimal(amount));

        return webTestClient.post()
                .uri("/accounts/{accountId}/transfer", source.getId())
                .bodyValue(transferRequest)
                .exchange();
    }

    private AccountResponse findAccountById(Long accountId) {
        return webTestClient.get()
                .uri("/accounts/{accountId}", accountId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(AccountResponse.class)
                .returnResult().getResponseBody();
    }

    private Long getAccountIdFromLocationHeader(ExchangeResult exchangeResult) {
        final var responseHeaders = exchangeResult.getResponseHeaders();
        final var location = responseHeaders.getLocation();
        assert location != null;

        final var segments = location.toString().split("/");
        final var lastSegment = segments[segments.length - 1];

        return Long.parseLong(lastSegment);
    }

    private void deleteAllAccounts() {
        final var accountIds = accounts.values().stream()
                .map(AccountResponse::getId)
                .toList();

        accountRepository.deleteAllById(accountIds);
    }
}
