package com.example.shop;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.dbclient.DbClient;
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

    /** Direct database access for the tests' setup and assertions. */
    static DbClient dbClient() {
        return DbClient.create(config().get("db"));
    }
}
