package com.example.shop.sales.application.domain.service;

import com.example.shop.sales.application.domain.model.Order;
import com.example.shop.sales.application.domain.model.OrderId;
import com.example.shop.sales.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.application.port.in.OrderPlacement;
import com.example.shop.sales.application.port.out.ShipmentRequester;
import com.example.shop.sales.application.port.out.OrderStore;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

// No stereotype annotation: the bean is declared in the hexagon's configuration package.
// @Transactional is the one accepted exception to the purity of the domain. It goes on the methods,
// never on the class, so each method chooses its own transaction (read-only or not).
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
