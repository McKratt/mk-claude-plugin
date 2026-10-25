package com.example.shop.sales.billing.adapter.in.web;

import com.example.shop.sales.billing.application.port.in.FindInvoiceByOrderQuery;
import com.example.shop.sales.billing.application.port.in.FindInvoiceByOrderUseCase;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
