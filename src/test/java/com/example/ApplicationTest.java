package com.example;

import com.example.testing.AutoConfigureDatabaseContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureDatabaseContainer
class ApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void findingUnknownAccount() {
        webTestClient.get()
                .uri("/accounts/5")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody().isEmpty();
    }
}
