package com.example.shop.sales.billing.application.port.in;

import com.example.shop.sales.billing.domain.Invoice;
import java.util.Optional;

public interface InvoiceLookup {

    Optional<Invoice> findByOrder(FindInvoiceByOrderQuery query);
}
