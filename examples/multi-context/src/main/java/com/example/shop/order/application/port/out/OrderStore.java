package com.example.shop.order.application.port.out;

import com.example.shop.order.domain.Order;

public interface OrderStore {

    void save(Order order);
}
