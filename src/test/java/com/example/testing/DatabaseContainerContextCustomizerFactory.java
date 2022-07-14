package com.example.testing;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

public final class DatabaseContainerContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
                                                     List<ContextConfigurationAttributes> configAttributes) {
        if (!AnnotatedElementUtils.hasAnnotation(testClass, AutoConfigureDatabaseContainer.class)) {
            return null;
        }

        return (configurableApplicationContext, mergedContextConfiguration) -> {
            final var container = new PostgreSQLContainer<>(DockerImageName.parse("postgres"))
                    .withReuse(false);

            container.start();

            configurableApplicationContext.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource("default", Map.ofEntries(
                            Map.entry("spring.datasource.url", container.getJdbcUrl()),
                            Map.entry("spring.datasource.username", container.getUsername()),
                            Map.entry("spring.datasource.password", container.getPassword()),
                            Map.entry("spring.datasource.driver-class-name", container.getDriverClassName()),
                            Map.entry("spring.test.database.replace", "NONE")
                    )));
        };
    }
}
