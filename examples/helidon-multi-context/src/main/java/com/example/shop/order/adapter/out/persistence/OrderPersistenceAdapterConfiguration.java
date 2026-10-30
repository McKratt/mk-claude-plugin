package com.example.shop.order.adapter.out.persistence;

import com.example.shop.common.persistence.DbTransactions;
import com.example.shop.order.application.port.out.OrderStore;
import com.example.shop.order.application.port.out.TransactionRunner;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * The only public class of the package: it creates the persistence adapters of the order context, which stay
 * package-private, and hands them out as ports. Used only by the context's configuration class.
 */
public final class OrderPersistenceAdapterConfiguration {

    private final OrderPersistenceAdapter orderPersistenceAdapter;
    private final JdbcTransactionRunner transactionRunner;

    /** @param dbTransactions the application's shared connection pool and transaction */
    public OrderPersistenceAdapterConfiguration(DbTransactions dbTransactions) {
        migrate(dbTransactions.dataSource());
        orderPersistenceAdapter = new OrderPersistenceAdapter(dbTransactions);
        transactionRunner = new JdbcTransactionRunner(dbTransactions);
    }

    public OrderStore orderStore() {
        return orderPersistenceAdapter;
    }

    public TransactionRunner transactionRunner() {
        return transactionRunner;
    }

    /** The context owns its schema (shop_order) and its own Flyway history in it. */
    private static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas("shop_order")
                .locations("classpath:db/migration/order")
                .load()
                .migrate();
    }
}
