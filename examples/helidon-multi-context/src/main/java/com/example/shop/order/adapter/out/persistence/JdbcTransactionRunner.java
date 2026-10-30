package com.example.shop.order.adapter.out.persistence;

import com.example.shop.common.persistence.DbTransactions;
import com.example.shop.order.application.port.out.TransactionRunner;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/** The order context's transaction port, backed by the application's shared {@link DbTransactions}. */
@RequiredArgsConstructor
class JdbcTransactionRunner implements TransactionRunner {

    private final DbTransactions dbTransactions;

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return dbTransactions.inTransaction(work);
    }
}
