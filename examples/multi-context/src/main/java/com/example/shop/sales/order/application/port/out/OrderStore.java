package com.example.shop.sales.order.application.port.out;

import com.example.shop.sales.order.domain.Order;

public interface OrderStore {

    void save(Order order);
}
