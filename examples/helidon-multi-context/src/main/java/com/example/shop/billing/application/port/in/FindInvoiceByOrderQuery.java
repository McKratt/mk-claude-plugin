package com.example.shop.billing.application.port.in;

import com.example.shop.common.Preconditions;
import java.util.UUID;

public record FindInvoiceByOrderQuery(UUID orderId) {

    public FindInvoiceByOrderQuery {
        Preconditions.requireNonNull(orderId, "orderId");
    }
}
