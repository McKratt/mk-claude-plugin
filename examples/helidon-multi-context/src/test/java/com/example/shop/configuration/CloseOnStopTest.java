package com.example.shop.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class CloseOnStopTest {

    @Test
    void should_close_the_resource_when_the_server_stops() {
        var closed = new AtomicBoolean();
        var server = WebServer.builder()
                .port(0)
                .routing(routing -> routing.addFeature(new CloseOnStop(() -> closed.set(true))))
                .build()
                .start();
        assertThat(closed).isFalse();

        server.stop();

        assertThat(closed).isTrue();
    }

    @Test
    void should_let_running_requests_complete_for_the_configured_grace_period() {
        var config = Config.just(ConfigSources.classpath("application.yaml"));

        var serverConfig = WebServerConfig.create(config.get("server"));

        assertThat(serverConfig.shutdownGracePeriod()).isEqualTo(Duration.ofSeconds(30));
    }
}
