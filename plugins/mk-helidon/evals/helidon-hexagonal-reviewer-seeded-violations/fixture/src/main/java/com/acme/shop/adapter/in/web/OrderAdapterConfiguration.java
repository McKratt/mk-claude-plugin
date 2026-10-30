package com.acme.shop.adapter.in.web;

import com.acme.shop.application.port.in.OrderPlacement;
import io.helidon.webserver.http.HttpRouting;

public final class OrderAdapterConfiguration {

    private OrderAdapterConfiguration() {
    }

    public static void register(HttpRouting.Builder routing, OrderPlacement placeOrder) {
        routing.register("/orders", new OrderController(placeOrder));
    }
}
