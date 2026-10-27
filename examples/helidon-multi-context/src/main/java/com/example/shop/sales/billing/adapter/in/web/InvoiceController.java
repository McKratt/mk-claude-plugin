package com.example.shop.sales.billing.adapter.in.web;

import com.example.shop.sales.billing.application.port.in.FindInvoiceByOrderQuery;
import com.example.shop.sales.billing.application.port.in.InvoiceLookup;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** Web adapter of the billing context: reads invoices through billing's incoming port only. */
@RequiredArgsConstructor
class InvoiceController implements HttpService {

    record Response(UUID id, UUID orderId, int itemCount, String issuedAt) {
    }

    record Problem(String title, int status, String detail) {
    }

    private final InvoiceLookup invoiceLookup;

    @Override
    public void routing(HttpRules rules) {
        rules.get("/by-order/{orderId}", this::findByOrder);
    }

    private void findByOrder(ServerRequest request, ServerResponse response) {
        UUID orderId;
        try {
            orderId = UUID.fromString(request.path().pathParameters().get("orderId"));
        } catch (IllegalArgumentException e) {
            response.status(Status.BAD_REQUEST_400)
                    .send(new Problem("Bad Request", Status.BAD_REQUEST_400.code(), e.getMessage()));
            return;
        }
        invoiceLookup.findByOrder(new FindInvoiceByOrderQuery(orderId))
                .map(invoice -> new Response(invoice.getId(), invoice.getOrderId(),
                        invoice.getItemCount(), invoice.getIssuedAt().toString()))
                .ifPresentOrElse(response::send,
                        () -> response.status(Status.NOT_FOUND_404).send());
    }
}
