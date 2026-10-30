package com.acme.shop.adapter.in.web;

import com.acme.shop.application.port.in.OrderPlacement;
import com.acme.shop.application.port.in.PlaceOrderCommand;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

class OrderController implements HttpService {

    private final OrderPlacement placeOrder;

    OrderController(OrderPlacement placeOrder) {
        this.placeOrder = placeOrder;
    }

    @Override
    public void routing(HttpRules rules) {
        rules.post("/", this::place);
    }

    private void place(ServerRequest request, ServerResponse response) {
        var customerId = request.query().get("customerId");
        var productId = request.query().get("productId");
        response.send(placeOrder.place(new PlaceOrderCommand(customerId, productId)).toString());
    }
}
