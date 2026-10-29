package com.example.shop.configuration;

import com.example.shop.application.service.PlaceOrderService;
import com.example.shop.application.port.in.OrderPlacement;
import com.example.shop.application.port.out.ShipmentRequester;
import com.example.shop.application.port.out.OrderStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SalesConfiguration {

    @Bean
    OrderPlacement orderPlacement(OrderStore orderStore, ShipmentRequester shipmentRequester, Clock clock) {
        return new PlaceOrderService(orderStore, shipmentRequester, clock);
    }
}
