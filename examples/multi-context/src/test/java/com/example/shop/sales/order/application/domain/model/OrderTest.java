package com.example.shop.sales.order.application.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void placesAnOrderWithItsLines() {
        var order = Order.place(List.of(new Order.Line("SKU-1", 2)), NOW);

        assertEquals(1, order.getLines().size());
        assertEquals(NOW, order.getPlacedAt());
    }

    @Test
    void rejectsAnOrderWithoutLines() {
        assertThrows(IllegalArgumentException.class, () -> Order.place(List.of(), NOW));
    }

    @Test
    void rejectsANonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new Order.Line("SKU-1", 0));
    }
}
