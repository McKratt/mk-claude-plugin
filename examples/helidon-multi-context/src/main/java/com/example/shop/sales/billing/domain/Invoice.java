package com.example.shop.sales.billing.domain;

import com.example.shop.sales.common.Preconditions;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Invoice {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID orderId;
    private final int itemCount;
    private final Instant issuedAt;

    public static Invoice issue(UUID orderId, int itemCount, Instant issuedAt) {
        return new Invoice(UUID.randomUUID(),
                Preconditions.requireNonNull(orderId, "orderId"),
                Preconditions.requirePositive(itemCount, "itemCount"),
                Preconditions.requireNonNull(issuedAt, "issuedAt"));
    }

    public static Invoice restore(UUID id, UUID orderId, int itemCount, Instant issuedAt) {
        return new Invoice(id, orderId, itemCount, issuedAt);
    }
}
