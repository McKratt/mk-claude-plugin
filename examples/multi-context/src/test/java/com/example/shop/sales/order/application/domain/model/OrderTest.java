package com.example.shop.sales.order.application.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void should_place_an_order_with_its_lines_when_lines_are_valid() {
        var order = Order.place(List.of(new Order.Line("SKU-1", 2)), NOW);

        assertEquals(1, order.getLines().size());
        assertEquals(NOW, order.getPlacedAt());
    }

    @Test
    void should_reject_the_order_when_it_has_no_lines() {
        assertThrows(IllegalArgumentException.class, () -> Order.place(List.of(), NOW));
    }

    @Test
    void should_reject_the_line_when_quantity_is_not_positive() {
        assertThrows(IllegalArgumentException.class, () -> new Order.Line("SKU-1", 0));
    }
}
