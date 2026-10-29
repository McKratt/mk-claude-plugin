package com.example.shop.domain;

import com.example.shop.common.Preconditions;
import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        Preconditions.requireNonNull(value, "value");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
}
