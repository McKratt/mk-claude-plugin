package com.example.shop.sales.order.configuration;

import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import com.example.shop.sales.common.persistence.DbTransactions;
import com.example.shop.sales.order.adapter.in.web.OrderWebAdapterConfiguration;
import com.example.shop.sales.order.adapter.out.billing.OrderBillingDomainAdapterConfiguration;
import com.example.shop.sales.order.adapter.out.persistence.OrderPersistenceAdapterConfiguration;
import com.example.shop.sales.order.application.domain.service.PlaceOrderService;
import com.example.shop.sales.order.application.port.in.OrderPlacement;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRules;
import java.time.Clock;

/** Wiring of the order hexagon. Billing is reached through billing's incoming port, given by the component. */
public final class OrderConfiguration {

    private final OrderPlacement orderPlacement;

    public OrderConfiguration(Config dbConfig, DbTransactions dbTransactions, Clock clock,
            InvoiceCreation invoiceCreation) {
        var persistence = new OrderPersistenceAdapterConfiguration(dbConfig, dbTransactions);
        var invoiceIssuer = OrderBillingDomainAdapterConfiguration.invoiceIssuer(invoiceCreation);
        orderPlacement = new PlaceOrderService(
                persistence.orderStore(), invoiceIssuer, persistence.transactionRunner(), clock);
    }

    public void routing(HttpRules rules) {
        OrderWebAdapterConfiguration.routing(rules, orderPlacement);
    }
}
