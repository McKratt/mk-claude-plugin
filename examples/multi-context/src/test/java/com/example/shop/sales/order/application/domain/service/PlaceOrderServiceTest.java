package com.example.shop.sales.order.application.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.shop.sales.order.application.domain.model.Order;
import com.example.shop.sales.order.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.order.application.port.out.InvoiceIssuer;
import com.example.shop.sales.order.application.port.out.OrderStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlaceOrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final OrderStore orderStore = mock(OrderStore.class);
    private final InvoiceIssuer invoiceIssuer = mock(InvoiceIssuer.class);
    private final PlaceOrderService service = new PlaceOrderService(orderStore, invoiceIssuer, CLOCK);

    @Test
    void should_save_the_order_with_the_current_instant_when_it_is_placed() {
        var orderId = service.placeOrder(
                new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 2))));

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(orderStore).save(captor.capture());
        assertEquals(orderId, captor.getValue().getId());
        assertEquals(NOW, captor.getValue().getPlacedAt());
    }

    @Test
    void should_ask_for_an_invoice_when_the_order_is_placed() {
        service.placeOrder(new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 1))));

        verify(invoiceIssuer).createInvoiceFor(any(Order.class));
    }
}
