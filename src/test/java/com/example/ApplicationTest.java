package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class ApplicationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINERS =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres"));

    @DynamicPropertySource
    private static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINERS::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINERS::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINERS::getPassword);
    }

    @Test
    void contextLoads() {
    }
}
