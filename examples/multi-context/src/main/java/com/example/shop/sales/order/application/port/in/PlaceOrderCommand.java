package com.example.shop.sales.order.application.port.in;

import com.example.shop.sales.common.Preconditions;
import java.util.List;

/** Input model of exactly one use case. It validates itself on construction. */
public record PlaceOrderCommand(List<Line> lines) {

    public PlaceOrderCommand {
        Preconditions.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
    }

    public record Line(String sku, int quantity) {

        public Line {
            Preconditions.requireNotBlank(sku, "sku");
            Preconditions.requirePositive(quantity, "quantity");
        }
    }
}
