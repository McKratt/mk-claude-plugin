package com.example.shop.configuration;

import com.example.shop.sales.configuration.SalesConfiguration;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRouting;
import java.time.Clock;

/** Application-wide assembly: shared objects, then one call per component. Used by main and by the *IT tests. */
public final class ShopConfiguration {

    private ShopConfiguration() {
    }

    public static void routing(HttpRouting.Builder routing, Config config) {
        var clock = Clock.systemUTC();
        SalesConfiguration.routing(routing, config, clock);
    }
}
