package com.example.shop.configuration;

import com.example.shop.common.persistence.DbTransactions;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import java.time.Clock;

/**
 * Application-wide assembly: the one way to start the application, used by main and by the *IT tests. It creates the
 * shared objects (Clock, one connection pool), then one configuration per bounded context, the target context (billing)
 * before the context that calls it (order), which receives billing's incoming port.
 */
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
            var server = WebServer.builder()
                    .config(config.get("server"))
                    .routing(routing -> {
                        routing.addFeature(new CloseOnStop(resources));
                        routing(routing, config, resources);
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

    private static void routing(HttpRouting.Builder routing, Config config, ApplicationResources resources) {
        var clock = Clock.systemUTC();
        var dbTransactions = DbTransactions.create(config.get("db"));
        resources.add(dbTransactions);

        var billing = new BillingConfiguration(dbTransactions, clock);
        var order = new OrderConfiguration(dbTransactions, clock, billing.invoiceCreation());

        billing.routing(routing);
        order.routing(routing);
    }
}
