package com.example.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.LifecycleProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;

/**
 * Starts the application as main does (real web server on a random port, PostgreSQL in a container), then stops
 * it: the web server shuts down gracefully, then the context closes its beans, the Hikari pool among them. The
 * test owns this context instead of sharing the cached one of the other *IT tests, since it closes it.
 */
class GracefulShutdownIT {

    @Test
    void should_shut_down_gracefully_and_close_the_connection_pool_when_the_application_stops() {
        var context = new SpringApplicationBuilder(ShopApplication.class, PostgresTestConfiguration.class)
                .properties("server.port=0")
                .run();
        var dataSource = context.getBean(HikariDataSource.class);
        assertThat(context.getBean(ServerProperties.class).getShutdown()).isEqualTo(Shutdown.GRACEFUL);
        assertThat(context.getBean(LifecycleProperties.class).getTimeoutPerShutdownPhase())
                .isEqualTo(Duration.ofSeconds(30));
        assertThat(dataSource.isClosed()).isFalse();

        context.close();

        assertThat(dataSource.isClosed()).isTrue();
    }
}
