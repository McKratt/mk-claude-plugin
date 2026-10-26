package com.example.shop.sales.order.adapter.out.billing;

import com.example.shop.sales.order.application.port.out.CreateInvoicePort;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OrderBillingDomainAdapterConfiguration {

    @Bean
    CreateInvoicePort createInvoicePort(CreateInvoiceUseCase createInvoiceUseCase) {
        return new BillingDomainAdapter(createInvoiceUseCase);
    }
}
