package com.acme.shop.adapter.out.persistence;

import com.acme.shop.application.port.out.OrderStore;
import com.acme.shop.domain.Order;
import io.helidon.dbclient.DbClient;

class OrderPersistenceAdapter implements OrderStore {

    private final DbClient db;

    OrderPersistenceAdapter(DbClient db) {
        this.db = db;
    }

    @Override
    public void save(Order order) {
        db.execute().insert("INSERT INTO orders (id, customer_id, product_id) VALUES (?, ?, ?)",
                order.getId(), order.getCustomerId(), order.getProductId());
    }
}
