package com.example.shop.application.port.out;

import com.example.shop.domain.Order;

/** Third-party service: asks the shipping company to deliver an order. */
public interface ShipmentRequester {

    void requestShipmentFor(Order order);
}
