package com.github.freeacs.config;

import com.github.freeacs.common.hikari.DatabaseConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URISyntaxException;

@Configuration
public class DataSourceConfig {

    /**
     * Standardizes the DataSource creation.
     * Supports both environment variables (DATABASE_URL) and application properties.
     */
    @Bean
    @Primary
    public DataSource dataSource(Environment env) throws URISyntaxException {
        String jdbcUrl = env.getProperty("main.datasource.jdbc-url");
        if (jdbcUrl == null) {
            jdbcUrl = env.getProperty("DATABASE_URL");
        }
        if (jdbcUrl == null) {
            jdbcUrl = env.getProperty("main.datasource.jdbcUrl");
        }

        if (jdbcUrl == null) {
            throw new IllegalArgumentException("JDBC URL is required! Check main.datasource.jdbc-url property.");
        }

        return DatabaseConfig.builder()
                .jdbcUrl(jdbcUrl)
                .driverClassName(env.getProperty("main.datasource.driver-class-name"))
                .username(env.getProperty("main.datasource.username"))
                .password(env.getProperty("main.datasource.password"))
                .minimumIdle(env.getProperty("main.datasource.minimum-idle", Integer.class, 2))
                .maximumPoolSize(env.getProperty("main.datasource.maximum-pool-size", Integer.class, 10))
                .poolName(env.getProperty("main.datasource.pool-name", "mainPool"))
                .build()
                .getDataSource();
    }
}
