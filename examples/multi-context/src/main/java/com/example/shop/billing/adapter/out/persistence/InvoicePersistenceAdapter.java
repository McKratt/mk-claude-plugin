package com.example.shop.billing.adapter.out.persistence;

import com.example.shop.billing.domain.Invoice;
import com.example.shop.billing.application.port.out.InvoiceFinder;
import com.example.shop.billing.application.port.out.InvoiceStore;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class InvoicePersistenceAdapter implements InvoiceStore, InvoiceFinder {

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
