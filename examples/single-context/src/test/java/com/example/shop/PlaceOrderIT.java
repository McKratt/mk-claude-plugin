package com.example.shop;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The "place an order" use case through the whole stack: real web server, real PostgreSQL (Testcontainers)
 * and the third-party shipping API replaced by WireMock.
 */
@IntegrationTest
class PlaceOrderIT {

    @RegisterExtension
    static WireMockExtension shipping = WireMockExtension.newInstance()
            // The JDK HttpClient behind RestClient tries an h2c upgrade on plain HTTP; WireMock (Jetty)
            // fails on it for POST requests with a body (wiremock/wiremock#2637, #3577).
            .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
            .build();

    @DynamicPropertySource
    static void shippingApi(DynamicPropertyRegistry registry) {
        registry.add("shipping.base-url", shipping::baseUrl);
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean_database() {
        jdbcTemplate.execute("delete from order_lines");
        jdbcTemplate.execute("delete from orders");
    }
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
    void should_store_the_order_and_request_a_shipment_when_the_order_is_valid() throws Exception {
        shipping.stubFor(WireMock.post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(202)));

        var response = post("/orders",
                "{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":2},{\"sku\":\"SKU-2\",\"quantity\":1}]}");

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"id\""));
        assertEquals(1, jdbcTemplate.queryForObject("select count(*) from orders", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject("select count(*) from order_lines", Integer.class));
        shipping.verify(1, postRequestedFor(urlEqualTo("/shipments"))
                .withRequestBody(matchingJsonPath("$.itemCount", equalTo("3"))));
    }

    @Test
    void should_store_nothing_when_the_shipping_api_fails() throws Exception {
        shipping.stubFor(WireMock.post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(503)));

        var response = post("/orders",
                "{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":1}]}");

        assertEquals(500, response.statusCode());
        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from orders", Integer.class));
    }

    @Test
    void should_return_400_and_call_no_third_party_when_the_order_has_no_lines() throws Exception {
        var response = post("/orders", "{\"lines\":[]}");

        assertEquals(400, response.statusCode());
        shipping.verify(0, postRequestedFor(urlEqualTo("/shipments")));
    }
}
