package com.example.shop.application.port.out;

import com.example.shop.domain.Order;

public interface OrderStore {

    void save(Order order);
}
