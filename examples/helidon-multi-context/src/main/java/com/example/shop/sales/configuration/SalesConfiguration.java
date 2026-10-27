package com.example.shop.sales.configuration;

import com.example.shop.sales.billing.configuration.BillingConfiguration;
import com.example.shop.sales.common.persistence.DbTransactions;
import com.example.shop.sales.order.configuration.OrderConfiguration;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRules;
import java.time.Clock;

/**
 * Wiring of the sales component: one shared DbClient, then the target context (billing) before the context
 * that calls it (order), which receives billing's incoming port.
 */
public final class SalesConfiguration {

    private SalesConfiguration() {
    }

    public static void routing(HttpRules rules, Config config, Clock clock) {
        var dbConfig = config.get("db");
        var dbTransactions = DbTransactions.create(dbConfig);

        var billing = new BillingConfiguration(dbConfig, dbTransactions, clock);
        var order = new OrderConfiguration(dbConfig, dbTransactions, clock, billing.invoiceCreation());

        billing.routing(rules);
        order.routing(rules);
    }
}
