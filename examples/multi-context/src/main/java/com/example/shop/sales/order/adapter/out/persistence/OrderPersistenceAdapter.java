package com.example.shop.sales.order.adapter.out.persistence;

import com.example.shop.sales.order.domain.Order;
import com.example.shop.sales.order.application.port.out.OrderStore;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class OrderPersistenceAdapter implements OrderStore {

    private final SpringDataOrderRepository repository;

    @Override
    public void save(Order order) {
        repository.save(toEntity(order));
    }

    private static OrderJpaEntity toEntity(Order order) {
        var lines = order.getLines().stream()
                .map(line -> new OrderLineEmbeddable(line.sku(), line.quantity()))
                .toList();
        return new OrderJpaEntity(order.getId().value(), order.getPlacedAt(), lines);
    }
}
