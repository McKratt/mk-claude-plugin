package com.example.shop.configuration;

import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRouting;
import java.time.Clock;

/** Application-wide assembly: shared objects, then the configuration of the domain. Used by main and by the *IT tests. */
public final class ShopConfiguration {

    private ShopConfiguration() {
    }

    public static void routing(HttpRouting.Builder routing, Config config) {
        var clock = Clock.systemUTC();
        SalesConfiguration.routing(routing, config, clock);
    }
}
