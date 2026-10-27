package com.example.shop.sales.billing.adapter.out.persistence;

import com.example.shop.sales.billing.application.port.out.InvoiceFinder;
import com.example.shop.sales.billing.application.port.out.InvoiceStore;
import com.example.shop.sales.billing.application.port.out.TransactionRunner;
import com.example.shop.sales.common.persistence.DbTransactions;
import io.helidon.config.Config;
import org.flywaydb.core.Flyway;

/**
 * The only public class of the package: it creates the persistence adapters of the billing context, which stay
 * package-private, and hands them out as ports. Used only by the context's configuration class.
 */
public final class BillingPersistenceAdapterConfiguration {

    private final InvoicePersistenceAdapter invoicePersistenceAdapter;
    private final DbClientTransactionRunner transactionRunner;

    /**
     * @param dbConfig       the {@code db} node of the configuration, for the schema migration
     * @param dbTransactions the component's shared DbClient
     */
    public BillingPersistenceAdapterConfiguration(Config dbConfig, DbTransactions dbTransactions) {
        migrate(dbConfig.get("connection"));
        invoicePersistenceAdapter = new InvoicePersistenceAdapter(dbTransactions);
        transactionRunner = new DbClientTransactionRunner(dbTransactions);
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

    /** The context owns its schema (sales_billing) and its own Flyway history in it. */
    private static void migrate(Config connection) {
        Flyway.configure()
                .dataSource(connection.get("url").asString().get(),
                        connection.get("username").asString().get(),
                        connection.get("password").asString().get())
                .schemas("sales_billing")
                .locations("classpath:db/migration/billing")
                .load()
                .migrate();
    }
}
