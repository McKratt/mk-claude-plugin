package com.example.shop.sales.application.port.in;

import com.example.shop.sales.application.domain.model.OrderId;

public interface PlaceOrderUseCase {

    OrderId placeOrder(PlaceOrderCommand command);
}
