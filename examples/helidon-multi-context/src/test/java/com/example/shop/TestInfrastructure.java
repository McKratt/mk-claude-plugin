package com.example.shop;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import java.sql.DriverManager;
import java.sql.SQLException;
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

    /** The application's configuration, with the container's address taking precedence over application.yaml. */
    static Config config() {
        return Config.builder()
                .addSource(ConfigSources.create(Map.of(
                        "db.connection.url", POSTGRES.getJdbcUrl(),
                        "db.connection.username", POSTGRES.getUsername(),
                        "db.connection.password", POSTGRES.getPassword())))
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
}
