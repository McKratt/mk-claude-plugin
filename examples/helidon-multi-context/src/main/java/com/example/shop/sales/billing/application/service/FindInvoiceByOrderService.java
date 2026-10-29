package com.example.shop.sales.billing.application.service;

import com.example.shop.sales.billing.domain.Invoice;
import com.example.shop.sales.billing.application.port.in.FindInvoiceByOrderQuery;
import com.example.shop.sales.billing.application.port.in.InvoiceLookup;
import com.example.shop.sales.billing.application.port.out.InvoiceFinder;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// A single read: no transaction needed.
@RequiredArgsConstructor
public class FindInvoiceByOrderService implements InvoiceLookup {

    private final InvoiceFinder invoiceFinder;

    @Override
    public Optional<Invoice> findByOrder(FindInvoiceByOrderQuery query) {
        return invoiceFinder.loadByOrderId(query.orderId());
    }
}
