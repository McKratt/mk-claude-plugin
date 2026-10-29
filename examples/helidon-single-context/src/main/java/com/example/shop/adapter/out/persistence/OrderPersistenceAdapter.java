package com.example.shop.adapter.out.persistence;

import com.example.shop.domain.Order;
import com.example.shop.application.port.out.OrderStore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;

/**
 * Maps the domain order to the orders and order_lines tables. The SQL is owned by this adapter.
 * DbClient binds a UUID as a string, hence the explicit casts.
 */
@RequiredArgsConstructor
class OrderPersistenceAdapter implements OrderStore {

    private final DbClientTransactionRunner transactionRunner;

    @Override
    public void save(Order order) {
        var db = transactionRunner.executor();
        var orderId = order.getId().value();
        db.insert("INSERT INTO orders (id, placed_at) VALUES (CAST(? AS UUID), ?)",
                orderId, OffsetDateTime.ofInstant(order.getPlacedAt(), ZoneOffset.UTC));
        for (var line : order.getLines()) {
            db.insert("INSERT INTO order_lines (order_id, sku, quantity) VALUES (CAST(? AS UUID), ?, ?)",
                    orderId, line.sku(), line.quantity());
        }
    }
}
