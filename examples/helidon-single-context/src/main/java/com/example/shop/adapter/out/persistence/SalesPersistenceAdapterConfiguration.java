package com.example.shop.adapter.out.persistence;

import com.example.shop.application.port.out.OrderStore;
import com.example.shop.application.port.out.TransactionRunner;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.helidon.config.Config;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * The only public class of the package: it creates the Hikari connection pool and the persistence adapters,
 * which stay package-private, and hands them out as ports. Used only by the domain's configuration class, which
 * closes it when the server stops.
 */
public final class SalesPersistenceAdapterConfiguration implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final JdbcTransactionRunner transactionRunner;
    private final OrderPersistenceAdapter orderPersistenceAdapter;

    /** @param dbConfig the {@code db} node of the configuration (connection and pool settings) */
    public SalesPersistenceAdapterConfiguration(Config dbConfig) {
        dataSource = dataSource(dbConfig);
        try {
            migrate(dataSource);
        } catch (RuntimeException e) {
            // The caller gets no instance to close.
            dataSource.close();
            throw e;
        }
        transactionRunner = new JdbcTransactionRunner(dataSource);
        orderPersistenceAdapter = new OrderPersistenceAdapter(transactionRunner);
    }

    public OrderStore orderStore() {
        return orderPersistenceAdapter;
    }

    public TransactionRunner transactionRunner() {
        return transactionRunner;
    }

    /** Closes the connection pool. */
    @Override
    public void close() {
        dataSource.close();
    }

    private static HikariDataSource dataSource(Config dbConfig) {
        var connection = dbConfig.get("connection");
        var hikari = new HikariConfig();
        hikari.setPoolName("shop");
        hikari.setJdbcUrl(connection.get("url").asString().get());
        hikari.setUsername(connection.get("username").asString().get());
        hikari.setPassword(connection.get("password").asString().get());
        dbConfig.get("pool").get("maximum-pool-size").asInt().ifPresent(hikari::setMaximumPoolSize);
        return new HikariDataSource(hikari);
    }

    /** The adapter owns its schema: Flyway applies src/main/resources/db/migration before first use. */
    private static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
    }
}
