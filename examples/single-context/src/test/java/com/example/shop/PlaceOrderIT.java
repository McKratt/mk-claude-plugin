package com.example.shop;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

/**
 * The "place an order" use case through the whole stack: real web server, real PostgreSQL (Testcontainers)
 * and the third-party shipping API replaced by WireMock.
 */
@IntegrationTest
@EnableWireMock(@ConfigureWireMock(name = "shipping", baseUrlProperties = "shipping.base-url"))
class PlaceOrderIT {

    // Random port; the base URL is published as shipping.base-url, which the adapter configuration reads.
    @InjectWireMock("shipping")
    WireMockServer shipping;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean_database() {
        jdbcTemplate.execute("delete from order_lines");
        jdbcTemplate.execute("delete from orders");
    }
    // Bound to the running server (random port) by @AutoConfigureRestTestClient on @IntegrationTest.
    @Autowired
    RestTestClient client;
    @Test
    void should_store_the_order_and_request_a_shipment_when_the_order_is_valid() throws Exception {
        shipping.stubFor(WireMock.post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(202)));

        client.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body("{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":2},{\"sku\":\"SKU-2\",\"quantity\":1}]}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.id").isNotEmpty();

        assertEquals(1, jdbcTemplate.queryForObject("select count(*) from orders", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject("select count(*) from order_lines", Integer.class));
        shipping.verify(1, postRequestedFor(urlEqualTo("/shipments"))
                .withRequestBody(matchingJsonPath("$.itemCount", equalTo("3"))));
    }

    @Test
    void should_store_nothing_when_the_shipping_api_fails() throws Exception {
        shipping.stubFor(WireMock.post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(503)));

        client.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body("{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":1}]}")
                .exchange()
                .expectStatus().is5xxServerError();

        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from orders", Integer.class));
    }

    @Test
    void should_return_400_and_call_no_third_party_when_the_order_has_no_lines() throws Exception {
        client.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body("{\"lines\":[]}")
                .exchange()
                .expectStatus().isBadRequest();

        shipping.verify(0, postRequestedFor(urlEqualTo("/shipments")));
    }
}
