package com.example.shop.common.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.helidon.config.Config;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;

/**
 * One Hikari connection pool for the whole application, with the connection of the open transaction bound to the
 * work through a {@link ScopedValue}. The persistence adapters of every bounded context run their statements
 * through {@link #run} and {@link #call}, so a use case of one context that calls another context through a
 * DomainAdapter keeps a single transaction. Only persistence adapters and configuration classes may use it; the
 * application's configuration closes it when the server stops.
 */
public final class DbTransactions implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final ScopedValue<Connection> current = ScopedValue.newInstance();

    private DbTransactions(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** @param dbConfig the {@code db} node of the configuration (connection and pool settings) */
    public static DbTransactions create(Config dbConfig) {
        var connection = dbConfig.get("connection");
        var hikari = new HikariConfig();
        hikari.setPoolName("shop");
        hikari.setJdbcUrl(connection.get("url").asString().get());
        hikari.setUsername(connection.get("username").asString().get());
        hikari.setPassword(connection.get("password").asString().get());
        dbConfig.get("pool").get("maximum-pool-size").asInt().ifPresent(hikari::setMaximumPoolSize);
        return new DbTransactions(new HikariDataSource(hikari));
    }

    /** The pool, for the schema migrations of the persistence adapter configurations. */
    public DataSource dataSource() {
        return dataSource;
    }

    /** Runs the work in the open transaction, or in a new one that commits when it returns and rolls back when it throws. */
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
    public <T> T call(SqlFunction<T> statements) {
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

    public void run(SqlConsumer statements) {
        call(connection -> {
            statements.accept(connection);
            return null;
        });
    }

    /** Closes the connection pool. */
    @Override
    public void close() {
        dataSource.close();
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
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
