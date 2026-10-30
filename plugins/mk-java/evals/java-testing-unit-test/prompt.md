---
tags: [java-testing]
plugins: [../../../mk-java]
max_turns: 15
timeout_seconds: 300
allowed_tools: [Skill, Read, Glob, Grep]
---

Write the unit test of the out-of-stock branch of this application service of my Java project. Do not create
any file and do not ask me anything: put the complete test class in your answer.

```java
package com.acme.shop.application.service;

public class PlaceOrderService implements OrderPlacement {

    private final OrderStore orderStore;
    private final StockReserver stockReserver;

    public PlaceOrderService(OrderStore orderStore, StockReserver stockReserver) {
        this.orderStore = orderStore;
        this.stockReserver = stockReserver;
    }

    @Override
    public OrderId place(PlaceOrderCommand command) {
        if (!stockReserver.reserve(command.items())) {
            throw new OutOfStockException(command.items());
        }
        Order order = Order.place(command.customerId(), command.items());
        orderStore.save(order);
        return order.id();
    }
}
```
