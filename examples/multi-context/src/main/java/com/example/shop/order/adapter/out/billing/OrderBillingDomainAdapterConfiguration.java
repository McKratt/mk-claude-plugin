package com.example.shop.order.adapter.out.billing;

import com.example.shop.order.application.port.out.InvoiceIssuer;
import com.example.shop.billing.application.port.in.InvoiceCreation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OrderBillingDomainAdapterConfiguration {

    @Bean
    InvoiceIssuer invoiceIssuer(InvoiceCreation invoiceCreation) {
        return new BillingDomainAdapter(invoiceCreation);
    }
}
