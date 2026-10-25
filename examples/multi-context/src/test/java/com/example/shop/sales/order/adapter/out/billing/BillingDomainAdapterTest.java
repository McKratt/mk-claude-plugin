package com.example.shop.sales.order.adapter.out.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.shop.sales.order.application.domain.model.Order;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceUseCase;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BillingDomainAdapterTest {

    private final CreateInvoiceUseCase createInvoiceUseCase = mock(CreateInvoiceUseCase.class);
    private final BillingDomainAdapter adapter = new BillingDomainAdapter(createInvoiceUseCase);

    @Test
    void convertsTheOrderIntoABillingCommand() {
        var order = Order.place(
                List.of(new Order.Line("SKU-1", 2), new Order.Line("SKU-2", 1)),
                Instant.parse("2026-01-01T10:00:00Z"));

        adapter.createInvoiceFor(order);

        var captor = ArgumentCaptor.forClass(CreateInvoiceCommand.class);
        verify(createInvoiceUseCase).createInvoice(captor.capture());
        assertEquals(order.getId().value(), captor.getValue().orderId());
        assertEquals(3, captor.getValue().itemCount());
    }
}
