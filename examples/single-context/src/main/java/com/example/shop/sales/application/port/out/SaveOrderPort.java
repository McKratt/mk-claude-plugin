package com.example.shop.sales.application.port.out;

import com.example.shop.sales.application.domain.model.Order;

public interface SaveOrderPort {

    void save(Order order);
}
