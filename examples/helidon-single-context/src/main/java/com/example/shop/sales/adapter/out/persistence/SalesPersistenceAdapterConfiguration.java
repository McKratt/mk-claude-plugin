package com.example.shop.sales.adapter.out.persistence;

import com.example.shop.sales.application.port.out.OrderStore;
import com.example.shop.sales.application.port.out.TransactionRunner;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import org.flywaydb.core.Flyway;

/**
 * The only public class of the package: it creates the persistence adapters, which stay package-private,
 * and hands them out as ports. Used only by the hexagon's configuration class.
 */
public final class SalesPersistenceAdapterConfiguration {

    private final DbClientTransactionRunner transactionRunner;
    private final OrderPersistenceAdapter orderPersistenceAdapter;

    /** @param dbConfig the {@code db} node of the configuration (DbClient settings) */
    public SalesPersistenceAdapterConfiguration(Config dbConfig) {
        migrate(dbConfig.get("connection"));
        transactionRunner = new DbClientTransactionRunner(DbClient.create(dbConfig));
        orderPersistenceAdapter = new OrderPersistenceAdapter(transactionRunner);
    }

    public OrderStore orderStore() {
        return orderPersistenceAdapter;
    }

    public TransactionRunner transactionRunner() {
        return transactionRunner;
    }

    /** The adapter owns its schema: Flyway applies src/main/resources/db/migration before first use. */
    private static void migrate(Config connection) {
        Flyway.configure()
                .dataSource(connection.get("url").asString().get(),
                        connection.get("username").asString().get(),
                        connection.get("password").asString().get())
                .load()
                .migrate();
    }
}
