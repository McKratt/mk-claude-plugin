package com.example.shop.billing.application.service;

import com.example.shop.billing.domain.Invoice;
import com.example.shop.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.billing.application.port.in.InvoiceCreation;
import com.example.shop.billing.application.port.out.InvoiceStore;
import com.example.shop.billing.application.port.out.TransactionRunner;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateInvoiceService implements InvoiceCreation {

    private final InvoiceStore invoiceStore;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    @Override
    public void createInvoice(CreateInvoiceCommand command) {
        transactionRunner.inTransaction(() -> {
            invoiceStore.save(Invoice.issue(command.orderId(), command.itemCount(), clock.instant()));
            return null;
        });
    }
}
