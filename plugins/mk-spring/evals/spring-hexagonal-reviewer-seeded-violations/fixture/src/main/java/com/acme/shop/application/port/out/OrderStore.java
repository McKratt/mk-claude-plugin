package com.acme.shop.application.port.out;

import com.acme.shop.domain.Order;

public interface OrderStore {

    void save(Order order);
}
