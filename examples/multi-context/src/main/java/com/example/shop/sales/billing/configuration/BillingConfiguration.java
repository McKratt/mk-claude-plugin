package com.example.shop.sales.billing.configuration;

import com.example.shop.sales.billing.application.domain.service.CreateInvoiceService;
import com.example.shop.sales.billing.application.domain.service.FindInvoiceByOrderService;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceUseCase;
import com.example.shop.sales.billing.application.port.in.FindInvoiceByOrderUseCase;
import com.example.shop.sales.billing.application.port.out.LoadInvoicePort;
import com.example.shop.sales.billing.application.port.out.SaveInvoicePort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BillingConfiguration {

    @Bean
    CreateInvoiceUseCase createInvoiceUseCase(SaveInvoicePort saveInvoicePort, Clock clock) {
        return new CreateInvoiceService(saveInvoicePort, clock);
    }

    @Bean
    FindInvoiceByOrderUseCase findInvoiceByOrderUseCase(LoadInvoicePort loadInvoicePort) {
        return new FindInvoiceByOrderService(loadInvoicePort);
    }
}
