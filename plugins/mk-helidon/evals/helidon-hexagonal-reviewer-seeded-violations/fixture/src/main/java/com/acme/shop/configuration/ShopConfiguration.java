package com.acme.shop.configuration;

import com.acme.shop.adapter.in.web.OrderAdapterConfiguration;
import com.acme.shop.adapter.out.persistence.OrderPersistenceAdapterConfiguration;
import com.acme.shop.application.service.PlaceOrderService;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;

public final class ShopConfiguration {

    private ShopConfiguration() {
    }

    public static WebServer start(Config config) {
        var orderStore = OrderPersistenceAdapterConfiguration.orderStore(config.get("db"));
        var placeOrder = new PlaceOrderService(orderStore);
        return WebServer.builder()
                .config(config.get("server"))
                .routing(routing -> OrderAdapterConfiguration.register(routing, placeOrder))
                .build()
                .start();
    }
}
