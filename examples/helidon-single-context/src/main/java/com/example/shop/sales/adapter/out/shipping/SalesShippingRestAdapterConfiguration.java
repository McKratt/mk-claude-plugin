package com.example.shop.sales.adapter.out.shipping;

import com.example.shop.sales.application.port.out.ShipmentRequester;
import io.helidon.config.Config;
import io.helidon.webclient.api.WebClient;

public final class SalesShippingRestAdapterConfiguration {

    private SalesShippingRestAdapterConfiguration() {
    }

    /** @param shippingConfig the {@code shipping} node: {@code base-url} and optional WebClient settings under {@code client} */
    public static ShipmentRequester shipmentRequester(Config shippingConfig) {
        // Helidon's WebClient, configured from Helidon Config: never a raw java.net.http.HttpClient.
        var webClient = WebClient.builder()
                .config(shippingConfig.get("client"))
                .baseUri(shippingConfig.get("base-url").asString().get())
                .build();
        return new ShippingRestAdapter(webClient);
    }
}
