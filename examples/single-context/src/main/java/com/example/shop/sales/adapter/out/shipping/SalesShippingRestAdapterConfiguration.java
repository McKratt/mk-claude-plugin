package com.example.shop.sales.adapter.out.shipping;

import com.example.shop.sales.application.port.out.ShipmentRequester;
import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class SalesShippingRestAdapterConfiguration {

    @Bean
    ShipmentRequester shipmentRequester(@Value("${shipping.base-url}") String baseUrl) {
        // HTTP/1.1 is pinned: the JDK client's default HTTP/2 upgrade attempt breaks POST requests
        // against plain-HTTP servers that do not support h2c.
        var httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        var restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        return new ShippingRestAdapter(restClient);
    }
}
