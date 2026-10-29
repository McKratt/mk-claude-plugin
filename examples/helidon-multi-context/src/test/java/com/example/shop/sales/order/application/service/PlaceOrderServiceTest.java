package com.example.shop.sales.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;

import com.example.shop.sales.order.domain.Order;
import com.example.shop.sales.order.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.order.application.port.out.InvoiceIssuer;
import com.example.shop.sales.order.application.port.out.OrderStore;
import com.example.shop.sales.order.application.port.out.TransactionRunner;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlaceOrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    /** Runs the work directly: the real transaction is covered by OrderToInvoiceIT. */
    private static final TransactionRunner NO_TRANSACTION = new TransactionRunner() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    private final OrderStore orderStore = mock(OrderStore.class);
    private final InvoiceIssuer invoiceIssuer = mock(InvoiceIssuer.class);
    private final PlaceOrderService service = new PlaceOrderService(orderStore, invoiceIssuer, NO_TRANSACTION, CLOCK);

    @Test
    void should_save_the_order_and_ask_for_its_invoice_when_it_is_placed() {
        var orderId = service.placeOrder(
                new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 2))));

        var captor = ArgumentCaptor.forClass(Order.class);
        then(orderStore).should().save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(orderId);
        assertThat(captor.getValue().getPlacedAt()).isEqualTo(NOW);
        then(invoiceIssuer).should().createInvoiceFor(captor.getValue());
    }
}
