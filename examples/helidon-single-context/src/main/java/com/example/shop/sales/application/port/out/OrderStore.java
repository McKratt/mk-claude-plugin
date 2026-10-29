package com.example.shop.sales.application.port.out;

import com.example.shop.sales.domain.Order;

public interface OrderStore {

    void save(Order order);
}
