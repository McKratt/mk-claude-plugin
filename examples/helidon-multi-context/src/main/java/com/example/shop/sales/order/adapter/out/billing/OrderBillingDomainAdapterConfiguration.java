package com.example.shop.sales.order.adapter.out.billing;

import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import com.example.shop.sales.order.application.port.out.InvoiceIssuer;

public final class OrderBillingDomainAdapterConfiguration {

    private OrderBillingDomainAdapterConfiguration() {
    }

    /** @param invoiceCreation billing's incoming port, created by billing's configuration */
    public static InvoiceIssuer invoiceIssuer(InvoiceCreation invoiceCreation) {
        return new BillingDomainAdapter(invoiceCreation);
    }
}
