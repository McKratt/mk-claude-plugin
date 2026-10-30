package com.acme.shop.adapter.out.persistence;

import com.acme.shop.application.port.out.OrderStore;
import com.acme.shop.domain.Order;

public class OrderPersistenceAdapter implements OrderStore {

    private final SpringDataOrderRepository repository;

    public OrderPersistenceAdapter(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Order order) {
        repository.save(order);
    }
}
