package com.example.shop.sales.order.fixture;

import com.example.shop.sales.billing.application.domain.model.Invoice;

/** Deliberately violates the bounded-context rules: order must not use billing's domain. */
public class ReachesIntoBilling {

    Invoice invoice;
}
