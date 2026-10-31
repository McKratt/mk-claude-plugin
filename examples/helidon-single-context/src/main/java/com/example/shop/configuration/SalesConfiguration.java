package com.example.shop.configuration;

import com.example.shop.adapter.in.web.SalesWebAdapterConfiguration;
import com.example.shop.adapter.out.persistence.SalesPersistenceAdapterConfiguration;
import com.example.shop.adapter.out.shipping.SalesShippingRestAdapterConfiguration;
import com.example.shop.application.service.PlaceOrderService;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRouting;
import java.time.Clock;

/**
 * Wiring of the sales domain: outgoing adapters, then services, then incoming adapters. No container. The connection
 * pool is closed when the server stops.
 */
public final class SalesConfiguration {

    private SalesConfiguration() {
    }

    public static void routing(HttpRouting.Builder routing, Config config, Clock clock) {
        var persistence = new SalesPersistenceAdapterConfiguration(config.get("db"));
        routing.addFeature(new CloseOnStop(persistence));
        var shipmentRequester = SalesShippingRestAdapterConfiguration.shipmentRequester(config.get("shipping"));

        var orderPlacement = new PlaceOrderService(
                persistence.orderStore(), shipmentRequester, persistence.transactionRunner(), clock);

        SalesWebAdapterConfiguration.routing(routing, orderPlacement);
    }
}
