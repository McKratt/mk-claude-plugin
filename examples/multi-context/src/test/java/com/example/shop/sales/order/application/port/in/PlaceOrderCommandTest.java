package com.example.shop.sales.order.application.port.in;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceOrderCommandTest {

    @Test
    void rejectsABlankSku() {
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand.Line(" ", 1));
    }

    @Test
    void rejectsNullLines() {
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand(null));
    }

    @Test
    void acceptsValidLines() {
        new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 1)));
    }
}
