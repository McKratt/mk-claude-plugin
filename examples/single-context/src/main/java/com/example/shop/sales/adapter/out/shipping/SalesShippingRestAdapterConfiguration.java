package com.example.shop.sales.adapter.out.shipping;

import com.example.shop.sales.application.port.out.ShipmentRequester;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class SalesShippingRestAdapterConfiguration {

    @Bean
    ShipmentRequester shipmentRequester(RestClient.Builder restClientBuilder,
            @Value("${shipping.base-url}") String baseUrl) {
        // Always start from the auto-configured builder: it carries Spring Boot's message converters,
        // request factory and global settings (spring.http.clients.*). Never RestClient.builder() or
        // a raw java.net.http.HttpClient.
        return new ShippingRestAdapter(restClientBuilder.baseUrl(baseUrl).build());
    }
}
