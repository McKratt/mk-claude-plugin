package com.example.shop.order.application.port.in;

import com.example.shop.order.domain.OrderId;

public interface OrderPlacement {

    OrderId placeOrder(PlaceOrderCommand command);
}
