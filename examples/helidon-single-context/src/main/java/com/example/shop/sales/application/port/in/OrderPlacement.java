package com.example.shop.sales.application.port.in;

import com.example.shop.sales.application.domain.model.OrderId;

public interface OrderPlacement {

    OrderId placeOrder(PlaceOrderCommand command);
}
