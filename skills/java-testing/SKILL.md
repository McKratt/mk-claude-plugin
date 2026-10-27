---
name: java-testing
description: Use when writing, reviewing, or fixing Java tests for a Spring Boot project. Prioritizes @SpringBootTest tests that cover a whole use case (Testcontainers database, WireMock for third parties, run by Maven failsafe) over slice tests; also covers unit tests (JUnit, AssertJ, BDDMockito).
---

# Java Testing

Based on the `java-testing` skill of McKratt/claude-skills, with one deliberate change: **slice tests are no longer the
default for Spring. Tests that cover a whole use case with `@SpringBootTest` come first.**

## Stack

| Concern         | Library                                                                         |
|-----------------|---------------------------------------------------------------------------------|
| Test runner     | JUnit 6 (`@Test`, `@ExtendWith`, `@ParameterizedTest`)                          |
| Assertions      | AssertJ (`assertThat(...)`)                                                     |
| Mocking         | Mockito via BDD notation (`BDDMockito`), for unit tests only                    |
| Database        | Real database in a container: Testcontainers 2.0 (never H2 or another embedded substitute) |
| Third parties   | WireMock through its Spring Boot integration (`org.wiremock.integrations:wiremock-spring-boot`) |
| Spring          | Spring Boot 4 Test: `@SpringBootTest` first, slices (`@WebMvcTest`, `@DataJpaTest`) as the exception |
| Build           | `maven-surefire-plugin` for `*Test`, `maven-failsafe-plugin` for `*IT`           |

---

## Which test for what

In order of priority:

1. **Use case tests (`*IT`, `@SpringBootTest`)**: the application starts on a random port, the test enters through the real
   entry point (HTTP) and checks the outcome end to end: response, database content, requests sent to third parties.
   Cover the main path of every use case, plus its important failure paths (validation error, third party down,
   rollback).
2. **Unit tests (`*Test`)** for logic that does not need Spring: domain objects, value objects, self-validating commands,
   mappers, and services with branching logic that is awkward to reach through the use case tests.
3. **Architecture tests (ArchUnit, `*Test`)**: no server needed, so surefire runs them.
4. **Slice tests (`@WebMvcTest`, `@DataJpaTest`)**: only when a use case test cannot reach the behavior reasonably,
   for example exhaustive request-validation combinations or a complex query with many edge cases. Say why in the test
   class. Never write one by default or "per layer".

Do not mock your own classes in use case tests: the point is to exercise the real wiring. Mock only what is outside the
application, and do it at the network boundary (WireMock), not by replacing a bean.

---

## Maven: unit tests in `test`, server tests in `integration-test`

Every `@SpringBootTest` (and so every test that starts the web server) is named `*IT` and **runs in the Maven
integration-test phase** through failsafe. Surefire keeps the fast `*Test` classes. `mvn verify` runs both; `mvn test`
runs only the fast ones.

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-failsafe-plugin</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>integration-test</goal>
        <goal>verify</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Test dependencies: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-testcontainers`, `org.testcontainers:testcontainers-postgresql`
(adapt the module to the database in use) and `org.wiremock.integrations:wiremock-spring-boot`. Testcontainers needs a running Docker daemon.

---

## Use case tests

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

## Structure: Arrange / Act / Assert

Every test method follows AAA with explicit comments when the sections are non-trivial:

```java
@Test
void should_save_the_order_when_it_is_placed() {
    // Arrange
    var command = new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 2)));

    // Act
    var orderId = placeOrderService.placeOrder(command);

    // Assert
    then(orderStore).should().save(argThat(order -> order.id().equals(orderId)));
}
```

---

## Unit tests

### Class setup

```java
@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {

    @Mock
    private OrderStore orderStore;            // outgoing port: mocked

    @InjectMocks
    private PlaceOrderService placeOrderService;
}
```

### BDD Mockito notation

Always use `BDDMockito` static imports, never `Mockito.when` / `Mockito.verify`:

```java
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
```

```java
given(repository.findById("123")).willReturn(Optional.of(entity));
given(repository.save(any(Order.class))).willAnswer(i -> i.getArgument(0));
willThrow(new DataAccessException("db down") {}).given(repository).findById("x");

then(repository).should().save(any(Order.class));
then(repository).should(never()).delete(any());
then(repository).shouldHaveNoMoreInteractions();
```

### AssertJ assertions

Prefer specific assertions over `isEqualTo(true)`:

```java
assertThat(results)
    .hasSize(2)
    .extracting(Order::getId)
    .containsExactly("1", "2");

assertThatThrownBy(() -> service.findById(null))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("id must not be null");

assertThat(result).isPresent().contains(expectedOrder);

assertSoftly(softly -> {
    softly.assertThat(order.getId()).isEqualTo("123");
    softly.assertThat(order.getStatus()).isEqualTo(Status.PENDING);
});
```

---

## Slice tests (the exception)

Reach for a slice only when a use case test cannot cover the behavior reasonably (see "Which test for what"), and use the
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

## Naming

Test method names use `snake_case` following `should_<expected_behavior>_when_<condition>`:

```
should_return_order_when_id_exists
should_throw_not_found_exception_when_id_missing
should_send_email_when_order_is_shipped
```

Class names: `PlaceOrderService` is tested by `PlaceOrderServiceTest`; a use case test that starts the application is named after
the use case with the `IT` suffix: `PlaceOrderIT`.

---

## What NOT to do

- Do not write slice tests by default, and do not test "per layer" with mocks of your own classes when a use case test would cover it
- Do not use H2 or any embedded database in place of the real engine
- Do not call real third-party services from tests: stub them with WireMock
- Do not run tests that start the web server in surefire: name them `*IT` so failsafe runs them in the integration-test phase
- Do not use `Mockito.when(...)` or `Mockito.verify(...)`: always use BDD notation
- Do not use `assertTrue(result != null)`: use AssertJ (`assertThat(result).isNotNull()`)
- Do not share mutable test state between test methods
- Do not use camelCase for test method names: always use snake_case

## Source

Adapted from `skills/java-testing/skill.md` in McKratt/claude-skills. The use case test setup (Testcontainers 2.0 with
`@ServiceConnection`, `wiremock-spring-boot`, failsafe) follows the `examples/` projects of this plugin, which are built by
CI in the plugin repository.
