package com.example.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@IntegrationTest
class OrderToInvoiceIT {

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    @Value("${local.server.port}")
    int port;
    private RestClient restClient;

    @BeforeEach
    void create_rest_client(@Autowired RestClient.Builder restClientBuilder) {
        // Error statuses are results to assert on, not exceptions.
        restClient = restClientBuilder
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    private ResponseEntity<String> post(String path, String json) {
        return restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(json)
                .retrieve().toEntity(String.class);
    }

    private ResponseEntity<String> get(String path) {
        return restClient.get().uri(path).retrieve().toEntity(String.class);
    }
    @Test
    void should_create_an_invoice_in_billing_when_an_order_is_placed() throws Exception {
        var created = post("/orders",
                "{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":2},{\"sku\":\"SKU-2\",\"quantity\":1}]}");
        assertEquals(201, created.getStatusCode().value());
        var matcher = ID.matcher(created.getBody());
        assertTrue(matcher.find(), "no order id in: " + created.getBody());
        var orderId = matcher.group(1);

        var invoice = get("/invoices/by-order/" + orderId);

        assertEquals(200, invoice.getStatusCode().value());
        assertTrue(invoice.getBody().contains("\"itemCount\":3"), invoice.getBody());
        assertTrue(invoice.getBody().contains(orderId), invoice.getBody());
    }

    @Test
    void should_answer_404_when_there_is_no_invoice() throws Exception {
        var response = get("/invoices/by-order/00000000-0000-0000-0000-000000000000");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void should_return_400_when_the_order_has_no_lines() throws Exception {
        assertEquals(400, post("/orders", "{\"lines\":[]}").getStatusCode().value());
    }
}
