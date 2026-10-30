package com.acme.shop.application.port.in;

import java.util.Objects;

public record PlaceOrderCommand(String customerId) {

    public PlaceOrderCommand {
        Objects.requireNonNull(customerId, "customerId");
    }
}
