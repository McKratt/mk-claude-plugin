# Templates: one vertical slice

Example slice: place an order. Base package `com.example.shop`, component `sales`, bounded context `order`.
Replace names to fit the user's domain. All code targets Java 25 and Spring Boot.
The slice below lives in a component with several bounded contexts (`sales/order`, `sales/billing`).
For a component with a single context, omit the context level: `com.example.shop.sales.domain`,
`com.example.shop.sales.adapter.in.web`, and so on. Utilities usable anywhere in the component go in
`com.example.shop.sales.common`.

## Null-safety (JSpecify)

Add `org.jspecify:jspecify` (version managed by the Spring Boot BOM). Mark the domain, service and port packages
(`domain`, `domain/service`, `application/service`, `application/port/in`, `application/port/out`) as
`@NullMarked`, so every type is non-null unless annotated `@Nullable`:

```java
// sales/order/domain/package-info.java
@NullMarked
package com.example.shop.sales.order.domain;

import org.jspecify.annotations.NullMarked;
```

## Domain (`sales/order/domain`)

```java
package com.example.shop.sales.order.domain;

import java.util.UUID;

public record OrderId(UUID value) {
    public OrderId {
        java.util.Objects.requireNonNull(value, "value must not be null");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
}
```

```java
package com.example.shop.sales.order.domain;

import java.util.List;
import java.util.Objects;

public final class Order {

    public record Line(String sku, int quantity) {}

    private final OrderId id;
    private final List<Line> lines;

    private Order(OrderId id, List<Line> lines) {
        this.id = id;
        this.lines = List.copyOf(lines);
    }

    /** Business rule lives here, not in the command. */
    public static Order place(List<Line> lines) {
        Objects.requireNonNull(lines, "lines must not be null");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one line");
        }
        return new Order(OrderId.newId(), lines);
    }

    /** Re-creates an existing order, for example when loading from persistence. */
    public static Order restore(OrderId id, List<Line> lines) {
        return new Order(id, lines);
    }

    public OrderId id() { return id; }
    public List<Line> lines() { return lines; }
}
```

## Domain service (optional, `sales/order/domain/service`): *not in the examples*

Only when a domain operation does not belong to a single entity or value object, typically when it needs several
aggregates. Plain Java, stateless, named after the operation, no transaction, security or port: the application
service loads the aggregates through its ports, calls the domain service and saves the result. `Customer` below
stands for a second aggregate of the context.

```java
// sales/order/domain/service/LoyaltyDiscount.java
package com.example.shop.sales.order.domain.service;

import com.example.shop.sales.order.domain.Customer;
import com.example.shop.sales.order.domain.Order;

/** Discount rule that needs two aggregates, so it belongs to neither. */
public final class LoyaltyDiscount {

    /** @return the discount in percent: 5 for a customer with at least 10 previous orders, else 0 */
    public int percentFor(Order order, Customer customer) {
        return customer.previousOrderCount() >= 10 && !order.getLines().isEmpty() ? 5 : 0;
    }
}
```

## Incoming port and command (`sales/order/application/port/in`)

```java
package com.example.shop.sales.order.application.port.in;

import com.example.shop.sales.order.domain.OrderId;

public interface OrderPlacement {
    OrderId placeOrder(PlaceOrderCommand command);
}
```

```java
package com.example.shop.sales.order.application.port.in;

import java.util.List;
import java.util.Objects;

/** Input model for exactly one use case. Validates itself on construction. */
public record PlaceOrderCommand(List<Line> lines) {

    public PlaceOrderCommand {
        Objects.requireNonNull(lines, "lines must not be null");
        lines = List.copyOf(lines);
    }

    public record Line(String sku, int quantity) {
        public Line {
            Objects.requireNonNull(sku, "sku must not be null");
            if (sku.isBlank()) {
                throw new IllegalArgumentException("sku must not be blank");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
        }
    }
}
```

## Outgoing port (`sales/order/application/port/out`)

```java
package com.example.shop.sales.order.application.port.out;

import com.example.shop.sales.order.domain.Order;

public interface OrderStore {
    void save(Order order);
}
```

## Application service (`sales/order/application/service`)

