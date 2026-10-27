package com.example.shop.sales.order.adapter.out.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;

import com.example.shop.sales.order.application.domain.model.Order;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BillingDomainAdapterTest {

    private final InvoiceCreation invoiceCreation = mock(InvoiceCreation.class);
    private final BillingDomainAdapter adapter = new BillingDomainAdapter(invoiceCreation);

    @Test
    void should_convert_the_order_into_a_billing_command_when_an_invoice_is_created() {
        var order = Order.place(
                List.of(new Order.Line("SKU-1", 2), new Order.Line("SKU-2", 1)),
                Instant.parse("2026-01-01T10:00:00Z"));

        adapter.createInvoiceFor(order);

        var captor = ArgumentCaptor.forClass(CreateInvoiceCommand.class);
        then(invoiceCreation).should().createInvoice(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(order.getId().value());
        assertThat(captor.getValue().itemCount()).isEqualTo(3);
    }
}
