package com.example.shop.sales.application.domain.model;

import com.example.shop.sales.common.Preconditions;
import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        Preconditions.requireNonNull(value, "value");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
}
