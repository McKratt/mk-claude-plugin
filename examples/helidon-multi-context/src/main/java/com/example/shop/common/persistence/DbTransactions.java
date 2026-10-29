package com.example.shop.common.persistence;

import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.DbTransaction;
import java.util.function.Supplier;

/**
 * One DbClient for the whole application, with the open transaction bound to the current thread (Helidon serves
 * each request on its own virtual thread). The persistence adapters of every bounded context run their
 * statements on {@link #executor()}, so a use case of one context that calls another context through a
 * DomainAdapter keeps a single transaction. Only persistence adapters and configuration classes may use it.
 */
public final class DbTransactions {

    private final DbClient dbClient;
    private final ThreadLocal<DbTransaction> current = new ThreadLocal<>();

    private DbTransactions(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    /** @param dbConfig the {@code db} node of the configuration (DbClient settings) */
    public static DbTransactions create(Config dbConfig) {
        return new DbTransactions(DbClient.create(dbConfig));
    }

    /** Runs the work in the open transaction, or in a new one that commits when it returns and rolls back when it throws. */
    public <T> T inTransaction(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();
        }
        var transaction = dbClient.transaction();
        current.set(transaction);
        try {
            T result = work.get();
            transaction.commit();
            return result;
        } catch (RuntimeException | Error e) {
            try {
                transaction.rollback();
            } catch (RuntimeException rollbackFailure) {
                // A failed statement already closed the connection (the pool then discards the work):
                // keep the original failure visible.
                e.addSuppressed(rollbackFailure);
            }
            throw e;
        } finally {
            current.remove();
        }
    }

    /** The current transaction, or a non-transactional executor outside of one. */
    public DbExecute executor() {
        var transaction = current.get();
        return transaction != null ? transaction : dbClient.execute();
    }
}
