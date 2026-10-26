package com.example.shop.sales.billing.application.domain.service;

import com.example.shop.sales.billing.application.domain.model.Invoice;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import com.example.shop.sales.billing.application.port.out.InvoiceStore;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class CreateInvoiceService implements InvoiceCreation {

    private final InvoiceStore invoiceStore;
    private final Clock clock;

    @Override
    @Transactional
    public void createInvoice(CreateInvoiceCommand command) {
        invoiceStore.save(Invoice.issue(command.orderId(), command.itemCount(), clock.instant()));
    }
}
