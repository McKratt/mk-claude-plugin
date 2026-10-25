package com.example.shop.sales.billing.application.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.shop.sales.billing.application.domain.model.Invoice;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.out.SaveInvoicePort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateInvoiceServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final SaveInvoicePort saveInvoicePort = mock(SaveInvoicePort.class);
    private final CreateInvoiceService service =
            new CreateInvoiceService(saveInvoicePort, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void issuesAndSavesTheInvoice() {
        var orderId = UUID.randomUUID();

        service.createInvoice(new CreateInvoiceCommand(orderId, 3));

        var captor = ArgumentCaptor.forClass(Invoice.class);
        verify(saveInvoicePort).save(captor.capture());
        assertEquals(orderId, captor.getValue().getOrderId());
        assertEquals(3, captor.getValue().getItemCount());
        assertEquals(NOW, captor.getValue().getIssuedAt());
    }
}
