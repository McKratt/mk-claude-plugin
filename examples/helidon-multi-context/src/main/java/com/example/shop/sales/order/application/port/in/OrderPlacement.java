package com.example.shop.sales.order.application.port.in;

import com.example.shop.sales.order.application.domain.model.OrderId;

public interface OrderPlacement {

    OrderId placeOrder(PlaceOrderCommand command);
}
