package com.example.shop;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared by every *IT test of the JVM: a real PostgreSQL in a container (Testcontainers) on a random port.
 * Started once, stopped by Testcontainers' Ryuk.
 */
final class TestInfrastructure {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    private TestInfrastructure() {
    }

    /**
     * The application's configuration on a random port, with the container's address taking precedence over
     * application.yaml.
     */
    static Config config() {
        return config(Map.of());
    }

    /** The same configuration, with these keys replaced. */
    static Config config(Map<String, String> overrides) {
        var values = new HashMap<>(Map.of(
                "server.port", "0",
                "db.connection.url", POSTGRES.getJdbcUrl(),
                "db.connection.username", POSTGRES.getUsername(),
                "db.connection.password", POSTGRES.getPassword()));
        values.putAll(overrides);
        return Config.builder()
                .addSource(ConfigSources.create(values))
                .addSource(ConfigSources.classpath("application.yaml"))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();
    }

    /** Runs a statement directly on the database, for the tests' setup. */
    static void execute(String sql) {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The number of rows of a table, for the tests' assertions. */
    static long count(String table) {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var resultSet = connection.createStatement().executeQuery("SELECT count(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The number of connections open in PostgreSQL under this JDBC application name. */
    static long connectionsOf(String applicationName) {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.prepareStatement(
                        "SELECT count(*) FROM pg_stat_activity WHERE application_name = ?")) {
            statement.setString(1, applicationName);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Waits for PostgreSQL to see the connections gone: a backend ends shortly after its client closes it. */
    static void awaitNoConnectionsOf(String applicationName) {
        var deadline = Instant.now().plusSeconds(10);
        while (connectionsOf(applicationName) > 0) {
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("Connections of " + applicationName + " are still open");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }
}
