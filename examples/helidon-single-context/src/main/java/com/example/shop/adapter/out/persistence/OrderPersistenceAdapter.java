package com.example.shop.adapter.out.persistence;

import com.example.shop.domain.Order;
import com.example.shop.application.port.out.OrderStore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;

/**
 * Maps the domain order to the orders and order_lines tables with plain JDBC. The SQL is owned by this adapter.
 * The PostgreSQL driver binds UUID and OffsetDateTime natively through setObject.
 */
@RequiredArgsConstructor
class OrderPersistenceAdapter implements OrderStore {

    private final JdbcTransactionRunner transactionRunner;

    @Override
    public void save(Order order) {
        var orderId = order.getId().value();
        transactionRunner.run(connection -> {
            try (var insertOrder = connection.prepareStatement("INSERT INTO orders (id, placed_at) VALUES (?, ?)");
                    var insertLine = connection.prepareStatement(
                            "INSERT INTO order_lines (order_id, sku, quantity) VALUES (?, ?, ?)")) {
                insertOrder.setObject(1, orderId);
                insertOrder.setObject(2, OffsetDateTime.ofInstant(order.getPlacedAt(), ZoneOffset.UTC));
                insertOrder.executeUpdate();
                for (var line : order.getLines()) {
                    insertLine.setObject(1, orderId);
                    insertLine.setString(2, line.sku());
                    insertLine.setInt(3, line.quantity());
                    insertLine.addBatch();
                }
                insertLine.executeBatch();
            }
        });
    }
}
