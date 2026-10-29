---
name: spring-testing
description: Use when writing, reviewing, or fixing tests of a Spring Boot 4 project. Spring mechanics of the java-testing rules (mk-java plugin): use case tests with @SpringBootTest on a random port, Testcontainers wired by @ServiceConnection, WireMock through wiremock-spring-boot, RestTestClient, modular test starters, @MockitoBean, and slice tests as the exception.
---

# Spring Boot Testing

The general rules (priorities, `*IT` run by failsafe, real database, WireMock, AAA, BDD Mockito, AssertJ, naming) are
in the `java-testing` skill of the `mk-java` plugin, which this plugin depends on. Apply them first; this skill adds the
Spring Boot 4 mechanics.

## Stack

| Concern         | Library                                                                         |
|-----------------|---------------------------------------------------------------------------------|
| Test runner     | JUnit (version managed by the Spring Boot BOM)                                  |
| Database        | Testcontainers 2.0, wired with `@ServiceConnection`                              |
| Third parties   | WireMock through its Spring Boot integration (`org.wiremock.integrations:wiremock-spring-boot`) |
| Spring          | Spring Boot 4 Test: `@SpringBootTest` first, slices (`@WebMvcTest`, `@DataJpaTest`) as the exception |

Test dependencies: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`,
`spring-boot-testcontainers`, `org.testcontainers:testcontainers-postgresql` (adapt the module to the database in use)
and `org.wiremock.integrations:wiremock-spring-boot`.

Every `@SpringBootTest` (and so every test that starts the web server) is named `*IT` and runs in the integration-test
phase (failsafe), as `java-testing` requires.

---

## Use case tests (`@SpringBootTest`)

### Shared setup

One `@TestConfiguration` declares the container, and `@ServiceConnection` points the application's datasource at it. A
composed annotation keeps the tests short and makes them share one Spring context (and so one container):

```java
@TestConfiguration(proxyBeanMethods = false)
class PostgresTestConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {   // org.testcontainers.postgresql.PostgreSQLContainer (Testcontainers 2.0)
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient   // binds the injected RestTestClient to the running server
@Import(PostgresTestConfiguration.class)
@interface IntegrationTest {
}
```

Use the same database engine and major version as production.

### A use case test with a mocked third party

```java
@IntegrationTest
@EnableWireMock(@ConfigureWireMock(name = "shipping", baseUrlProperties = "shipping.base-url"))
class PlaceOrderIT {

    // Random port; the base URL is published as the property shipping.base-url (read by the adapter configuration).
    @InjectWireMock("shipping")
    WireMockServer shipping;

    // Bound to the running server (random port) by @AutoConfigureRestTestClient on @IntegrationTest.
    @Autowired
    RestTestClient client;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean_database() {
        jdbcTemplate.execute("delete from order_lines");
        jdbcTemplate.execute("delete from orders");
    }

    @Test
    void should_store_the_order_and_request_a_shipment_when_the_order_is_valid() {
        // Arrange
        shipping.stubFor(WireMock.post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(202)));

