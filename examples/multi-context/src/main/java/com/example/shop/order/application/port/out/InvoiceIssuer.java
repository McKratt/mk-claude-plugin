package com.example.shop.order.application.port.out;

import com.example.shop.order.domain.Order;

public interface InvoiceIssuer {

    void createInvoiceFor(Order order);
}
