package com.acme.shop.application.port.out;

public interface IStockChecker {

    boolean inStock(String productId);
}
