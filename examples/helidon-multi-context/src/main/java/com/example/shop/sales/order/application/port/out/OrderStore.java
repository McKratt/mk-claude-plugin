package com.example.shop.sales.order.application.port.out;

import com.example.shop.sales.order.application.domain.model.Order;

public interface OrderStore {

    void save(Order order);
}
