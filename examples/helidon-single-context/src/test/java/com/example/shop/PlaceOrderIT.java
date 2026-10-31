package com.example.shop;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.shop.configuration.RunningApplication;
import com.example.shop.configuration.ShopConfiguration;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The "place an order" use case through the whole stack: the application started as main starts it, on a random
 * port, with a real PostgreSQL (Testcontainers) and the shipping API replaced by WireMock.
 */
class PlaceOrderIT {

    private static RunningApplication application;
    private static Http1Client client;

    /** The same start as main, fed with the test configuration. */
    @BeforeAll
    static void start_application() {
        application = ShopConfiguration.start(TestInfrastructure.config());
        client = Http1Client.builder().baseUri("http://localhost:" + application.port()).build();
    }

    @AfterAll
    static void stop_application() {
        application.close();
    }

    @BeforeEach
    void clean_database_and_stubs() {
        TestInfrastructure.execute("DELETE FROM order_lines");
        TestInfrastructure.execute("DELETE FROM orders");
        TestInfrastructure.SHIPPING.resetAll();
    }

    @Test
    void should_store_the_order_and_request_a_shipment_when_the_order_is_valid() {
        TestInfrastructure.SHIPPING.stubFor(post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(202)));

        try (var response = postOrder("{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":2},{\"sku\":\"SKU-2\",\"quantity\":1}]}")) {
            assertThat(response.status()).isEqualTo(Status.CREATED_201);
            assertThat(response.as(String.class)).contains("\"id\"");
        }

        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("order_lines")).isEqualTo(2);
        TestInfrastructure.SHIPPING.verify(1, postRequestedFor(urlEqualTo("/shipments"))
                .withRequestBody(matchingJsonPath("$.itemCount", equalTo("3"))));
    }

    @Test
    void should_store_nothing_when_the_shipping_api_fails() {
        TestInfrastructure.SHIPPING.stubFor(post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(503)));

        try (var response = postOrder("{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":1}]}")) {
            assertThat(response.status().family()).isEqualTo(Status.Family.SERVER_ERROR);
        }

        assertThat(count("orders")).isZero();
    }

    @Test
    void should_return_400_and_call_no_third_party_when_the_order_has_no_lines() {
        try (var response = postOrder("{\"lines\":[]}")) {
            assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);
        }

        TestInfrastructure.SHIPPING.verify(0, postRequestedFor(urlEqualTo("/shipments")));
    }

    private static io.helidon.webclient.http1.Http1ClientResponse postOrder(String json) {
        return client.post("/orders")
                .header(HeaderNames.CONTENT_TYPE, "application/json")
                .submit(json);
    }

    private static long count(String table) {
        return TestInfrastructure.count(table);
    }
}
