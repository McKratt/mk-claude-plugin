package com.example.shop.configuration;

import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import java.time.Clock;

/** Application-wide assembly: the one way to start the application, used by main and by the *IT tests. */
public final class ShopConfiguration {

    private ShopConfiguration() {
    }

    /**
     * Wires the application and starts its server.
     *
     * @param config the whole configuration, whose {@code server} node configures the server
     * @return the started application; closing it stops the server and releases the resources
     * @throws RuntimeException if the startup fails, once what was already created is released
     */
    public static RunningApplication start(Config config) {
        var resources = new ApplicationResources();
        try {
            var clock = Clock.systemUTC();
            var server = WebServer.builder()
                    .config(config.get("server"))
                    .routing(routing -> {
                        routing.addFeature(new CloseOnStop(resources));
                        SalesConfiguration.routing(routing, config, clock, resources);
                    })
                    .build()
                    .start();
            // Helidon logs a failed start, such as a port already in use, and returns a stopped server.
            if (!server.isRunning()) {
                throw new IllegalStateException("The web server failed to start, see the log");
            }
            return new RunningApplication(server);
        } catch (RuntimeException | Error e) {
            resources.close();
            throw e;
        }
    }
}
