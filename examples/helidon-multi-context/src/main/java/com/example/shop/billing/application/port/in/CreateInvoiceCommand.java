package com.example.shop.billing.application.port.in;

import com.example.shop.common.Preconditions;
import java.util.UUID;

public record CreateInvoiceCommand(UUID orderId, int itemCount) {

    public CreateInvoiceCommand {
        Preconditions.requireNonNull(orderId, "orderId");
        Preconditions.requirePositive(itemCount, "itemCount");
    }
}
