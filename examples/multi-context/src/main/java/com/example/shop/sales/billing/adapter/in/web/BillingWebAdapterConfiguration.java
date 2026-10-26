package com.example.shop.sales.billing.adapter.in.web;

import com.example.shop.sales.billing.application.port.in.FindInvoiceByOrderUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class BillingWebAdapterConfiguration {

    @Bean
    InvoiceController invoiceController(FindInvoiceByOrderUseCase findInvoiceByOrderUseCase) {
        return new InvoiceController(findInvoiceByOrderUseCase);
    }
}
