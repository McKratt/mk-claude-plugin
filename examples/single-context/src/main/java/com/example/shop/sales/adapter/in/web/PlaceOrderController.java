package com.example.shop.sales.adapter.in.web;

import com.example.shop.sales.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.application.port.in.PlaceOrderUseCase;
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
import org.springframework.web.bind.annotation.ResponseBody;

// No @RestController: the bean is declared in the adapter's configuration class.
// A type-level @RequestMapping is enough for Spring MVC to detect the handler.
@ResponseBody
@RequestMapping("/orders")
@RequiredArgsConstructor
class PlaceOrderController {

    record Request(List<Line> lines) {

        record Line(String sku, int quantity) {
        }
    }

    record Response(UUID id) {
    }

    private final PlaceOrderUseCase placeOrderUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Response place(@RequestBody Request request) {
        var lines = request.lines() == null ? List.<Request.Line>of() : request.lines();
        var command = new PlaceOrderCommand(lines.stream()
                .map(line -> new PlaceOrderCommand.Line(line.sku(), line.quantity()))
                .toList());
        return new Response(placeOrderUseCase.placeOrder(command).value());
    }

    /** Self-validating commands and domain rules throw IllegalArgumentException. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
