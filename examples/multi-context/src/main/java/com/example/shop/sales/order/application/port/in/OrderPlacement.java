package com.example.shop.sales.order.application.port.in;

import com.example.shop.sales.order.domain.OrderId;

public interface OrderPlacement {

    OrderId placeOrder(PlaceOrderCommand command);
}
