package com.example.shop.application.service;

import com.example.shop.domain.Order;
import com.example.shop.domain.OrderId;
import com.example.shop.application.port.in.PlaceOrderCommand;
import com.example.shop.application.port.in.OrderPlacement;
import com.example.shop.application.port.out.ShipmentRequester;
import com.example.shop.application.port.out.OrderStore;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

// Application service: it runs the use case and owns its cross-cutting concerns, here the transaction.
// No stereotype annotation: the bean is declared in the domain's file of the configuration package. @Transactional
// goes on the methods, never on the class, so each method chooses its own transaction (read-only or not).
@RequiredArgsConstructor
public class PlaceOrderService implements OrderPlacement {

    private final OrderStore orderStore;
    private final ShipmentRequester shipmentRequester;
    private final Clock clock;

    @Override
    @Transactional
    public OrderId placeOrder(PlaceOrderCommand command) {
        var lines = command.lines().stream()
                .map(line -> new Order.Line(line.sku(), line.quantity()))
                .toList();
        var order = Order.place(lines, clock.instant());
        orderStore.save(order);
        shipmentRequester.requestShipmentFor(order);
        return order.getId();
    }
}
