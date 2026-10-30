package com.acme.shop.application.port.in;

import java.util.UUID;

public interface PlaceOrderUseCase {

    UUID place(PlaceOrderCommand command);
}
