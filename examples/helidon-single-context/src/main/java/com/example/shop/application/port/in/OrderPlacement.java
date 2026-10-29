package com.example.shop.application.port.in;

import com.example.shop.domain.OrderId;

public interface OrderPlacement {

    OrderId placeOrder(PlaceOrderCommand command);
}
