package com.acme.shop.application.port.in;

import java.util.Objects;

public record PlaceOrderCommand(String customerId, String productId) {

    public PlaceOrderCommand {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(productId, "productId");
    }
}