```java
package com.example.shop.sales.order.application.service;

import com.example.shop.sales.order.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.order.application.port.in.OrderPlacement;
import com.example.shop.sales.order.application.port.out.OrderStore;
import com.example.shop.sales.order.domain.Order;
import com.example.shop.sales.order.domain.OrderId;
import org.springframework.transaction.annotation.Transactional;

// Application service: runs the use case and owns its cross-cutting concerns (here the transaction).
// No @Service: the bean is declared in OrderConfiguration. Put @Transactional on methods, never on the class,
// so queries can be read-only.
public class PlaceOrderService implements OrderPlacement {

    private final OrderStore orderStore;

    public PlaceOrderService(OrderStore orderStore) {
        this.orderStore = orderStore;
    }

    @Override
    @Transactional
    public OrderId placeOrder(PlaceOrderCommand command) {
        var lines = command.lines().stream()
                .map(l -> new Order.Line(l.sku(), l.quantity()))
                .toList();
        var order = Order.place(lines);
        orderStore.save(order);
        return order.id();
    }
}
```

## Wiring of the hexagon (`sales/order/configuration`)

Services carry no stereotype annotation, so each one is declared with a `@Bean` method.

```java
package com.example.shop.sales.order.configuration;

import com.example.shop.sales.order.application.service.PlaceOrderService;
import com.example.shop.sales.order.application.port.in.OrderPlacement;
import com.example.shop.sales.order.application.port.out.OrderStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OrderConfiguration {

    @Bean
    OrderPlacement orderPlacement(OrderStore orderStore) {
        return new PlaceOrderService(orderStore);
    }
}
```

## Web adapter (`sales/order/adapter/in/web`)

```java
package com.example.shop.sales.order.adapter.in.web;

import com.example.shop.sales.order.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.order.application.port.in.OrderPlacement;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

// The one stereotype allowed on an adapter: Spring MVC only detects @Controller / @RestController handlers.
@RestController
@RequestMapping("/orders")
class PlaceOrderController {

    record Request(List<Line> lines) {
        record Line(String sku, int quantity) {}
    }

    record Response(UUID id) {}

    private final OrderPlacement orderPlacement;

    PlaceOrderController(OrderPlacement orderPlacement) {
        this.orderPlacement = orderPlacement;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Response place(@RequestBody Request request) {
        var command = new PlaceOrderCommand(
                request.lines().stream()
                        .map(l -> new PlaceOrderCommand.Line(l.sku(), l.quantity()))
                        .toList());
        return new Response(orderPlacement.placeOrder(command).value());
    }

    /** Self-validating commands throw IllegalArgumentException: answer HTTP 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
```

Keep error mapping as `@ExceptionHandler` methods in the controller: `@RestControllerAdvice` would be a second stereotype.

## Persistence adapter (`sales/order/adapter/out/persistence`)

```java
package com.example.shop.sales.order.adapter.out.persistence;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
class OrderJpaEntity {

    @Id
    UUID id;

    @ElementCollection
    @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
    List<OrderLineEmbeddable> lines;

    protected OrderJpaEntity() {}

    OrderJpaEntity(UUID id, List<OrderLineEmbeddable> lines) {
        this.id = id;
        this.lines = lines;
    }
}
```

```java
package com.example.shop.sales.order.adapter.out.persistence;

import jakarta.persistence.Embeddable;

@Embeddable
class OrderLineEmbeddable {
    String sku;
    int quantity;

    protected OrderLineEmbeddable() {}

    OrderLineEmbeddable(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }
}
```

```java
package com.example.shop.sales.order.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {}
```

```java
package com.example.shop.sales.order.adapter.out.persistence;

import com.example.shop.sales.order.application.port.out.OrderStore;
import com.example.shop.sales.order.domain.Order;
class OrderPersistenceAdapter implements OrderStore {

    private final SpringDataOrderRepository repository;

    OrderPersistenceAdapter(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Order order) {
        repository.save(toEntity(order));
    }

    private static OrderJpaEntity toEntity(Order order) {
        var lines = order.lines().stream()
                .map(l -> new OrderLineEmbeddable(l.sku(), l.quantity()))
                .toList();
        return new OrderJpaEntity(order.id().value(), lines);
    }
}
```

```java
package com.example.shop.sales.order.adapter.out.persistence;

import com.example.shop.sales.order.application.port.out.OrderStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OrderPersistenceAdapterConfiguration {

    @Bean
    OrderStore orderStore(SpringDataOrderRepository repository) {
        return new OrderPersistenceAdapter(repository);
    }
}
```

JPA entities and Spring Data repositories are not stereotype beans: Spring Boot finds them by scanning from the
`@SpringBootApplication` package, so they stay package-private as well.

Move mapping into a dedicated `OrderMapper` class once it grows (add `toDomain` for load ports).

