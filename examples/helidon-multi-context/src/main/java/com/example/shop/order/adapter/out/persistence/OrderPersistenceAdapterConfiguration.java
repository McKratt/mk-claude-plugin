package com.example.shop.order.adapter.out.persistence;

import com.example.shop.common.persistence.DbTransactions;
import com.example.shop.order.application.port.out.OrderStore;
import com.example.shop.order.application.port.out.TransactionRunner;
import io.helidon.config.Config;
import org.flywaydb.core.Flyway;

/**
 * The only public class of the package: it creates the persistence adapters of the order context, which stay
 * package-private, and hands them out as ports. Used only by the context's configuration class.
 */
public final class OrderPersistenceAdapterConfiguration {

    private final OrderPersistenceAdapter orderPersistenceAdapter;
    private final DbClientTransactionRunner transactionRunner;

    /**
     * @param dbConfig       the {@code db} node of the configuration, for the schema migration
     * @param dbTransactions the application's shared DbClient
     */
    public OrderPersistenceAdapterConfiguration(Config dbConfig, DbTransactions dbTransactions) {
        migrate(dbConfig.get("connection"));
        orderPersistenceAdapter = new OrderPersistenceAdapter(dbTransactions);
        transactionRunner = new DbClientTransactionRunner(dbTransactions);
    }

    public OrderStore orderStore() {
        return orderPersistenceAdapter;
    }

    public TransactionRunner transactionRunner() {
        return transactionRunner;
    }

    /** The context owns its schema (ordering) and its own Flyway history in it. */
    private static void migrate(Config connection) {
        Flyway.configure()
                .dataSource(connection.get("url").asString().get(),
                        connection.get("username").asString().get(),
                        connection.get("password").asString().get())
                .schemas("ordering")
                .locations("classpath:db/migration/order")
                .load()
                .migrate();
    }
}
