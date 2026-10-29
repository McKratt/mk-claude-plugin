package com.example.shop.order.adapter.in.web;

import com.example.shop.order.application.port.in.OrderPlacement;
import com.example.shop.order.application.port.in.PlaceOrderCommand;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** Web adapter: a Helidon {@link HttpService} that owns its request and response models (JSON via Jackson). */
@RequiredArgsConstructor
class PlaceOrderController implements HttpService {

    record Request(List<Line> lines) {

        record Line(String sku, int quantity) {
        }
    }

    record Response(UUID id) {
    }

    record Problem(String title, int status, String detail) {
    }

    private final OrderPlacement orderPlacement;

    @Override
    public void routing(HttpRules rules) {
        rules.post("/", this::place);
    }

    private void place(ServerRequest request, ServerResponse response) {
        var body = request.content().as(Request.class);
        var lines = body.lines() == null ? List.<Request.Line>of() : body.lines();
        try {
            var command = new PlaceOrderCommand(lines.stream()
                    .map(line -> new PlaceOrderCommand.Line(line.sku(), line.quantity()))
                    .toList());
            var id = orderPlacement.placeOrder(command).value();
            response.status(Status.CREATED_201).send(new Response(id));
        } catch (IllegalArgumentException e) {
            // Self-validating commands and domain rules throw IllegalArgumentException.
            response.status(Status.BAD_REQUEST_400)
                    .send(new Problem("Bad Request", Status.BAD_REQUEST_400.code(), e.getMessage()));
        }
    }
}
