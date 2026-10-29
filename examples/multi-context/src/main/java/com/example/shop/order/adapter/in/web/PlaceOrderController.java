package com.example.shop.order.adapter.in.web;

import com.example.shop.order.application.port.in.PlaceOrderCommand;
import com.example.shop.order.application.port.in.OrderPlacement;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Spring MVC only detects handlers carrying @Controller / @RestController: the one stereotype allowed
// on an adapter. The controller is found by component scanning, not created in a configuration class.
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
class PlaceOrderController {

    record Request(List<Line> lines) {

        record Line(String sku, int quantity) {
        }
    }

    record Response(UUID id) {
    }

    private final OrderPlacement orderPlacement;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Response place(@RequestBody Request request) {
        var lines = request.lines() == null ? List.<Request.Line>of() : request.lines();
        var command = new PlaceOrderCommand(lines.stream()
                .map(line -> new PlaceOrderCommand.Line(line.sku(), line.quantity()))
                .toList());
        return new Response(orderPlacement.placeOrder(command).value());
    }

    /** Self-validating commands and domain rules throw IllegalArgumentException. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
