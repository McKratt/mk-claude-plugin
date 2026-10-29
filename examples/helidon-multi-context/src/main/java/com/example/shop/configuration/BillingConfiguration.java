package com.example.shop.configuration;

import com.example.shop.billing.adapter.in.web.BillingWebAdapterConfiguration;
import com.example.shop.billing.adapter.out.persistence.BillingPersistenceAdapterConfiguration;
import com.example.shop.billing.application.service.CreateInvoiceService;
import com.example.shop.billing.application.service.FindInvoiceByOrderService;
import com.example.shop.billing.application.port.in.InvoiceCreation;
import com.example.shop.billing.application.port.in.InvoiceLookup;
import com.example.shop.common.persistence.DbTransactions;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRules;
import java.time.Clock;

/** Wiring of the billing bounded context. Its incoming port InvoiceCreation is handed to the order context. */
public final class BillingConfiguration {

    private final InvoiceCreation invoiceCreation;
    private final InvoiceLookup invoiceLookup;

    public BillingConfiguration(Config dbConfig, DbTransactions dbTransactions, Clock clock) {
        var persistence = new BillingPersistenceAdapterConfiguration(dbConfig, dbTransactions);
        invoiceCreation = new CreateInvoiceService(persistence.invoiceStore(), persistence.transactionRunner(), clock);
        invoiceLookup = new FindInvoiceByOrderService(persistence.invoiceFinder());
    }

    public InvoiceCreation invoiceCreation() {
        return invoiceCreation;
    }

    public void routing(HttpRules rules) {
        BillingWebAdapterConfiguration.routing(rules, invoiceLookup);
    }
}
