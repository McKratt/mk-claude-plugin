package com.example.shop.sales.order.application.domain.service;

import com.example.shop.sales.order.application.domain.model.Order;
import com.example.shop.sales.order.application.domain.model.OrderId;
import com.example.shop.sales.order.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.order.application.port.in.PlaceOrderUseCase;
import com.example.shop.sales.order.application.port.out.CreateInvoicePort;
import com.example.shop.sales.order.application.port.out.SaveOrderPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

// No stereotype annotation: the bean is declared in the hexagon's configuration package.
// @Transactional is the one accepted exception to the purity of the domain.
@Transactional
@RequiredArgsConstructor
public class PlaceOrderService implements PlaceOrderUseCase {

    private final SaveOrderPort saveOrderPort;
    private final CreateInvoicePort createInvoicePort;
    private final Clock clock;

    @Override
    public OrderId placeOrder(PlaceOrderCommand command) {
        var lines = command.lines().stream()
                .map(line -> new Order.Line(line.sku(), line.quantity()))
                .toList();
        var order = Order.place(lines, clock.instant());
        saveOrderPort.save(order);
        createInvoicePort.createInvoiceFor(order);
        return order.getId();
    }
}
