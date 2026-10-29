package com.example.shop.application.service;

import com.example.shop.domain.Order;
import com.example.shop.domain.OrderId;
import com.example.shop.application.port.in.OrderPlacement;
import com.example.shop.application.port.in.PlaceOrderCommand;
import com.example.shop.application.port.out.OrderStore;
import com.example.shop.application.port.out.ShipmentRequester;
import com.example.shop.application.port.out.TransactionRunner;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

// Application service: plain Java, no Helidon type, no annotation. It is created by the domain's configuration
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
