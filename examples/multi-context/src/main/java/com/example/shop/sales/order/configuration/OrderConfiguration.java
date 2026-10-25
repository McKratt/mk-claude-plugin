package com.example.shop.sales.order.configuration;

import com.example.shop.sales.order.application.domain.service.PlaceOrderService;
import com.example.shop.sales.order.application.port.in.PlaceOrderUseCase;
import com.example.shop.sales.order.application.port.out.CreateInvoicePort;
import com.example.shop.sales.order.application.port.out.SaveOrderPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OrderConfiguration {

    @Bean
    PlaceOrderUseCase placeOrderUseCase(SaveOrderPort saveOrderPort, CreateInvoicePort createInvoicePort, Clock clock) {
        return new PlaceOrderService(saveOrderPort, createInvoicePort, clock);
    }
}
