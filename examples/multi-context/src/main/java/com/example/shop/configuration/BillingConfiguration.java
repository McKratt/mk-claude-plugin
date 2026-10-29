package com.example.shop.configuration;

import com.example.shop.billing.application.service.CreateInvoiceService;
import com.example.shop.billing.application.service.FindInvoiceByOrderService;
import com.example.shop.billing.application.port.in.InvoiceCreation;
import com.example.shop.billing.application.port.in.InvoiceLookup;
import com.example.shop.billing.application.port.out.InvoiceFinder;
import com.example.shop.billing.application.port.out.InvoiceStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BillingConfiguration {

    @Bean
    InvoiceCreation invoiceCreation(InvoiceStore invoiceStore, Clock clock) {
        return new CreateInvoiceService(invoiceStore, clock);
    }

    @Bean
    InvoiceLookup invoiceLookup(InvoiceFinder invoiceFinder) {
        return new FindInvoiceByOrderService(invoiceFinder);
    }
}
