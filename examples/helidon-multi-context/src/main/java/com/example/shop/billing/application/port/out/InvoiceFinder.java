package com.example.shop.billing.application.port.out;

import com.example.shop.billing.domain.Invoice;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceFinder {

    Optional<Invoice> loadByOrderId(UUID orderId);
}
