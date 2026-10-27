package com.example.shop;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            .options(wireMockConfig().dynamicPort())
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
    void cleanDatabase() {
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
    void placesAnOrderStoresItAndRequestsAShipment() throws Exception {
        shipping.stubFor(post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(202)));

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
    void storesNothingWhenTheShippingApiFails() throws Exception {
        shipping.stubFor(post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(503)));

        var response = post("/orders",
                "{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":1}]}");

        assertEquals(500, response.statusCode());
        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from orders", Integer.class));
    }

    @Test
    void rejectsAnOrderWithoutLines() throws Exception {
        var response = post("/orders", "{\"lines\":[]}");

        assertEquals(400, response.statusCode());
        shipping.verify(0, postRequestedFor(urlEqualTo("/shipments")));
    }
}
