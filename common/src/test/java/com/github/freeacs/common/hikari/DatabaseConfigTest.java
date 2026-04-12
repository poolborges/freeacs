package com.github.freeacs.common.hikari;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    void testGetDataSourceWithDirectJdbcUrl() throws URISyntaxException {
        DatabaseConfig config = DatabaseConfig.builder()
                .jdbcUrl("jdbc:mysql://localhost:3306/mydb?useSSL=false")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .username("user")
                .password("pass")
                .build();

        HikariConfig hikariConfig = config.getHikariConfig();
        assertEquals("com.mysql.cj.jdbc.Driver", hikariConfig.getDriverClassName());
        assertEquals("jdbc:mysql://localhost:3306/mydb?useSSL=false", hikariConfig.getJdbcUrl());
        assertEquals("user", hikariConfig.getUsername());
        assertEquals("pass", hikariConfig.getPassword());
    }

    /**
     * Test logic for URI parsing (common in cloud environments like Heroku).
     */
    @Test
    void testGetDataSourceWithUri() throws URISyntaxException {
        DatabaseConfig config = DatabaseConfig.builder()
                .jdbcUrl("mysql://user:pass@localhost:3306/mydb?useSSL=false")
                .build();

        HikariConfig hikariConfig = config.getHikariConfig();
        assertEquals("com.mysql.cj.jdbc.Driver", hikariConfig.getDriverClassName());
        assertEquals("jdbc:mysql://localhost:3306/mydb?useSSL=false", hikariConfig.getJdbcUrl());
        assertEquals("user", hikariConfig.getUsername());
        assertEquals("pass", hikariConfig.getPassword());
    }

    /**
     * IMPORTANT: Verifies that a standard JDBC URL without user info in the string
     * does NOT cause a NullPointerException.
     */
    @Test
    void testGetDataSourceWithStandardJdbcUrlNoUserInfo() throws URISyntaxException {
        DatabaseConfig config = DatabaseConfig.builder()
                .jdbcUrl("jdbc:mysql://localhost:3306/freeacs")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .username("freeacs")
                .password("freeacs")
                .build();

        // This should not throw NPE even if URI parsing logic exists
        HikariConfig hikariConfig = config.getHikariConfig();
        assertNotNull(hikariConfig);
        assertEquals("jdbc:mysql://localhost:3306/freeacs", hikariConfig.getJdbcUrl());
        assertEquals("freeacs", hikariConfig.getUsername());
    }

    @Test
    void testNoJdbcUrl() {
        DatabaseConfig config = DatabaseConfig.builder().build();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, config::getHikariConfig);
        assertEquals("JDBC URL is required", exception.getMessage());
    }
}
