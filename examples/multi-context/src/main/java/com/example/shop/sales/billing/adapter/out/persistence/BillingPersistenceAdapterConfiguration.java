package com.example.shop.sales.billing.adapter.out.persistence;

import com.example.shop.sales.billing.application.port.out.LoadInvoicePort;
import com.example.shop.sales.billing.application.port.out.SaveInvoicePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class BillingPersistenceAdapterConfiguration {

    @Bean
    InvoicePersistenceAdapter invoicePersistenceAdapter(SpringDataInvoiceRepository repository) {
        return new InvoicePersistenceAdapter(repository);
    }
}
