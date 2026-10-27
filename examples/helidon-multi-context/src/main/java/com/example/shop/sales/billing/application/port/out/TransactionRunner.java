package com.example.shop.sales.billing.application.port.out;

import java.util.function.Supplier;

/**
 * Transaction boundary of a use case. Without a container there is no {@code @Transactional}: the service
 * says where the transaction starts and ends through this port, and the persistence adapter implements it.
 */
public interface TransactionRunner {

    /** Runs the work in one transaction (joining the open one, if any): commits when it returns, rolls back when it throws. */
    <T> T inTransaction(Supplier<T> work);
}