## Joining two bounded contexts of the same component (`DomainAdapter`)

Both `order` and `billing` belong to the component `sales`. After an order is placed, `order` asks `billing`
to create an invoice. `order` never imports anything from `billing` except its incoming port and command,
and only inside its `DomainAdapter`.

Target context `billing`: incoming port and command (`sales/billing/application/port/in`).

```java
package com.example.shop.sales.billing.application.port.in;

public interface InvoiceCreation {
    void createInvoice(CreateInvoiceCommand command);
}
```

```java
package com.example.shop.sales.billing.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record CreateInvoiceCommand(UUID orderId, int itemCount) {
    public CreateInvoiceCommand {
        Objects.requireNonNull(orderId, "orderId must not be null");
        if (itemCount <= 0) {
            throw new IllegalArgumentException("itemCount must be positive");
        }
    }
}
```

Source context `order`: outgoing port, written in `order`'s own language (`sales/order/application/port/out`).

```java
package com.example.shop.sales.order.application.port.out;

import com.example.shop.sales.order.domain.Order;

public interface InvoiceIssuer {
    void createInvoiceFor(Order order);
}
```

Source context `order`: the `DomainAdapter` (`sales/order/adapter/out/billing`). It implements the port, calls the target's
incoming port directly and converts `order` types into the target's command.

```java
package com.example.shop.sales.order.adapter.out.billing;

import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import com.example.shop.sales.order.domain.Order;
import com.example.shop.sales.order.application.port.out.InvoiceIssuer;

class BillingDomainAdapter implements InvoiceIssuer {

    private final InvoiceCreation invoiceCreation;

    BillingDomainAdapter(InvoiceCreation invoiceCreation) {
        this.invoiceCreation = invoiceCreation;
    }

    @Override
    public void createInvoiceFor(Order order) {
        int itemCount = order.lines().stream().mapToInt(Order.Line::quantity).sum();
        invoiceCreation.createInvoice(new CreateInvoiceCommand(order.id().value(), itemCount));
    }
}
```

```java
package com.example.shop.sales.order.adapter.out.billing;

import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import com.example.shop.sales.order.application.port.out.InvoiceIssuer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OrderBillingDomainAdapterConfiguration {

    @Bean
    InvoiceIssuer invoiceIssuer(InvoiceCreation invoiceCreation) {
        return new BillingDomainAdapter(invoiceCreation);
    }
}
```

`PlaceOrderService` then takes an `InvoiceIssuer` in its constructor and calls
`invoiceIssuer.createInvoiceFor(order)` after saving. It never sees `billing`.

## Calling another component through its public API (REST example)

`shipping` is a different component. `sales/order` never imports its classes. It only knows `shipping`'s public API,
here REST, and reaches it through an outgoing port and an adapter. The API could be something else (messaging, gRPC).

```java
package com.example.shop.sales.order.application.port.out;

import com.example.shop.sales.order.domain.Order;

public interface ShipmentRequester {
    void requestShipmentFor(Order order);
}
```

```java
package com.example.shop.sales.order.adapter.out.shipping;

import com.example.shop.sales.order.domain.Order;
import com.example.shop.sales.order.application.port.out.ShipmentRequester;
import java.util.UUID;
import org.springframework.web.client.RestClient;

class ShippingRestAdapter implements ShipmentRequester {

    // Shape of the request expected by the shipping API, owned by this adapter.
    record ShipmentRequest(UUID orderId, int itemCount) {}

    private final RestClient restClient;

    ShippingRestAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void requestShipmentFor(Order order) {
        int itemCount = order.lines().stream().mapToInt(Order.Line::quantity).sum();
        restClient.post()
                .uri("/shipments")
                .body(new ShipmentRequest(order.id().value(), itemCount))
                .retrieve()
                .toBodilessEntity();
    }
}
```

```java
package com.example.shop.sales.order.adapter.out.shipping;

import com.example.shop.sales.order.application.port.out.ShipmentRequester;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class OrderShippingRestAdapterConfiguration {

    @Bean
    ShipmentRequester shipmentRequester(RestClient.Builder builder, @Value("${shipping.base-url}") String baseUrl) {
        return new ShippingRestAdapter(builder.baseUrl(baseUrl).build());
    }
}
```

Always start from the injected, auto-configured `RestClient.Builder` (dependency `spring-boot-starter-restclient`), never
from `RestClient.builder()`, `RestClient.create()` or a raw `java.net.http.HttpClient`. No extra HTTP client library is
needed: Spring Boot then uses the JDK client.

