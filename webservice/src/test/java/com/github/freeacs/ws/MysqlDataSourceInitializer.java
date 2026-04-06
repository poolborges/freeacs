package com.github.freeacs.ws;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.MySQLContainer;

public interface MysqlDataSourceInitializer {

    public static void initialize(MySQLContainer<?> databaseTestContainer, @NotNull ConfigurableApplicationContext applicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "spring.test.database.replace=none", // Tells Spring Boot not to start in-memory db for tests.
                "main.datasource.jdbcUrl=" + databaseTestContainer.getJdbcUrl(),
                "main.datasource.username=" + databaseTestContainer.getUsername(),
                "main.datasource.password=" + databaseTestContainer.getPassword()
        );
    }
}