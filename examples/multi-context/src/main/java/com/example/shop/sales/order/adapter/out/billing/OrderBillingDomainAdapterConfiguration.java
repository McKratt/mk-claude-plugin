package com.example.shop.sales.order.adapter.out.billing;

import com.example.shop.sales.order.application.port.out.InvoiceIssuer;
import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OrderBillingDomainAdapterConfiguration {

    @Bean
    InvoiceIssuer invoiceIssuer(InvoiceCreation invoiceCreation) {
        return new BillingDomainAdapter(invoiceCreation);
    }
}