The base URL comes from `application.yaml`:

```yaml
shipping:
  base-url: https://shipping.example.com
```

The path, payload and base URL above are placeholders: use the real contract published by the target component.
Keep the `RestClient` details, error mapping and retries inside this adapter.

As with `InvoiceIssuer`, `PlaceOrderService` takes a `ShipmentRequester` in its constructor and calls
`shipmentRequester.requestShipmentFor(order)` after saving; the use case test below relies on it.

## Architecture test (ArchUnit)

Add `com.tngtech.archunit:archunit-junit5` with test scope, using the latest release from Maven Central.

```java
package com.example.shop;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

@AnalyzeClasses(packages = "com.example.shop", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] ORM_AND_JSON = {
            "jakarta.persistence..", "javax.persistence..", "org.hibernate..",
            "org.eclipse.persistence..", "org.mybatis..",
            "com.fasterxml.jackson..", "tools.jackson..", "jakarta.json..", "com.google.gson..", "org.json.."};

    // Lombok (lombok..) and JSpecify (org.jspecify..) are deliberately not forbidden anywhere.
    @ArchTest
    static final ArchRule domainAndApplicationHaveNoOrmOrJson = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(ORM_AND_JSON);

    // The domain (entities, value objects, optional domain services in domain.service) is plain Java:
    // no Spring at all, not even @Transactional, which belongs to the application services.
    @ArchTest
    static final ArchRule domainIsFrameworkFreeAndSelfContained = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..adapter..", "org.springframework..");

    @ArchTest
    static final ArchRule servicesDoNotUseAdapters = noClasses()
            .that().resideInAPackage("..application.service..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    // Application services own the cross-cutting concerns of a use case: transactions (@Transactional, with its
    // Propagation/Isolation members) and method security (@PreAuthorize, @Secured...). Nothing else from Spring:
    // no @Service or @Component, the services are created by @Bean methods.
    @ArchTest
    static final ArchRule applicationUsesSpringOnlyForCrossCuttingConcerns = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat(
                    resideInAPackage("org.springframework..")
                            .and(not(resideInAnyPackage("org.springframework.transaction.annotation",
                                    "org.springframework.security.access.prepost",
                                    "org.springframework.security.access.annotation"))));

    @ArchTest
    static final ArchRule transactionalIsNeverOnAServiceClass = noClasses()
            .that().resideInAPackage("..application.service..")
            .should().beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule commonIsIndependent = noClasses()
            .that().resideInAPackage("..common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..domain..", "..application..", "..adapter..", "com.example.shop..configuration..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    // Adapters are created in their own @Configuration class: no stereotype (except controllers), package-private.
    @ArchTest
    static final ArchRule adaptersHaveNoSpringStereotype = noClasses()
            .that().resideInAPackage("..adapter..")
            .and().areNotAnnotatedWith(Configuration.class)
            .and().areNotMetaAnnotatedWith(Controller.class)
            .should().beMetaAnnotatedWith(Component.class);

    @ArchTest
    static final ArchRule adaptersArePackagePrivate = classes()
            .that().resideInAPackage("..adapter..")
            .and().areTopLevelClasses()
            .should().notBePublic();

    @ArchTest
    static final ArchRule portsAreInterfaces = classes()
            .that().resideInAPackage("..application.port..")
            .and().areNotAssignableTo(Record.class)
            .should().beInterfaces();

    // https://www.baeldung.com/java-interface-naming-conventions: noun or adjective, no "I" prefix, no "Impl" suffix.
    @ArchTest
    static final ArchRule interfaceNamesFollowJavaConventions = noClasses()
            .that().areInterfaces()
            .should().haveSimpleNameEndingWith("Port")
            .orShould().haveSimpleNameEndingWith("UseCase")
            .orShould().haveSimpleNameEndingWith("Impl")
            .orShould().haveNameMatching(".*[.]I[A-Z][^.]*");

    @ArchTest
    static final ArchRule noImplSuffixOnClasses = noClasses()
            .should().haveSimpleNameEndingWith("Impl");

    @ArchTest
    static final ArchRule nobodyDependsOnConfigurationClasses = noClasses()
            .should().dependOnClassesThat().areAnnotatedWith(Configuration.class);

    @ArchTest
    static final ArchRule inboundAdaptersDoNotUseOutboundAdapters = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..");

    @ArchTest
    static final ArchRule outboundAdaptersDoNotUseInboundAdapters = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.in..");

    @ArchTest
    static final ArchRule nothingDependsOnConfiguration = noClasses()
            .that().resideOutsideOfPackage("com.example.shop..configuration..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop..configuration..");

    // Outgoing HTTP goes through Spring's RestClient (built from the auto-configured RestClient.Builder),
    // never through a raw HTTP client.
    @ArchTest
    static final ArchRule noRawHttpClient = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("java.net.http..", "org.apache.hc..", "okhttp3..")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("java.net.HttpURLConnection");

    // One pair of rules per source -> target bounded context, for example order -> billing.
    @ArchTest
    static final ArchRule orderReachesBillingOnlyThroughItsDomainAdapter = noClasses()
            .that().resideInAPackage("com.example.shop.sales.order..")
            .and().resideOutsideOfPackage("com.example.shop.sales.order.adapter.out.billing..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop.sales.billing..");

    @ArchTest
    static final ArchRule orderDomainAdapterUsesOnlyBillingIncomingPorts = classes()
            .that().resideInAPackage("com.example.shop.sales.order.adapter.out.billing..")
            .should().onlyDependOnClassesThat(
                    not(resideInAPackage("com.example.shop.sales.billing.."))
                            .or(resideInAPackage("com.example.shop.sales.billing.application.port.in..")));

    // Different components share no classes: they only talk through the target's public API (REST here).
    @ArchTest
    static final ArchRule salesDoesNotUseShippingClasses = noClasses()
            .that().resideInAPackage("com.example.shop.sales..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop.shipping..");
}
```

