package com.example.shop.sales.billing.application.port.in;

import com.example.shop.sales.common.Preconditions;
import java.util.UUID;

public record FindInvoiceByOrderQuery(UUID orderId) {

    public FindInvoiceByOrderQuery {
        Preconditions.requireNonNull(orderId, "orderId");
    }
}
