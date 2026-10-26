package com.example.shop.sales.billing.application.port.out;

import com.example.shop.sales.billing.application.domain.model.Invoice;

public interface InvoiceStore {

    void save(Invoice invoice);
}
