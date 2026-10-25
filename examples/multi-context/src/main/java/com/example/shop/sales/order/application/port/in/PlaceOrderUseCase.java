package com.example.shop.sales.order.application.port.in;

import com.example.shop.sales.order.application.domain.model.OrderId;

public interface PlaceOrderUseCase {

    OrderId placeOrder(PlaceOrderCommand command);
}
