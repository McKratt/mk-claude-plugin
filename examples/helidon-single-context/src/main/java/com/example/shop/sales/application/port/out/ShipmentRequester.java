package com.example.shop.sales.application.port.out;

import com.example.shop.sales.application.domain.model.Order;

/** Third-party service: asks the shipping company to deliver an order. */
public interface ShipmentRequester {

    void requestShipmentFor(Order order);
}
