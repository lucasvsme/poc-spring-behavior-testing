package com.example.testing;

import com.example.testing.AutoConfigureDatabaseContainer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureDatabaseContainer
@CucumberContextConfiguration
public class CucumberTestContextConfiguration {

}
