package com.example.shop.billing.application.port.in;

import com.example.shop.billing.domain.Invoice;
import java.util.Optional;

public interface InvoiceLookup {

    Optional<Invoice> findByOrder(FindInvoiceByOrderQuery query);
}
