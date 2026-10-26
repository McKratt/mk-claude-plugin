package com.example.shop.sales.billing.adapter.in.web;

import com.example.shop.sales.billing.application.port.in.FindInvoiceByOrderQuery;
import com.example.shop.sales.billing.application.port.in.FindInvoiceByOrderUseCase;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Spring MVC only detects handlers carrying @Controller / @RestController: the one stereotype allowed
// on an adapter. The controller is found by component scanning, not created in a configuration class.
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
class InvoiceController {

    record Response(UUID id, UUID orderId, int itemCount, Instant issuedAt) {
    }

    private final FindInvoiceByOrderUseCase findInvoiceByOrderUseCase;

    @GetMapping("/by-order/{orderId}")
    ResponseEntity<Response> findByOrder(@PathVariable("orderId") UUID orderId) {
        return findInvoiceByOrderUseCase.findByOrder(new FindInvoiceByOrderQuery(orderId))
                .map(invoice -> new Response(invoice.getId(), invoice.getOrderId(),
                        invoice.getItemCount(), invoice.getIssuedAt()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Self-validating commands and domain rules throw IllegalArgumentException. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
