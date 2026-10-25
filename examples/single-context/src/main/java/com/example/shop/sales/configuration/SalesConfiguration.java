package com.example.shop.sales.configuration;

import com.example.shop.sales.application.domain.service.PlaceOrderService;
import com.example.shop.sales.application.port.in.PlaceOrderUseCase;
import com.example.shop.sales.application.port.out.SaveOrderPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SalesConfiguration {

    @Bean
    PlaceOrderUseCase placeOrderUseCase(SaveOrderPort saveOrderPort, Clock clock) {
        return new PlaceOrderService(saveOrderPort, clock);
    }
}
