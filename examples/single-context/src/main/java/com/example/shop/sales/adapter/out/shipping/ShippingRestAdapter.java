package com.example.shop.sales.adapter.out.shipping;

import com.example.shop.sales.application.domain.model.Order;
import com.example.shop.sales.application.port.out.ShipmentRequester;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

/** Calls the public REST API of the shipping company. The payload is owned by this adapter. */
@RequiredArgsConstructor
class ShippingRestAdapter implements ShipmentRequester {

    record ShipmentRequest(UUID orderId, int itemCount) {
    }

    private final RestClient restClient;

    @Override
    public void requestShipmentFor(Order order) {
        int itemCount = order.getLines().stream().mapToInt(Order.Line::quantity).sum();
        restClient.post()
                .uri("/shipments")
                .body(new ShipmentRequest(order.getId().value(), itemCount))
                .retrieve()
                .toBodilessEntity();
    }
}
