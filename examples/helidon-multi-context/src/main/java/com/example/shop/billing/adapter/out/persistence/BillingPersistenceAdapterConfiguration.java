package com.example.shop.billing.adapter.out.persistence;

import com.example.shop.billing.application.port.out.InvoiceFinder;
import com.example.shop.billing.application.port.out.InvoiceStore;
import com.example.shop.billing.application.port.out.TransactionRunner;
import com.example.shop.common.persistence.DbTransactions;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * The only public class of the package: it creates the persistence adapters of the billing context, which stay
 * package-private, and hands them out as ports. Used only by the context's configuration class.
 */
public final class BillingPersistenceAdapterConfiguration {

    private final InvoicePersistenceAdapter invoicePersistenceAdapter;
    private final JdbcTransactionRunner transactionRunner;

    /** @param dbTransactions the application's shared connection pool and transaction */
    public BillingPersistenceAdapterConfiguration(DbTransactions dbTransactions) {
        migrate(dbTransactions.dataSource());
        invoicePersistenceAdapter = new InvoicePersistenceAdapter(dbTransactions);
        transactionRunner = new JdbcTransactionRunner(dbTransactions);
    }

    public InvoiceStore invoiceStore() {
        return invoicePersistenceAdapter;
    }

    public InvoiceFinder invoiceFinder() {
        return invoicePersistenceAdapter;
    }

    public TransactionRunner transactionRunner() {
        return transactionRunner;
    }

    /** The context owns its schema (shop_billing) and its own Flyway history in it. */
    private static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas("shop_billing")
                .locations("classpath:db/migration/billing")
                .load()
                .migrate();
    }
}
