package com.github.freeacs.utils;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MySQLContainer;

public interface MysqlDataSourceInitializer {

    static void initialize(MySQLContainer<?> databaseTestContainer, @NotNull ConfigurableApplicationContext applicationContext) {
        // Ensure the container is running before extracting properties
        if (!databaseTestContainer.isRunning()) {
            databaseTestContainer.start();
        }

        // Using TestPropertyValues is more robust for programmatic injection
        TestPropertyValues.of(
                "spring.test.database.replace=none",
                "main.datasource.jdbc-url=" + databaseTestContainer.getJdbcUrl(), // Fixed key
                "main.datasource.username=" + databaseTestContainer.getUsername(),
                "main.datasource.password=" + databaseTestContainer.getPassword(),
                "main.datasource.driver-class-name=" + databaseTestContainer.getDriverClassName(),
                "spring.main.allow-bean-definition-overriding=true" // Critical for multi-module conflicts
        ).applyTo(applicationContext.getEnvironment());
    }
}
