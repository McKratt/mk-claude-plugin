package com.example.shop.adapter.out.shipping;

import com.example.shop.domain.Order;
import com.example.shop.application.port.out.ShipmentRequester;
import io.helidon.http.Status;
import io.helidon.webclient.api.WebClient;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** Calls the public REST API of the shipping company. The payload is owned by this adapter. */
@RequiredArgsConstructor
class ShippingRestAdapter implements ShipmentRequester {

    record ShipmentRequest(UUID orderId, int itemCount) {
    }

    private final WebClient webClient;

    @Override
    public void requestShipmentFor(Order order) {
        int itemCount = order.getLines().stream().mapToInt(Order.Line::quantity).sum();
        try (var response = webClient.post("/shipments")
                .submit(new ShipmentRequest(order.getId().value(), itemCount))) {
            // Unlike Spring's RestClient, Helidon's WebClient does not throw on an error status.
            if (response.status().family() != Status.Family.SUCCESSFUL) {
                throw new IllegalStateException("Shipping API answered " + response.status());
            }
        }
    }
}
