package com.example.shop.order.adapter.in.web;

import com.example.shop.order.application.port.in.OrderPlacement;
import io.helidon.webserver.http.HttpRules;

public final class OrderWebAdapterConfiguration {

    private OrderWebAdapterConfiguration() {
    }

    /** Registers the web adapters of the order context; they depend on incoming ports only. */
    public static void routing(HttpRules rules, OrderPlacement orderPlacement) {
        rules.register("/orders", new PlaceOrderController(orderPlacement));
    }
}
