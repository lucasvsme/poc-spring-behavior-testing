package com.example.account;

import com.example.account.api.AccountRequest;
import com.example.account.api.AccountResponse;
import com.example.account.api.DepositRequest;
import com.example.account.api.TransferRequest;
import com.example.testing.DatabaseTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.ExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DatabaseTestConfiguration.class)
@AutoConfigureWebTestClient
class AccountControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void findingUnknownAccount() {
        // Defining unknown ID to represent source account
        final var accountId = 101L;
        assertAccountDoesNotExist(accountId);

        // Trying to find it and expecting an error
        webTestClient.get()
                .uri("/accounts/{accountId}", accountId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody().isEmpty();
    }

    @Test
    void depositingToUnknownAccount() {
        // Defining unknown ID to represent source account
        final var accountId = 203L;
        assertAccountDoesNotExist(accountId);

        final var depositRequest = new DepositRequest();
        depositRequest.setAmount(BigDecimal.ONE);

        webTestClient.post()
                .uri("/accounts/{accountId}/deposit", accountId)
                .bodyValue(depositRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody().isEmpty();
    }

    @Test
    void transferringFromUnknownSourceAccount() {
        // Defining unknown ID to represent source account
        final var sourceAccountId = 924L;
        assertAccountDoesNotExist(sourceAccountId);

        // Creating target account
        final var targetAccountRequest = new AccountRequest();
        targetAccountRequest.setName("A");
        final var targetAccount = createAccount(targetAccountRequest);

        // Defining the transfer
        final var transferRequest = new TransferRequest();
        transferRequest.setAmount(BigDecimal.ONE);
        transferRequest.setTargetAccountId(targetAccount.getId());

        // Transferring and inspecting the response is an error
        webTestClient.post()
                .uri("/accounts/{accountId}/transfer", sourceAccountId)
                .bodyValue(transferRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody().isEmpty();
    }

    @Test
    void transferringFromUnknownTargetAccount() {
        // Creating source account
        final var sourceAccountRequest = new AccountRequest();
        sourceAccountRequest.setName("B");
        final var sourceAccount = createAccount(sourceAccountRequest);

        // Defining unknown ID to represent target account
        final var targetAccountId = 123L;
        assertAccountDoesNotExist(targetAccountId);

        // Defining the transfer
        final var transferRequest = new TransferRequest();
        transferRequest.setAmount(BigDecimal.ONE);
        transferRequest.setTargetAccountId(targetAccountId);

        // Transferring and inspecting the response is an error
        webTestClient.post()
                .uri("/accounts/{accountId}/transfer", sourceAccount.getId())
                .bodyValue(transferRequest)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody().isEmpty();
    }

    private AccountResponse createAccount(AccountRequest accountRequest) {
        final var exchange = webTestClient.post()
                .uri("/accounts")
                .bodyValue(accountRequest)
                .exchange()
                .returnResult(Void.class);

        final var accountId = getAccountIdFromLocationHeader(exchange);

        return webTestClient.get()
                .uri("/accounts/{accountId}", accountId)
                .exchange()
                .returnResult(AccountResponse.class)
                .getResponseBody()
                .blockFirst();
    }

    private void assertAccountDoesNotExist(Long accountId) {
        webTestClient.get()
                .uri("/accounts/{accountId}", accountId)
                .exchange()
                .expectStatus().isNotFound();
    }

    private Long getAccountIdFromLocationHeader(ExchangeResult exchangeResult) {
        final var responseHeaders = exchangeResult.getResponseHeaders();
        final var location = responseHeaders.getLocation();
        assert location != null;

        final var segments = location.toString().split("/");
        final var lastSegment = segments[segments.length - 1];

        return Long.parseLong(lastSegment);
    }
}
