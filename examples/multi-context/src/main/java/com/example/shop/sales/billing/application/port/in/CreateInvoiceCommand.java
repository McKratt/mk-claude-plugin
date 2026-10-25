package com.example.shop.sales.billing.application.port.in;

import com.example.shop.sales.common.Preconditions;
import java.util.UUID;

public record CreateInvoiceCommand(UUID orderId, int itemCount) {

    public CreateInvoiceCommand {
        Preconditions.requireNonNull(orderId, "orderId");
        Preconditions.requirePositive(itemCount, "itemCount");
    }
}
