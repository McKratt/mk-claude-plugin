package com.example.shop.billing.application.port.in;

public interface InvoiceCreation {

    void createInvoice(CreateInvoiceCommand command);
}
