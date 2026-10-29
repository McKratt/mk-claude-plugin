package com.example.shop.sales.order.adapter.out.persistence;

import com.example.shop.sales.common.persistence.DbTransactions;
import com.example.shop.sales.order.domain.Order;
import com.example.shop.sales.order.application.port.out.OrderStore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;

/**
 * Maps the domain order to the tables of the order context's schema. The SQL is owned by this adapter.
 * DbClient binds a UUID as a string, hence the explicit casts.
 */
@RequiredArgsConstructor
class OrderPersistenceAdapter implements OrderStore {

    private final DbTransactions dbTransactions;

    @Override
    public void save(Order order) {
        var db = dbTransactions.executor();
        var orderId = order.getId().value();
        db.insert("INSERT INTO sales_order.orders (id, placed_at) VALUES (CAST(? AS UUID), ?)",
                orderId, OffsetDateTime.ofInstant(order.getPlacedAt(), ZoneOffset.UTC));
        for (var line : order.getLines()) {
            db.insert("INSERT INTO sales_order.order_lines (order_id, sku, quantity) VALUES (CAST(? AS UUID), ?, ?)",
                    orderId, line.sku(), line.quantity());
        }
    }
}
