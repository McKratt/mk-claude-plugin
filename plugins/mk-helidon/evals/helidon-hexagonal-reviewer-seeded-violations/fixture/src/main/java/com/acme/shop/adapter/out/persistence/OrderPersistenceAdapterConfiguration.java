package com.acme.shop.adapter.out.persistence;

import com.acme.shop.application.port.out.OrderStore;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;

public final class OrderPersistenceAdapterConfiguration {

    private OrderPersistenceAdapterConfiguration() {
    }

    public static OrderStore orderStore(Config db) {
        return new OrderPersistenceAdapter(DbClient.create(db));
    }
}
