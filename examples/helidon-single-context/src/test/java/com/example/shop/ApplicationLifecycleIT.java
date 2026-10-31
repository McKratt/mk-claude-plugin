package com.example.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.shop.configuration.ShopConfiguration;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The application's own startup and shutdown. Each test names its connection pool in the JDBC URL, so that PostgreSQL
 * shows whether the connections of that pool are really closed.
 */
class ApplicationLifecycleIT {

    @Test
    void should_close_the_connection_pool_when_the_application_is_closed() {
        var pool = "closed-application";
        var application = ShopConfiguration.start(TestInfrastructure.config(Map.of("db.connection.url", jdbcUrl(pool))));
        assertThat(TestInfrastructure.connectionsOf(pool)).isPositive();

        application.close();

        TestInfrastructure.awaitNoConnectionsOf(pool);
    }

    @Test
    void should_fail_and_close_the_connection_pool_when_the_port_is_taken() throws IOException {
        var pool = "port-taken";
        try (var taken = new ServerSocket(0, 1, InetAddress.getByName("0.0.0.0"))) {
            var config = TestInfrastructure.config(Map.of(
                    "server.port", String.valueOf(taken.getLocalPort()),
                    "db.connection.url", jdbcUrl(pool)));

            assertThatThrownBy(() -> ShopConfiguration.start(config)).isInstanceOf(IllegalStateException.class);
        }

        TestInfrastructure.awaitNoConnectionsOf(pool);
    }

    @Test
    void should_fail_and_close_the_connection_pool_when_the_migration_fails() {
        var pool = "failed-migration";
        TestInfrastructure.execute("CREATE SCHEMA broken");
        TestInfrastructure.execute("CREATE TABLE broken.flyway_schema_history (not_what_flyway_expects INTEGER)");
        var config = TestInfrastructure.config(Map.of(
                "db.connection.url", jdbcUrl(pool) + "&currentSchema=broken"));

        assertThatThrownBy(() -> ShopConfiguration.start(config)).isInstanceOf(RuntimeException.class);

        TestInfrastructure.awaitNoConnectionsOf(pool);
    }

    private static String jdbcUrl(String pool) {
        return TestInfrastructure.POSTGRES.getJdbcUrl() + "&ApplicationName=" + pool;
    }
}
