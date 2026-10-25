package com.example.shop.sales.order.adapter.out.persistence;

import com.example.shop.sales.order.application.domain.model.Order;
import com.example.shop.sales.order.application.port.out.SaveOrderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OrderPersistenceAdapter implements SaveOrderPort {

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
