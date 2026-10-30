package com.example.shop.order.adapter.out.persistence;

import com.example.shop.common.persistence.DbTransactions;
import com.example.shop.order.domain.Order;
import com.example.shop.order.application.port.out.OrderStore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;

/**
 * Maps the domain order to the tables of the order context's schema with plain JDBC. The SQL is owned by this
 * adapter. The PostgreSQL driver binds UUID and OffsetDateTime natively through setObject.
 */
@RequiredArgsConstructor
class OrderPersistenceAdapter implements OrderStore {

    private final DbTransactions dbTransactions;

    @Override
    public void save(Order order) {
        var orderId = order.getId().value();
        dbTransactions.run(connection -> {
            try (var insertOrder = connection.prepareStatement(
                            "INSERT INTO shop_order.orders (id, placed_at) VALUES (?, ?)");
                    var insertLine = connection.prepareStatement(
                            "INSERT INTO shop_order.order_lines (order_id, sku, quantity) VALUES (?, ?, ?)")) {
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
