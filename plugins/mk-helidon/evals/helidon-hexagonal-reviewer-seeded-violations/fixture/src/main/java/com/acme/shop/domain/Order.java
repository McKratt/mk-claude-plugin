package com.acme.shop.domain;

import io.helidon.dbclient.DbRow;
import lombok.Getter;

import java.util.UUID;

@Getter
public class Order {

    private final UUID id;
    private final String customerId;
    private final String productId;

    private Order(UUID id, String customerId, String productId) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
    }

    public static Order place(String customerId, String productId) {
        return new Order(UUID.randomUUID(), customerId, productId);
    }

    public static Order fromRow(DbRow row) {
        return new Order(row.column("id").get(UUID.class),
                row.column("customer_id").getString(),
                row.column("product_id").getString());
    }
}
