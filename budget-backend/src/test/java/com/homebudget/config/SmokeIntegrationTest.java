package com.homebudget.config;

import com.homebudget.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

class SmokeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void mysqlContainerIsRunning() {
        assertThat(mysql.isRunning()).isTrue();
    }

    @Test
    void dataSourceIsConfigured() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
        }
    }

    @Test
    void liquibaseMigrationsCompleted() throws Exception {
        try (Connection connection = dataSource.getConnection();
             ResultSet rs = connection.createStatement()
                     .executeQuery("SELECT COUNT(*) FROM DATABASECHANGELOG")) {
            rs.next();
            int changelogCount = rs.getInt(1);
            assertThat(changelogCount).isGreaterThan(0);
        }
    }

    @Test
    void springContextLoadsSuccessfully() {
        assertThat(categoryRepository).isNotNull();
    }

    @Test
    void tablesCreatedByLiquibase() throws Exception {
        try (Connection connection = dataSource.getConnection();
             ResultSet rs = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            boolean hasBudgets = false;
            boolean hasCategories = false;
            boolean hasExpenses = false;
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME").toLowerCase();
                if ("budgets".equals(tableName)) hasBudgets = true;
                if ("categories".equals(tableName)) hasCategories = true;
                if ("expenses".equals(tableName)) hasExpenses = true;
            }
            assertThat(hasBudgets).as("budgets table exists").isTrue();
            assertThat(hasCategories).as("categories table exists").isTrue();
            assertThat(hasExpenses).as("expenses table exists").isTrue();
        }
    }
}
