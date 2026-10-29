package com.example.shop.adapter.out.persistence;

import com.example.shop.application.port.out.TransactionRunner;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.DbTransaction;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/**
 * Binds the open DbClient transaction to the current thread, so every persistence adapter of this package
 * joins it through {@link #executor()}. Helidon serves each request on its own virtual thread.
 */
@RequiredArgsConstructor
class DbClientTransactionRunner implements TransactionRunner {

    private final DbClient dbClient;
    private final ThreadLocal<DbTransaction> current = new ThreadLocal<>();

    @Override
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
    DbExecute executor() {
        var transaction = current.get();
        return transaction != null ? transaction : dbClient.execute();
    }
}
