package com.example.shop.sales.application.port.in;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceOrderCommandTest {

    @Test
    void should_reject_the_line_when_sku_is_blank() {
        assertThatThrownBy(() -> new PlaceOrderCommand.Line(" ", 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_the_command_when_lines_are_null() {
        assertThatThrownBy(() -> new PlaceOrderCommand(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_accept_the_command_when_lines_are_valid() {
        new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 1)));
    }
}
