package com.example.shop.sales.billing.adapter.in.web;

import com.example.shop.sales.billing.application.port.in.InvoiceLookup;
import io.helidon.webserver.http.HttpRules;

public final class BillingWebAdapterConfiguration {

    private BillingWebAdapterConfiguration() {
    }

    /** Registers the web adapters of the billing context; they depend on incoming ports only. */
    public static void routing(HttpRules rules, InvoiceLookup invoiceLookup) {
        rules.register("/invoices", new InvoiceController(invoiceLookup));
    }
}
