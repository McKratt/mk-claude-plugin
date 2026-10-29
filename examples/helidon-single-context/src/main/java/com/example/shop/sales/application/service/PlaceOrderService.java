package com.example.shop.sales.application.service;

import com.example.shop.sales.domain.Order;
import com.example.shop.sales.domain.OrderId;
import com.example.shop.sales.application.port.in.OrderPlacement;
import com.example.shop.sales.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.application.port.out.OrderStore;
import com.example.shop.sales.application.port.out.ShipmentRequester;
import com.example.shop.sales.application.port.out.TransactionRunner;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

// Application service: plain Java, no Helidon type, no annotation. It is created by the hexagon's configuration
// class, runs the use case and owns its cross-cutting concerns:
// the transaction boundary, through the TransactionRunner port.
@RequiredArgsConstructor
public class PlaceOrderService implements OrderPlacement {

    private final OrderStore orderStore;
    private final ShipmentRequester shipmentRequester;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    @Override
    public OrderId placeOrder(PlaceOrderCommand command) {
        var lines = command.lines().stream()
                .map(line -> new Order.Line(line.sku(), line.quantity()))
                .toList();
        return transactionRunner.inTransaction(() -> {
            var order = Order.place(lines, clock.instant());
            orderStore.save(order);
            shipmentRequester.requestShipmentFor(order);
            return order.getId();
        });
    }
}
