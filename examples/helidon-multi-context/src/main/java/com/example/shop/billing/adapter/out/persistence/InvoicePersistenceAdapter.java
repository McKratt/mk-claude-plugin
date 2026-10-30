package com.example.shop.billing.adapter.out.persistence;

import com.example.shop.billing.domain.Invoice;
import com.example.shop.billing.application.port.out.InvoiceFinder;
import com.example.shop.billing.application.port.out.InvoiceStore;
import com.example.shop.common.persistence.DbTransactions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Maps invoices to the billing context's schema with plain JDBC. The PostgreSQL driver binds and reads UUID and
 * OffsetDateTime natively through setObject and getObject.
 */
@RequiredArgsConstructor
class InvoicePersistenceAdapter implements InvoiceStore, InvoiceFinder {

    private final DbTransactions dbTransactions;

    @Override
    public void save(Invoice invoice) {
        dbTransactions.run(connection -> {
            try (var insert = connection.prepareStatement(
                    "INSERT INTO shop_billing.invoices (id, order_id, item_count, issued_at) VALUES (?, ?, ?, ?)")) {
                insert.setObject(1, invoice.getId());
                insert.setObject(2, invoice.getOrderId());
                insert.setInt(3, invoice.getItemCount());
                insert.setObject(4, OffsetDateTime.ofInstant(invoice.getIssuedAt(), ZoneOffset.UTC));
                insert.executeUpdate();
            }
        });
    }

    @Override
    public Optional<Invoice> loadByOrderId(UUID orderId) {
        return dbTransactions.call(connection -> {
            try (var select = connection.prepareStatement(
                    "SELECT id, item_count, issued_at FROM shop_billing.invoices WHERE order_id = ?")) {
                select.setObject(1, orderId);
                try (var rows = select.executeQuery()) {
                    return rows.next() ? Optional.of(toInvoice(rows, orderId)) : Optional.<Invoice>empty();
                }
            }
        });
    }

    private static Invoice toInvoice(ResultSet row, UUID orderId) throws SQLException {
        return Invoice.restore(
                row.getObject("id", UUID.class),
                orderId,
                row.getInt("item_count"),
                row.getObject("issued_at", OffsetDateTime.class).toInstant());
    }
}
