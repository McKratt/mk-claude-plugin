package com.example.shop;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared by every *IT test of the JVM: a real PostgreSQL in a container (Testcontainers) and WireMock for the
 * third-party shipping API, both on random ports. Started once, stopped by Testcontainers' Ryuk and the JVM.
 */
final class TestInfrastructure {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    static final WireMockServer SHIPPING = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        POSTGRES.start();
        SHIPPING.start();
    }

    private TestInfrastructure() {
    }

    /** The application's configuration, with the containers' addresses taking precedence over application.yaml. */
    static Config config() {
        return Config.builder()
                .addSource(ConfigSources.create(Map.of(
                        "db.connection.url", POSTGRES.getJdbcUrl(),
                        "db.connection.username", POSTGRES.getUsername(),
                        "db.connection.password", POSTGRES.getPassword(),
                        "shipping.base-url", SHIPPING.baseUrl())))
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
