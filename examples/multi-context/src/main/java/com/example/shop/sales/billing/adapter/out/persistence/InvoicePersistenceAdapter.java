package com.example.shop.sales.billing.adapter.out.persistence;

import com.example.shop.sales.billing.application.domain.model.Invoice;
import com.example.shop.sales.billing.application.port.out.LoadInvoicePort;
import com.example.shop.sales.billing.application.port.out.SaveInvoicePort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class InvoicePersistenceAdapter implements SaveInvoicePort, LoadInvoicePort {

    private final SpringDataInvoiceRepository repository;

    @Override
    public void save(Invoice invoice) {
        repository.save(new InvoiceJpaEntity(invoice.getId(), invoice.getOrderId(),
                invoice.getItemCount(), invoice.getIssuedAt()));
    }

    @Override
    public Optional<Invoice> loadByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId)
                .map(entity -> Invoice.restore(entity.id, entity.orderId, entity.itemCount, entity.issuedAt));
    }
}
