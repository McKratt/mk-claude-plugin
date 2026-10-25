package com.example.shop.sales.order.adapter.out.billing;

import com.example.shop.sales.order.application.domain.model.Order;
import com.example.shop.sales.order.application.port.out.CreateInvoicePort;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Joins the order context to the billing context of the same component: it implements order's outgoing
 * port and calls billing's incoming port directly, converting order types into billing's command.
 */
@Component
@RequiredArgsConstructor
class BillingDomainAdapter implements CreateInvoicePort {

    private final CreateInvoiceUseCase createInvoiceUseCase;

    @Override
    public void createInvoiceFor(Order order) {
        int itemCount = order.getLines().stream().mapToInt(Order.Line::quantity).sum();
        createInvoiceUseCase.createInvoice(new CreateInvoiceCommand(order.getId().value(), itemCount));
    }
}
