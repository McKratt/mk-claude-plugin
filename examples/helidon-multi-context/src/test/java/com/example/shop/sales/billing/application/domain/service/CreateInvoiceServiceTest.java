package com.example.shop.sales.billing.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;

import com.example.shop.sales.billing.application.domain.model.Invoice;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.out.InvoiceStore;
import com.example.shop.sales.billing.application.port.out.TransactionRunner;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateInvoiceServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static final TransactionRunner NO_TRANSACTION = new TransactionRunner() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    private final InvoiceStore invoiceStore = mock(InvoiceStore.class);
    private final CreateInvoiceService service =
            new CreateInvoiceService(invoiceStore, NO_TRANSACTION, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void should_issue_and_save_the_invoice_when_a_command_is_received() {
        var orderId = UUID.randomUUID();

        service.createInvoice(new CreateInvoiceCommand(orderId, 3));

        var captor = ArgumentCaptor.forClass(Invoice.class);
        then(invoiceStore).should().save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getItemCount()).isEqualTo(3);
        assertThat(captor.getValue().getIssuedAt()).isEqualTo(NOW);
    }
}
