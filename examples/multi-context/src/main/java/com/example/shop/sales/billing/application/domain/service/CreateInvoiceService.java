package com.example.shop.sales.billing.application.domain.service;

import com.example.shop.sales.billing.application.domain.model.Invoice;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceUseCase;
import com.example.shop.sales.billing.application.port.out.SaveInvoicePort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
public class CreateInvoiceService implements CreateInvoiceUseCase {

    private final SaveInvoicePort saveInvoicePort;
    private final Clock clock;

    @Override
    public void createInvoice(CreateInvoiceCommand command) {
        saveInvoicePort.save(Invoice.issue(command.orderId(), command.itemCount(), clock.instant()));
    }
}
