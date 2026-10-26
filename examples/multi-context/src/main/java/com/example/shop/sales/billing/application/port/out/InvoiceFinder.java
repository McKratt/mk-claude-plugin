package com.example.shop.sales.billing.application.port.out;

import com.example.shop.sales.billing.application.domain.model.Invoice;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceFinder {

    Optional<Invoice> loadByOrderId(UUID orderId);
}
