package com.example.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

@IntegrationTest
class OrderToInvoiceIT {

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    @Value("${local.server.port}")
    int port;
    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> post(String path, String json) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
    @Test
    void should_create_an_invoice_in_billing_when_an_order_is_placed() throws Exception {
        var created = post("/orders",
                "{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":2},{\"sku\":\"SKU-2\",\"quantity\":1}]}");
        assertEquals(201, created.statusCode());
        var matcher = ID.matcher(created.body());
        assertTrue(matcher.find(), "no order id in: " + created.body());
        var orderId = matcher.group(1);

        var invoice = get("/invoices/by-order/" + orderId);

        assertEquals(200, invoice.statusCode());
        assertTrue(invoice.body().contains("\"itemCount\":3"), invoice.body());
        assertTrue(invoice.body().contains(orderId), invoice.body());
    }

    @Test
    void should_answer_404_when_there_is_no_invoice() throws Exception {
        var response = get("/invoices/by-order/00000000-0000-0000-0000-000000000000");

        assertEquals(404, response.statusCode());
    }

    @Test
    void should_return_400_when_the_order_has_no_lines() throws Exception {
        assertEquals(400, post("/orders", "{\"lines\":[]}").statusCode());
    }
}
