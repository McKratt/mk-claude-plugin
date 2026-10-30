package com.acme.shop.application.service;

import com.acme.shop.application.port.in.OrderPlacement;
import com.acme.shop.application.port.in.PlaceOrderCommand;
import com.acme.shop.application.port.out.OrderStore;
import com.acme.shop.domain.Order;
import io.helidon.service.registry.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@Service.Singleton
public class PlaceOrderService implements OrderPlacement {

    private final OrderStore orderStore;
    private final HttpClient http = HttpClient.newHttpClient();

    public PlaceOrderService(OrderStore orderStore) {
        this.orderStore = orderStore;
    }

    @Override
    public UUID place(PlaceOrderCommand command) {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://stock.internal/products/" + command.productId())).build();
        try {
            if (http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() != 200) {
                throw new IllegalStateException("out of stock: " + command.productId());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        Order order = Order.place(command.customerId(), command.productId());
        orderStore.save(order);
        return order.getId();
    }
}
