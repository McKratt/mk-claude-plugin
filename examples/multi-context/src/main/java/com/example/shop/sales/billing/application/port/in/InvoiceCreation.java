package com.example.shop.sales.billing.application.port.in;

public interface InvoiceCreation {

    void createInvoice(CreateInvoiceCommand command);
}
