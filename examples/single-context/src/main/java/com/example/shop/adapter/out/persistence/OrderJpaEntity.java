package com.example.shop.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
class OrderJpaEntity {

    @Id
    UUID id;

    Instant placedAt;

    @ElementCollection
    @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
    List<OrderLineEmbeddable> lines;

    protected OrderJpaEntity() {
    }

    OrderJpaEntity(UUID id, Instant placedAt, List<OrderLineEmbeddable> lines) {
        this.id = id;
        this.placedAt = placedAt;
        this.lines = lines;
    }
}
