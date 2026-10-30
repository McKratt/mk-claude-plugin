package com.acme.shop.application.service;

import com.acme.shop.adapter.out.persistence.OrderPersistenceAdapter;
import com.acme.shop.application.port.in.PlaceOrderCommand;
import com.acme.shop.application.port.in.PlaceOrderUseCase;
import com.acme.shop.domain.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderPersistenceAdapter orderStore;

    public PlaceOrderService(OrderPersistenceAdapter orderStore) {
        this.orderStore = orderStore;
    }

    @Override
    @Transactional
    public UUID place(PlaceOrderCommand command) {
        Order order = Order.place(command.customerId());
        orderStore.save(order);
        return order.getId();
    }
}
