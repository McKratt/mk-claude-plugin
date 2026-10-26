package com.example.shop.sales.order.adapter.in.web;

import com.example.shop.sales.order.application.port.in.PlaceOrderUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Creates the web adapter beans so the adapter classes can stay package-private and stereotype-free. */
@Configuration(proxyBeanMethods = false)
class OrderWebAdapterConfiguration {

    @Bean
    PlaceOrderController placeOrderController(PlaceOrderUseCase placeOrderUseCase) {
        return new PlaceOrderController(placeOrderUseCase);
    }
}
