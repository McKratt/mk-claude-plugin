package com.example.shop.configuration;

import com.example.shop.common.persistence.DbTransactions;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRouting;
import java.time.Clock;

/**
 * Application-wide assembly, used by main and by the *IT tests: shared objects (Clock, one DbClient), then one
 * configuration per bounded context, the target context (billing) before the context that calls it (order),
 * which receives billing's incoming port.
 */
public final class ShopConfiguration {

    private ShopConfiguration() {
    }

    public static void routing(HttpRouting.Builder routing, Config config) {
        var clock = Clock.systemUTC();
        var dbConfig = config.get("db");
        var dbTransactions = DbTransactions.create(dbConfig);

        var billing = new BillingConfiguration(dbConfig, dbTransactions, clock);
        var order = new OrderConfiguration(dbConfig, dbTransactions, clock, billing.invoiceCreation());

        billing.routing(routing);
        order.routing(routing);
    }
}
