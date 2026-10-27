package com.example.shop.sales.order.application.port.in;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceOrderCommandTest {

    @Test
    void should_reject_the_line_when_sku_is_blank() {
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand.Line(" ", 1));
    }

    @Test
    void should_reject_the_command_when_lines_are_null() {
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand(null));
    }

    @Test
    void should_accept_the_command_when_lines_are_valid() {
        new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 1)));
    }
}
