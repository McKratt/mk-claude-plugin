package com.example.shop.sales.order.application.service;

import com.example.shop.sales.order.domain.Order;
import com.example.shop.sales.order.domain.OrderId;
import com.example.shop.sales.order.application.port.in.OrderPlacement;
import com.example.shop.sales.order.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.order.application.port.out.InvoiceIssuer;
import com.example.shop.sales.order.application.port.out.OrderStore;
import com.example.shop.sales.order.application.port.out.TransactionRunner;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

// Plain Java: no Helidon type, no annotation. The invoice is created by the billing context in the same
// transaction: billing's service joins the transaction opened here.
@RequiredArgsConstructor
public class PlaceOrderService implements OrderPlacement {

    private final OrderStore orderStore;
    private final InvoiceIssuer invoiceIssuer;
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
            invoiceIssuer.createInvoiceFor(order);
            return order.getId();
        });
    }
}
