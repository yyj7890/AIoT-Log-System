package com.aiot.log.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "FLYWAY_TEST_DB_URL", matches = ".+")
class FlywayMigrationIntegrationTest {

    private static final String ADMIN_URL = System.getenv("FLYWAY_TEST_DB_URL");
    private static final String USERNAME = System.getenv().getOrDefault("FLYWAY_TEST_DB_USERNAME", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("FLYWAY_TEST_DB_PASSWORD", "");
    private static final String V1_MIGRATION = "db/migration/V1__create_initial_schema.sql";
    private static final int EXPECTED_BUSINESS_TABLES = 11;

    @Test
    void emptyDatabaseExecutesAllMigrationsThroughV6() throws Exception {
        String database = databaseName("empty");
        createDatabase(database);
        try {
            Flyway flyway = flyway(database);
            flyway.migrate();

            assertEquals(EXPECTED_BUSINESS_TABLES, countBusinessTables(database));
            assertMigrationRecord(database, "1", "SQL");
            assertMigrationRecord(database, "2", "SQL");
            assertMigrationRecord(database, "3", "SQL");
            assertMigrationRecord(database, "4", "SQL");
            assertMigrationRecord(database, "5", "SQL");
            assertMigrationRecord(database, "6", "SQL");
            assertMonitoringMode(database, "LOG_ONLY");
        } finally {
            dropDatabase(database);
        }
    }

    @Test
    void existingV1DatabaseIsBaselinedThenUpgradedThroughV6WithoutLosingData() throws Exception {
        String database = databaseName("legacy");
        createDatabase(database);
        try {
            try (Connection connection = connect(database)) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource(V1_MIGRATION));
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO devices(name, device_code, type, status) VALUES (?, ?, ?, ?)")) {
                    statement.setString(1, "迁移保留设备");
                    statement.setString(2, "FLYWAY-KEEP-001");
                    statement.setString(3, "TEST");
                    statement.setString(4, "NORMAL");
                    statement.executeUpdate();
                }
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("""
                            INSERT INTO logs(device_id, title, content, log_type, level, status, source)
                            SELECT id, '迁移保留日志', 'V1日志内容', 'RUNNING', 'INFO', 'RESOLVED', 'DEVICE'
                            FROM devices WHERE device_code = 'FLYWAY-KEEP-001'
                            """);
                    statement.executeUpdate("""
                            INSERT INTO device_reports(
                                device_id, temperature, humidity, voltage, signal_strength, status, message, reported_at)
                            SELECT id, 25.50, 60.00, 3.30, -55, 'NORMAL', 'V1上报数据', CURRENT_TIMESTAMP
                            FROM devices WHERE device_code = 'FLYWAY-KEEP-001'
                            """);
                }
            }

            flyway(database).migrate();

            assertEquals(1, countRows(
                    database,
                    "SELECT COUNT(*) FROM devices WHERE device_code = 'FLYWAY-KEEP-001'"));
            assertEquals(1, countRows(
                    database,
                    "SELECT COUNT(*) FROM logs WHERE title = '迁移保留日志'"));
            assertEquals(1, countRows(
                    database,
                    "SELECT COUNT(*) FROM device_reports WHERE message = 'V1上报数据'"));
            assertEquals(1, countRows(
                    database,
                    "SELECT COUNT(*) FROM devices "
                            + "WHERE device_code = 'FLYWAY-KEEP-001' AND monitoring_mode = 'LOG_ONLY'"));
            assertEquals(EXPECTED_BUSINESS_TABLES, countBusinessTables(database));
            assertMigrationRecord(database, "1", "BASELINE");
            assertMigrationRecord(database, "2", "SQL");
            assertMigrationRecord(database, "3", "SQL");
            assertMigrationRecord(database, "4", "SQL");
            assertMigrationRecord(database, "5", "SQL");
            assertMigrationRecord(database, "6", "SQL");
            assertMonitoringMode(database, "LOG_ONLY");
        } finally {
            dropDatabase(database);
        }
    }

    private Flyway flyway(String database) {
        return Flyway.configure()
                .dataSource(databaseUrl(database), USERNAME, PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();
    }

    private void assertMigrationRecord(String database, String version, String expectedType) throws Exception {
        try (Connection connection = connect(database);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT type, success FROM flyway_schema_history WHERE version = ?")) {
            statement.setString(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(expectedType, resultSet.getString("type").toUpperCase(Locale.ROOT));
                assertTrue(resultSet.getBoolean("success"));
            }
        }
    }

    private void assertMonitoringMode(String database, String expectedDefault) throws Exception {
        try (Connection connection = connect(database);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT column_default FROM information_schema.columns "
                             + "WHERE table_schema = ? AND table_name = 'devices' AND column_name = 'monitoring_mode'")) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(expectedDefault, resultSet.getString("column_default"));
            }
        }
    }

    private int countBusinessTables(String database) throws Exception {
        try (Connection connection = connect(database);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM information_schema.tables "
                             + "WHERE table_schema = ? AND table_name <> 'flyway_schema_history'")) {
            statement.setString(1, database);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int countRows(String database, String sql) throws Exception {
        try (Connection connection = connect(database);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void createDatabase(String database) throws Exception {
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + database
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
    }

    private void dropDatabase(String database) throws Exception {
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + database + "`");
        }
    }

    private Connection connect(String database) throws Exception {
        return DriverManager.getConnection(databaseUrl(database), USERNAME, PASSWORD);
    }

    private String databaseUrl(String database) {
        int queryIndex = ADMIN_URL.indexOf('?');
        String base = queryIndex >= 0 ? ADMIN_URL.substring(0, queryIndex) : ADMIN_URL;
        String query = queryIndex >= 0 ? ADMIN_URL.substring(queryIndex) : "";
        int slashIndex = base.lastIndexOf('/');
        return base.substring(0, slashIndex + 1) + database + query;
    }

    private String databaseName(String purpose) {
        return "aiot_regression_" + purpose + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
