package com.example.shop.sales.order.application.port.out;

import com.example.shop.sales.order.application.domain.model.Order;

public interface SaveOrderPort {

    void save(Order order);
}
