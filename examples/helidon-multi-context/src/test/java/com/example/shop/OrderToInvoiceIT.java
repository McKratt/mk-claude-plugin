package com.example.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shop.configuration.RunningApplication;
import com.example.shop.configuration.ShopConfiguration;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Placing an order in the order context creates the invoice in the billing context (through
 * BillingDomainAdapter), checked through billing's own HTTP endpoint. The application is started as main starts it,
 * on a random port, with a real PostgreSQL (Testcontainers).
 */
class OrderToInvoiceIT {

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

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
    void clean_database() {
        TestInfrastructure.execute("DELETE FROM shop_billing.invoices");
        TestInfrastructure.execute("DELETE FROM shop_order.order_lines");
        TestInfrastructure.execute("DELETE FROM shop_order.orders");
    }

    @Test
    void should_create_an_invoice_in_billing_when_an_order_is_placed() {
        String orderId;
        try (var response = postOrder("{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":2},{\"sku\":\"SKU-2\",\"quantity\":1}]}")) {
            assertThat(response.status()).isEqualTo(Status.CREATED_201);
            var body = response.as(String.class);
            var matcher = ID.matcher(body);
            assertThat(matcher.find()).as("no order id in: " + body).isTrue();
            orderId = matcher.group(1);
        }

        try (var response = client.get("/invoices/by-order/" + orderId).request()) {
            assertThat(response.status()).isEqualTo(Status.OK_200);
            assertThat(response.as(String.class))
                    .contains("\"orderId\":\"" + orderId + "\"")
                    .contains("\"itemCount\":3");
        }
    }

    /** Billing joins order's transaction (shared DbTransactions): a billing failure rolls the order back. */
    @Test
    void should_store_no_order_when_billing_fails_to_create_the_invoice() {
        TestInfrastructure.execute("CREATE OR REPLACE FUNCTION shop_billing.fail_invoice() RETURNS trigger"
                + " LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'billing down'; END $$");
        TestInfrastructure.execute("CREATE TRIGGER fail_invoice BEFORE INSERT ON shop_billing.invoices"
                + " FOR EACH ROW EXECUTE FUNCTION shop_billing.fail_invoice()");
        try (var response = postOrder("{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":1}]}")) {
            assertThat(response.status().family()).isEqualTo(Status.Family.SERVER_ERROR);
        } finally {
            TestInfrastructure.execute("DROP TRIGGER fail_invoice ON shop_billing.invoices");
        }

        assertThat(count("shop_order.orders")).isZero();
        assertThat(count("shop_order.order_lines")).isZero();
    }

    /**
     * The invoice is written before order's transaction commits: a failure at commit time must take the
     * invoice with it. With one transaction per context, billing's would already be committed.
     */
    @Test
    void should_store_no_invoice_when_the_order_transaction_fails_at_commit() {
        TestInfrastructure.execute("CREATE OR REPLACE FUNCTION shop_order.fail_at_commit() RETURNS trigger"
                + " LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'commit refused'; END $$");
        TestInfrastructure.execute("CREATE CONSTRAINT TRIGGER fail_at_commit AFTER INSERT ON shop_order.orders"
                + " DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION shop_order.fail_at_commit()");
        try (var response = postOrder("{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":1}]}")) {
            assertThat(response.status().family()).isEqualTo(Status.Family.SERVER_ERROR);
        } finally {
            TestInfrastructure.execute("DROP TRIGGER fail_at_commit ON shop_order.orders");
        }

        assertThat(count("shop_order.orders")).isZero();
        assertThat(count("shop_billing.invoices")).isZero();
    }

    @Test
    void should_answer_404_when_there_is_no_invoice() {
        try (var response = client.get("/invoices/by-order/00000000-0000-0000-0000-000000000000").request()) {
            assertThat(response.status()).isEqualTo(Status.NOT_FOUND_404);
        }
    }

    @Test
    void should_return_400_and_store_nothing_when_the_order_has_no_lines() {
        try (var response = postOrder("{\"lines\":[]}")) {
            assertThat(response.status()).isEqualTo(Status.BAD_REQUEST_400);
        }

        assertThat(count("shop_order.orders")).isZero();
        assertThat(count("shop_billing.invoices")).isZero();
    }

    private static Http1ClientResponse postOrder(String json) {
        return client.post("/orders")
                .header(HeaderNames.CONTENT_TYPE, "application/json")
                .submit(json);
    }

    private static long count(String table) {
        return TestInfrastructure.count(table);
    }
}
