package com.example.shop.sales.order.application.port.out;

import com.example.shop.sales.order.application.domain.model.Order;

public interface InvoiceIssuer {

    void createInvoiceFor(Order order);
}
