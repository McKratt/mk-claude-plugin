package com.example.shop.billing.application.service;

import com.example.shop.billing.domain.Invoice;
import com.example.shop.billing.application.port.in.FindInvoiceByOrderQuery;
import com.example.shop.billing.application.port.in.InvoiceLookup;
import com.example.shop.billing.application.port.out.InvoiceFinder;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class FindInvoiceByOrderService implements InvoiceLookup {

    private final InvoiceFinder invoiceFinder;

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findByOrder(FindInvoiceByOrderQuery query) {
        return invoiceFinder.loadByOrderId(query.orderId());
    }
}
