package com.example.shop.adapter.in.web;

import com.example.shop.application.port.in.OrderPlacement;
import io.helidon.webserver.http.HttpRules;

public final class SalesWebAdapterConfiguration {

    private SalesWebAdapterConfiguration() {
    }

    /** Registers the web adapters of the domain; they depend on incoming ports only. */
    public static void routing(HttpRules rules, OrderPlacement orderPlacement) {
        rules.register("/orders", new PlaceOrderController(orderPlacement));
    }
}
