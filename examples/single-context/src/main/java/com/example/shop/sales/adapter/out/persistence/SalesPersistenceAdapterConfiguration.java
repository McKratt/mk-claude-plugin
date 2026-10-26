package com.example.shop.sales.adapter.out.persistence;

import com.example.shop.sales.application.port.out.OrderStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the persistence adapter bean. Entities and Spring Data repositories are found by Spring Boot's
 * default scanning from the application package, so they can stay package-private too.
 */
@Configuration(proxyBeanMethods = false)
class SalesPersistenceAdapterConfiguration {

    @Bean
    OrderStore orderStore(SpringDataOrderRepository repository) {
        return new OrderPersistenceAdapter(repository);
    }
}
