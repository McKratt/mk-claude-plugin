package com.example.shop.order.domain;

import com.example.shop.common.Preconditions;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Order {

    public record Line(String sku, int quantity) {

        public Line {
            Preconditions.requireNotBlank(sku, "sku");
            Preconditions.requirePositive(quantity, "quantity");
        }
    }

    @EqualsAndHashCode.Include
    private final OrderId id;
    private final List<Line> lines;
    private final Instant placedAt;

    /** The business rule lives here, not in the command. */
    public static Order place(List<Line> lines, Instant placedAt) {
        Preconditions.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one line");
        }
        return new Order(OrderId.newId(), List.copyOf(lines),
                Preconditions.requireNonNull(placedAt, "placedAt"));
    }

    /** Re-creates an existing order, for example when loading it from persistence. */
    public static Order restore(OrderId id, List<Line> lines, Instant placedAt) {
        return new Order(id, List.copyOf(lines), placedAt);
    }
}
