package com.example.shop.sales.order.configuration;

import com.example.shop.sales.order.application.service.PlaceOrderService;
import com.example.shop.sales.order.application.port.in.OrderPlacement;
import com.example.shop.sales.order.application.port.out.InvoiceIssuer;
import com.example.shop.sales.order.application.port.out.OrderStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OrderConfiguration {

    @Bean
    OrderPlacement orderPlacement(OrderStore orderStore, InvoiceIssuer invoiceIssuer, Clock clock) {
        return new PlaceOrderService(orderStore, invoiceIssuer, clock);
    }
}
