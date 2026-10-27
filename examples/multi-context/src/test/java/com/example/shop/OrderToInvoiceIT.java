package com.example.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@IntegrationTest
class OrderToInvoiceIT {

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    // Bound to the running server (random port) by @AutoConfigureRestTestClient on @IntegrationTest.
    @Autowired
    RestTestClient client;
    @Test
    void should_create_an_invoice_in_billing_when_an_order_is_placed() throws Exception {
        var created = client.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body("{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":2},{\"sku\":\"SKU-2\",\"quantity\":1}]}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class).returnResult().getResponseBody();
        var matcher = ID.matcher(created);
        assertThat(matcher.find()).as("no order id in: " + created).isTrue();
        var orderId = matcher.group(1);

        client.get().uri("/invoices/by-order/{orderId}", orderId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.itemCount").isEqualTo(3)
                .jsonPath("$.orderId").isEqualTo(orderId);
    }

    @Test
    void should_answer_404_when_there_is_no_invoice() throws Exception {
        client.get().uri("/invoices/by-order/00000000-0000-0000-0000-000000000000")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_return_400_when_the_order_has_no_lines() throws Exception {
        client.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body("{\"lines\":[]}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