## General configuration (`com.example.shop.configuration`)

```java
package com.example.shop.configuration;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
```

Place other general `@Configuration` classes here (web, security, serialization). Component-specific wiring stays inside its component.

## Use case integration test (`*IT`)

Priority test type; the `java-testing` skill of this plugin has the full rules. Build setup (Maven):
`org.postgresql:postgresql` (runtime), and in test scope `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`,
`spring-boot-starter-data-jpa-test`, `spring-boot-testcontainers`, `org.testcontainers:testcontainers-postgresql` and,
for third-party APIs, `org.wiremock.integrations:wiremock-spring-boot` (WireMock's Spring Boot integration). Failsafe
runs the `*IT` classes in the integration-test phase:

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

```java
package com.example.shop;

@TestConfiguration(proxyBeanMethods = false)
class PostgresTestConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {   // org.testcontainers.postgresql.PostgreSQLContainer
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient   // org.springframework.boot.resttestclient.autoconfigure
@Import(PostgresTestConfiguration.class)
@interface IntegrationTest {
}
```

```java
package com.example.shop;

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
        shipping.stubFor(WireMock.post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(202)));

        client.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body("{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":2}]}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.id").isNotEmpty();
        assertThat(jdbcTemplate.queryForObject("select count(*) from orders", Integer.class)).isEqualTo(1);
        shipping.verify(1, postRequestedFor(urlEqualTo("/shipments"))
                .withRequestBody(matchingJsonPath("$.itemCount", equalTo("2"))));
    }

    @Test
    void should_store_nothing_when_the_shipping_api_fails() {
        shipping.stubFor(WireMock.post(urlEqualTo("/shipments")).willReturn(aResponse().withStatus(503)));

        client.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body("{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":1}]}")
                .exchange()
                .expectStatus().is5xxServerError();
        assertThat(jdbcTemplate.queryForObject("select count(*) from orders", Integer.class)).isZero();
    }
}
```

See `examples/single-context` (WireMock) and `examples/multi-context` (two contexts through one HTTP call) in the plugin repository for complete, CI-validated versions.

## Test examples

Unit tests follow the `java-testing` skill: AssertJ assertions and BDD Mockito notation.

Service test with a mocked outgoing port:

```java
package com.example.shop.sales.order.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

import com.example.shop.sales.order.domain.Order;
import com.example.shop.sales.order.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.order.application.port.out.OrderStore;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {

    @Mock
    private OrderStore orderStore;

    @InjectMocks
    private PlaceOrderService service;

    @Test
    void should_save_the_order_when_it_is_placed() {
        service.placeOrder(new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 2))));

        then(orderStore).should().save(any(Order.class));
    }
}
```

Command validation test:

```java
package com.example.shop.sales.order.application.port.in;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PlaceOrderCommandTest {

    @Test
    void should_reject_the_line_when_quantity_is_not_positive() {
        assertThatThrownBy(() -> new PlaceOrderCommand.Line("SKU-1", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be positive");
    }
}
```
