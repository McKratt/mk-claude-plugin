package com.example.shop.billing.adapter.out.persistence;

import com.example.shop.billing.domain.Invoice;
import com.example.shop.billing.application.port.out.InvoiceFinder;
import com.example.shop.billing.application.port.out.InvoiceStore;
import com.example.shop.common.persistence.DbTransactions;
import io.helidon.dbclient.DbRow;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** Maps invoices to the billing context's schema. DbClient binds a UUID as a string, hence the explicit casts. */
@RequiredArgsConstructor
class InvoicePersistenceAdapter implements InvoiceStore, InvoiceFinder {

    private final DbTransactions dbTransactions;

    @Override
    public void save(Invoice invoice) {
        dbTransactions.executor().insert(
                "INSERT INTO shop_billing.invoices (id, order_id, item_count, issued_at)"
                        + " VALUES (CAST(? AS UUID), CAST(? AS UUID), ?, ?)",
                invoice.getId(), invoice.getOrderId(), invoice.getItemCount(),
                OffsetDateTime.ofInstant(invoice.getIssuedAt(), ZoneOffset.UTC));
    }

    @Override
    public Optional<Invoice> loadByOrderId(UUID orderId) {
        return dbTransactions.executor()
                .get("SELECT CAST(id AS VARCHAR) AS id, item_count, issued_at FROM shop_billing.invoices"
                        + " WHERE order_id = CAST(? AS UUID)", orderId)
                .map(row -> toInvoice(row, orderId));
    }

    private static Invoice toInvoice(DbRow row, UUID orderId) {
        return Invoice.restore(
                UUID.fromString(row.column("id").get(String.class)),
                orderId,
                row.column("item_count").get(Integer.class),
                row.column("issued_at").get(java.sql.Timestamp.class).toInstant());
    }
}