        // Act
        client.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"lines":[{"sku":"SKU-1","quantity":2},{"sku":"SKU-2","quantity":1}]}""")
                .exchange()
                // Assert
                .expectStatus().isCreated();
        assertThat(jdbcTemplate.queryForObject("select count(*) from orders", Integer.class)).isEqualTo(1);
        shipping.verify(1, postRequestedFor(urlEqualTo("/shipments"))
                .withRequestBody(matchingJsonPath("$.itemCount", equalTo("3"))));
    }

    @Test
    void should_store_nothing_when_the_shipping_api_fails() {
        // Arrange
        shipping.stubFor(WireMock.post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(503)));

        // Act
        client.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"lines":[{"sku":"SKU-1","quantity":1}]}""")
                .exchange()
                // Assert
                .expectStatus().is5xxServerError();
        assertThat(jdbcTemplate.queryForObject("select count(*) from orders", Integer.class)).isZero();
    }
}
```

Rules for these tests:

- **Isolation**: clean the tables in `@BeforeEach` (or use unique ids and query by id). Never rely on test order.
- **Container lifecycle**: declare it as a bean in a `@TestConfiguration`, not per class, so every `*IT` shares one container.
- **WireMock URL**: `@EnableWireMock(@ConfigureWireMock(name = "shipping", baseUrlProperties = "shipping.base-url"))`
  starts a WireMock on a random port and publishes its base URL as a Spring property, so no `@DynamicPropertySource`
  is needed; `@InjectWireMock("shipping") WireMockServer` gives access to it
  ([WireMock Spring Boot docs](https://wiremock.org/docs/spring-boot/)).
- **Verify what you send**: `shipping.verify(...)` on the path, headers and body the adapter produces.
- **Failure paths are part of the use case**: stub 4xx/5xx and timeouts, and assert the consequence (HTTP status, rollback).
- **Outgoing HTTP always goes through Spring's `RestClient`, built from the auto-configured `RestClient.Builder`**
  (inject it; dependency `spring-boot-starter-restclient`). Never `java.net.http.HttpClient`, `RestClient.builder()`
  or `RestClient.create()` in application code: the injected builder carries Spring Boot's message converters, request
  factory and global `spring.http.clients.*` settings
  ([Spring Boot reference](https://docs.spring.io/spring-boot/reference/io/rest-client.html)). An ArchUnit rule can
  enforce it (no dependency on `java.net.http..`, `org.apache.hc..`, `okhttp3..`).
- **JDK client, no extra HTTP library**: without Apache/Jetty/Reactor Netty on the classpath, `RestClient` uses the JDK
  `HttpClient`, and that is fine with `wiremock-spring-boot`: the example tests pass in CI with it (4.0.9, Spring Boot
  4.1). Known pitfall: with a plain `WireMockExtension` (wiremock-standalone 3.13.2) instead, POSTs with a body failed
  in CI with `Received RST_STREAM`, an upstream h2c issue ([wiremock/wiremock #2637](https://github.com/wiremock/wiremock/issues/2637),
  [#3577](https://github.com/wiremock/wiremock/issues/3577)); `http2PlainDisabled(true)` on WireMock or Apache
  HttpClient on the classpath avoided it. Why `wiremock-spring-boot` is not affected has not been investigated.
- **Calling the application from a test**: Spring Boot 4's `RestTestClient`. Annotate the test (here, the
  `@IntegrationTest` meta-annotation) with `@AutoConfigureRestTestClient`
  (`org.springframework.boot.resttestclient.autoconfigure`) and inject `RestTestClient`; with
  `webEnvironment = RANDOM_PORT` it is bound to the running server, and error statuses are assertions
  (`expectStatus().isBadRequest()`), not exceptions. It replaces `TestRestTemplate`, which Boot 4 removed
  ([Boot 4.0 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)).
  If you need the port itself, inject it with `@LocalServerPort` (`org.springframework.boot.test.web.server`), not
  `@Value("${local.server.port}")`.
- **Modular test starters (Spring Boot 4)**: besides `spring-boot-starter-test`, add the starter of each technology the
  tests touch: `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`. The example projects build with
  these coordinates in CI.
- **`@MockBean` no longer exists in Spring Boot 4**: use `@MockitoBean`, and in a use case test only for a collaborator
  that truly cannot be reached otherwise.

---

## Slice tests (the exception)

Reach for a slice only when a use case test cannot cover the behavior reasonably (see "Which test for what" in `java-testing`), and use the
narrowest one. In Spring Boot 4 the slice annotations moved to per-technology packages (for example
`@AutoConfigureMockMvc` is in `org.springframework.boot.webmvc.test.autoconfigure`, per the
[Boot testing reference](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)); check the
import for `@WebMvcTest`, `@DataJpaTest` and `@AutoConfigureTestDatabase` in the Boot 4 API docs, these snippets were not
compiled by the example CI.

```java
// Many request-validation combinations, no need to boot everything for each one.
@WebMvcTest(OrderController.class)
class OrderControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderPlacement orderPlacement;   // the use case: here a mock is the point of the slice
}
```

```java
// A complex query with many edge cases, against the real database engine.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestConfiguration.class)
class OrderQueryTest {
}
```

A `@DataJpaTest` replaces the datasource with an embedded one by default; keep `Replace.NONE` so it uses the container.

---

## What NOT to do (Spring)

- Do not write `@WebMvcTest` / `@DataJpaTest` by default: use case tests with `@SpringBootTest` come first
- Do not let `@DataJpaTest` replace the datasource with an embedded one: keep `Replace.NONE` and the container
- Do not use `@MockBean` (removed in Spring Boot 4) or `TestRestTemplate` (removed): use `@MockitoBean` and `RestTestClient`
- Do not create HTTP clients with `RestClient.builder()`, `RestClient.create()` or `java.net.http.HttpClient` in application code

## Source

Split from the `java-testing` skill (McKratt/claude-skills, then McKratt/mk-claude-plugin). The setup (Testcontainers 2.0
with `@ServiceConnection`, `wiremock-spring-boot`, `RestTestClient`, failsafe) follows the Spring `examples/` projects of
the McKratt/mk-claude-plugin repository, which are built by CI.
