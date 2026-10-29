package com.example.shop.order.fixture;

import com.example.shop.billing.domain.Invoice;

/** Deliberately violates the bounded-context rules: order must not use billing's domain. */
public class ReachesIntoBilling {

    Invoice invoice;
}
