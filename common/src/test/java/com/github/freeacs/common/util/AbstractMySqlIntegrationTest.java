package com.github.freeacs.common.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public interface AbstractMySqlIntegrationTest {

    // Singleton Container: Shared across all test classes for performance
    MySQLContainer<?> databaseTestContainer = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName("freeacs")
            .withUsername("freeacs")
            .withPassword("freeacs");

    class DataSourceHolder {
        private static HikariDataSource dataSource;

        static synchronized DataSource get() throws SQLException {
            if (!databaseTestContainer.isRunning()) {
                databaseTestContainer.start();
            }
            if (dataSource == null) {
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(databaseTestContainer.getJdbcUrl());
                config.setUsername(databaseTestContainer.getUsername());
                config.setPassword(databaseTestContainer.getPassword());
                config.setDriverClassName(databaseTestContainer.getDriverClassName());
                config.setMaximumPoolSize(10);
                dataSource = new HikariDataSource(config);
            }
            return dataSource;
        }
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        DataSource ds = DataSourceHolder.get();
        try (Connection conn = ds.getConnection();
             Statement st = conn.createStatement()) {

            // Only run install script if the database is empty
            ResultSet rs = st.executeQuery("SHOW TABLES");
            if (!rs.next()) {
                // This creates tables and might insert base users
                DBScriptUtility.runScript("mysql/install.sql", conn);
            }
        }
    }

    @BeforeEach
    default void resetDatabase() throws Exception {
        try (Connection conn = getDataSource().getConnection();
             Statement st = conn.createStatement()) {

            // Disable Foreign Key checks to allow truncating tables with dependencies
            st.execute("SET FOREIGN_KEY_CHECKS = 0");

            ResultSet rs = st.executeQuery("SHOW TABLES");
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                String tableName = rs.getString(1);
                // SKIP truncating the 'user_' table because it contains base data from install.sql
                // and is required as a foreign key for 'filestore' and other tables.
                if (!tableName.equalsIgnoreCase("user_")) {
                    tables.add(tableName);
                }
            }

            for (String table : tables) {
                st.execute("TRUNCATE TABLE " + table);
            }

            // Re-enable Foreign Key checks
            st.execute("SET FOREIGN_KEY_CHECKS = 1");

            // Restore other transient test data
            DBScriptUtility.runScript("seed.sql", conn);
        }
    }


    @NotNull
    static DataSource getDataSource() throws SQLException {
        return DataSourceHolder.get();
    }
}
