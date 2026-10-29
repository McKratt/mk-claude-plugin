package com.example.shop.billing.adapter.out.persistence;

import com.example.shop.billing.application.port.out.InvoiceFinder;
import com.example.shop.billing.application.port.out.InvoiceStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class BillingPersistenceAdapterConfiguration {

    @Bean
    InvoicePersistenceAdapter invoicePersistenceAdapter(SpringDataInvoiceRepository repository) {
        return new InvoicePersistenceAdapter(repository);
    }
}
