package com.example.shop.billing.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoices")
class InvoiceJpaEntity {

    @Id
    UUID id;

    UUID orderId;
    int itemCount;
    Instant issuedAt;

    protected InvoiceJpaEntity() {
    }

    InvoiceJpaEntity(UUID id, UUID orderId, int itemCount, Instant issuedAt) {
        this.id = id;
        this.orderId = orderId;
        this.itemCount = itemCount;
        this.issuedAt = issuedAt;
    }
}
