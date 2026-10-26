package com.example.shop.sales.configuration;

import com.example.shop.sales.application.domain.service.PlaceOrderService;
import com.example.shop.sales.application.port.in.OrderPlacement;
import com.example.shop.sales.application.port.out.OrderStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SalesConfiguration {

    @Bean
    OrderPlacement orderPlacement(OrderStore orderStore, Clock clock) {
        return new PlaceOrderService(orderStore, clock);
    }
}
