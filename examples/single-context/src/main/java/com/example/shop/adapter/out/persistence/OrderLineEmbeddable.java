package com.example.shop.adapter.out.persistence;

import jakarta.persistence.Embeddable;

@Embeddable
class OrderLineEmbeddable {

    String sku;
    int quantity;

    protected OrderLineEmbeddable() {
    }

    OrderLineEmbeddable(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }
}
