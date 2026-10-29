package com.example.shop.sales.billing.fixture;

import com.example.shop.sales.order.domain.Order;

/** Deliberately violates the bounded-context rules: billing must not use order at all. */
public class ReachesIntoOrder {

    Order order;
}
