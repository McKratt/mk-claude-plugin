package com.acme.shop.adapter.out.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderPersistenceAdapterConfiguration {

    @Bean
    OrderPersistenceAdapter orderPersistenceAdapter(SpringDataOrderRepository repository) {
        return new OrderPersistenceAdapter(repository);
    }
}
