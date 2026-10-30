package com.example.shop.configuration;

import com.example.shop.billing.application.port.in.InvoiceCreation;
import com.example.shop.common.persistence.DbTransactions;
import com.example.shop.order.adapter.in.web.OrderWebAdapterConfiguration;
import com.example.shop.order.adapter.out.billing.OrderBillingDomainAdapterConfiguration;
import com.example.shop.order.adapter.out.persistence.OrderPersistenceAdapterConfiguration;
import com.example.shop.order.application.service.PlaceOrderService;
import com.example.shop.order.application.port.in.OrderPlacement;
import io.helidon.webserver.http.HttpRules;
import java.time.Clock;

/** Wiring of the order bounded context. Billing is reached through billing's incoming port, given by ShopConfiguration. */
public final class OrderConfiguration {

    private final OrderPlacement orderPlacement;

    public OrderConfiguration(DbTransactions dbTransactions, Clock clock, InvoiceCreation invoiceCreation) {
        var persistence = new OrderPersistenceAdapterConfiguration(dbTransactions);
        var invoiceIssuer = OrderBillingDomainAdapterConfiguration.invoiceIssuer(invoiceCreation);
        orderPlacement = new PlaceOrderService(
                persistence.orderStore(), invoiceIssuer, persistence.transactionRunner(), clock);
    }

    public void routing(HttpRules rules) {
        OrderWebAdapterConfiguration.routing(rules, orderPlacement);
    }
}
