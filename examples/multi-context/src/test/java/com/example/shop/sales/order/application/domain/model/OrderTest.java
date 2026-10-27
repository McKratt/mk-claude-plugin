package com.example.shop.sales.order.application.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void should_place_an_order_with_its_lines_when_lines_are_valid() {
        var order = Order.place(List.of(new Order.Line("SKU-1", 2)), NOW);

        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getPlacedAt()).isEqualTo(NOW);
    }

    @Test
    void should_reject_the_order_when_it_has_no_lines() {
        assertThatThrownBy(() -> Order.place(List.of(), NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_the_line_when_quantity_is_not_positive() {
        assertThatThrownBy(() -> new Order.Line("SKU-1", 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
