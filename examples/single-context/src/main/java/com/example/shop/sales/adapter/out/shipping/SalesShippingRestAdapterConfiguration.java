package com.example.shop.sales.adapter.out.shipping;

import com.example.shop.sales.application.port.out.ShipmentRequester;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class SalesShippingRestAdapterConfiguration {

    @Bean
    ShipmentRequester shipmentRequester(@Value("${shipping.base-url}") String baseUrl) {
        return new ShippingRestAdapter(RestClient.builder().baseUrl(baseUrl).build());
    }
}
