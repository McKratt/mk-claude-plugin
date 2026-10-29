package com.example.shop.billing.application.port.out;

import com.example.shop.billing.domain.Invoice;

public interface InvoiceStore {

    void save(Invoice invoice);
}
