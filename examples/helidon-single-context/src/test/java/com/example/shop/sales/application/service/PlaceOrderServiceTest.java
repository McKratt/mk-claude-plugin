package com.example.shop.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;

import com.example.shop.sales.domain.Order;
import com.example.shop.sales.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.application.port.out.OrderStore;
import com.example.shop.sales.application.port.out.ShipmentRequester;
import com.example.shop.sales.application.port.out.TransactionRunner;
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

    /** Runs the work directly: the real transaction is covered by PlaceOrderIT. */
    private static final TransactionRunner NO_TRANSACTION = new TransactionRunner() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    private final OrderStore orderStore = mock(OrderStore.class);
    private final ShipmentRequester shipmentRequester = mock(ShipmentRequester.class);
    private final PlaceOrderService service =
            new PlaceOrderService(orderStore, shipmentRequester, NO_TRANSACTION, CLOCK);

    @Test
    void should_save_the_order_with_the_current_instant_when_it_is_placed() {
        var orderId = service.placeOrder(
                new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 2))));

        var captor = ArgumentCaptor.forClass(Order.class);
        then(orderStore).should().save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(orderId);
        assertThat(captor.getValue().getPlacedAt()).isEqualTo(NOW);
    }

    @Test
    void should_request_a_shipment_when_the_order_is_placed() {
        service.placeOrder(new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 1))));

        then(shipmentRequester).should().requestShipmentFor(any(Order.class));
    }
}
