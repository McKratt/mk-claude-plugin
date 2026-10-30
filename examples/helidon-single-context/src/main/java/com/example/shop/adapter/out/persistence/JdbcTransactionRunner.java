package com.example.shop.adapter.out.persistence;

import com.example.shop.application.port.out.TransactionRunner;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;

/**
 * Runs the work in one JDBC transaction on a connection of the Hikari pool. The connection is bound to the work
 * with a {@link ScopedValue}, so every persistence adapter of this package joins it through {@link #run} and
 * {@link #call}.
 */
@RequiredArgsConstructor
class JdbcTransactionRunner implements TransactionRunner {

    private final DataSource dataSource;
    private final ScopedValue<Connection> current = ScopedValue.newInstance();

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        if (current.isBound()) {
            return work.get();
        }
        // Closing hands the connection back to Hikari, which rolls back uncommitted work and restores auto-commit.
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            T result;
            try {
                result = ScopedValue.where(current, connection).call(work::get);
            } catch (RuntimeException | Error e) {
                rollback(connection, e);
                throw e;
            }
            connection.commit();
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Transaction failed: " + e.getMessage(), e);
        }
    }

    /** Runs the statements on the connection of the current transaction, or on a pooled connection in auto-commit. */
    <T> T call(SqlFunction<T> statements) {
        try {
            if (current.isBound()) {
                return statements.apply(current.get());
            }
            try (var connection = dataSource.getConnection()) {
                return statements.apply(connection);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL failed: " + e.getMessage(), e);
        }
    }

    void run(SqlConsumer statements) {
        call(connection -> {
            statements.accept(connection);
            return null;
        });
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            // Keep the original failure visible.
            failure.addSuppressed(rollbackFailure);
        }
    }

    @FunctionalInterface
    interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
