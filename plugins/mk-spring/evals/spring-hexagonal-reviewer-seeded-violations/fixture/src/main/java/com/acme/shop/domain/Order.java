package com.acme.shop.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

import java.util.UUID;

@Entity
@Getter
public class Order {

    @Id
    private UUID id;
    private String customerId;

    protected Order() {
    }

    private Order(UUID id, String customerId) {
        this.id = id;
        this.customerId = customerId;
    }

    public static Order place(String customerId) {
        return new Order(UUID.randomUUID(), customerId);
    }
}
