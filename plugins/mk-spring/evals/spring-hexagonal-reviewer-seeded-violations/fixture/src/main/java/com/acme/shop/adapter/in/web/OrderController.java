package com.acme.shop.adapter.in.web;

import com.acme.shop.application.port.in.PlaceOrderCommand;
import com.acme.shop.application.port.in.PlaceOrderUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
class OrderController {

    private final PlaceOrderUseCase placeOrder;

    OrderController(PlaceOrderUseCase placeOrder) {
        this.placeOrder = placeOrder;
    }

    @PostMapping("/orders")
    UUID place(@RequestBody PlaceOrderRequest request) {
        return placeOrder.place(new PlaceOrderCommand(request.customerId()));
    }

    record PlaceOrderRequest(String customerId) {
    }
}
