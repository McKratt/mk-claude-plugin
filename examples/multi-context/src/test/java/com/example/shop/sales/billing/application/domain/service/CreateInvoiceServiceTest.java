package com.example.shop.sales.billing.application.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.shop.sales.billing.application.domain.model.Invoice;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.out.InvoiceStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateInvoiceServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final InvoiceStore invoiceStore = mock(InvoiceStore.class);
    private final CreateInvoiceService service =
            new CreateInvoiceService(invoiceStore, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void should_issue_and_save_the_invoice_when_a_command_is_received() {
        var orderId = UUID.randomUUID();

        service.createInvoice(new CreateInvoiceCommand(orderId, 3));

        var captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceStore).save(captor.capture());
        assertEquals(orderId, captor.getValue().getOrderId());
        assertEquals(3, captor.getValue().getItemCount());
        assertEquals(NOW, captor.getValue().getIssuedAt());
    }
}
