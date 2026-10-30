package com.acme.shop.application.port.in;

import java.util.UUID;

public interface OrderPlacement {

    UUID place(PlaceOrderCommand command);
}
