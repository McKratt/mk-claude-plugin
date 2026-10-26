# Templates: one vertical slice

Example slice: place an order. Base package `com.example.shop`, component `sales`, bounded context `order`.
Replace names to fit the user's domain. All code targets Java 25 and Spring Boot.
The slice below lives in a component with several bounded contexts (`sales/order`, `sales/billing`).
For a component with a single context, omit the context level: `com.example.shop.sales.application.domain.model`,
`com.example.shop.sales.adapter.in.web`, and so on. Utilities usable anywhere in the component go in
`com.example.shop.sales.common`.

## Domain (`sales/order/application/domain/model`)

```java
package com.example.shop.sales.order.application.domain.model;

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
package com.example.shop.sales.order.application.domain.model;

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

## Incoming port and command (`sales/order/application/port/in`)

```java
package com.example.shop.sales.order.application.port.in;

import com.example.shop.sales.order.application.domain.model.OrderId;

public interface PlaceOrderUseCase {
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

import com.example.shop.sales.order.application.domain.model.Order;

public interface SaveOrderPort {
    void save(Order order);
}
```

## Service (`sales/order/application/domain/service`)

```java
package com.example.shop.sales.order.application.domain.service;

import com.example.shop.sales.order.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.order.application.port.in.PlaceOrderUseCase;
import com.example.shop.sales.order.application.port.out.SaveOrderPort;
import com.example.shop.sales.order.application.domain.model.Order;
import com.example.shop.sales.order.application.domain.model.OrderId;
import org.springframework.transaction.annotation.Transactional;

// No @Service: the bean is declared in OrderConfiguration. @Transactional is the one accepted
// exception to the purity of the domain.
@Transactional
public class PlaceOrderService implements PlaceOrderUseCase {

    private final SaveOrderPort saveOrderPort;

    public PlaceOrderService(SaveOrderPort saveOrderPort) {
        this.saveOrderPort = saveOrderPort;
    }

    @Override
    public OrderId placeOrder(PlaceOrderCommand command) {
        var lines = command.lines().stream()
                .map(l -> new Order.Line(l.sku(), l.quantity()))
                .toList();
        var order = Order.place(lines);
        saveOrderPort.save(order);
        return order.id();
    }
}
```

## Wiring of the hexagon (`sales/order/configuration`)

Services carry no stereotype annotation, so each one is declared with a `@Bean` method.

```java
package com.example.shop.sales.order.configuration;

import com.example.shop.sales.order.application.domain.service.PlaceOrderService;
import com.example.shop.sales.order.application.port.in.PlaceOrderUseCase;
import com.example.shop.sales.order.application.port.out.SaveOrderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OrderConfiguration {

    @Bean
    PlaceOrderUseCase placeOrderUseCase(SaveOrderPort saveOrderPort) {
        return new PlaceOrderService(saveOrderPort);
    }
}
```

## Web adapter (`sales/order/adapter/in/web`)

```java
package com.example.shop.sales.order.adapter.in.web;

import com.example.shop.sales.order.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.order.application.port.in.PlaceOrderUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
class PlaceOrderController {

    record Request(List<Line> lines) {
        record Line(String sku, int quantity) {}
    }

    record Response(UUID id) {}

    private final PlaceOrderUseCase placeOrderUseCase;

    PlaceOrderController(PlaceOrderUseCase placeOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Response place(@RequestBody Request request) {
        var command = new PlaceOrderCommand(
                request.lines().stream()
                        .map(l -> new PlaceOrderCommand.Line(l.sku(), l.quantity()))
                        .toList());
        return new Response(placeOrderUseCase.placeOrder(command).value());
    }
}
```

Map `IllegalArgumentException` from command validation to HTTP 400 with a `@RestControllerAdvice` in this package.

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

import com.example.shop.sales.order.application.port.out.SaveOrderPort;
import com.example.shop.sales.order.application.domain.model.Order;
import org.springframework.stereotype.Component;

@Component
class OrderPersistenceAdapter implements SaveOrderPort {

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

Move mapping into a dedicated `OrderMapper` class once it grows (add `toDomain` for load ports).

## Joining two bounded contexts of the same component (`DomainAdapter`)

Both `order` and `billing` belong to the component `sales`. After an order is placed, `order` asks `billing`
to create an invoice. `order` never imports anything from `billing` except its incoming port and command,
and only inside its `DomainAdapter`.

Target context `billing`: incoming port and command (`sales/billing/application/port/in`).

```java
package com.example.shop.sales.billing.application.port.in;

public interface CreateInvoiceUseCase {
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

import com.example.shop.sales.order.application.domain.model.Order;

public interface CreateInvoicePort {
    void createInvoiceFor(Order order);
}
```

Source context `order`: the `DomainAdapter` (`sales/order/adapter/out/billing`). It implements the port, calls the target's
incoming port directly and converts `order` types into the target's command.

```java
package com.example.shop.sales.order.adapter.out.billing;

import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceUseCase;
import com.example.shop.sales.order.application.domain.model.Order;
import com.example.shop.sales.order.application.port.out.CreateInvoicePort;
import org.springframework.stereotype.Component;

@Component
class BillingDomainAdapter implements CreateInvoicePort {

    private final CreateInvoiceUseCase createInvoiceUseCase;

    BillingDomainAdapter(CreateInvoiceUseCase createInvoiceUseCase) {
        this.createInvoiceUseCase = createInvoiceUseCase;
    }

    @Override
    public void createInvoiceFor(Order order) {
        int itemCount = order.lines().stream().mapToInt(Order.Line::quantity).sum();
        createInvoiceUseCase.createInvoice(new CreateInvoiceCommand(order.id().value(), itemCount));
    }
}
```

`PlaceOrderService` then takes a `CreateInvoicePort` in its constructor and calls
`createInvoicePort.createInvoiceFor(order)` after saving. It never sees `billing`.

## Calling another component through its public API (REST example)

`shipping` is a different component. `sales/order` never imports its classes. It only knows `shipping`'s public API,
here REST, and reaches it through an outgoing port and an adapter. The API could be something else (messaging, gRPC).

```java
package com.example.shop.sales.order.application.port.out;

import com.example.shop.sales.order.application.domain.model.Order;

public interface RequestShipmentPort {
    void requestShipmentFor(Order order);
}
```

```java
package com.example.shop.sales.order.adapter.out.shipping;

import com.example.shop.sales.order.application.domain.model.Order;
import com.example.shop.sales.order.application.port.out.RequestShipmentPort;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class ShippingRestAdapter implements RequestShipmentPort {

    // Shape of the request expected by the shipping API, owned by this adapter.
    record ShipmentRequest(UUID orderId) {}

    private final RestClient restClient;

    ShippingRestAdapter(RestClient.Builder builder, @Value("${shipping.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public void requestShipmentFor(Order order) {
        restClient.post()
                .uri("/shipments")
                .body(new ShipmentRequest(order.id().value()))
                .retrieve()
                .toBodilessEntity();
    }
}
```

The path, payload and base URL above are placeholders: use the real contract published by the target component.
Keep the `RestClient` details, error mapping and retries inside this adapter.

## Architecture test (ArchUnit)

Add `com.tngtech.archunit:archunit-junit5` with test scope, using the latest release from Maven Central.

```java
package com.example.shop;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.shop", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] ORM_AND_JSON = {
            "jakarta.persistence..", "javax.persistence..", "org.hibernate..",
            "org.eclipse.persistence..", "org.mybatis..",
            "com.fasterxml.jackson..", "tools.jackson..", "jakarta.json..", "com.google.gson..", "org.json.."};

    // Lombok (lombok..) is deliberately not forbidden anywhere in the domain.
    @ArchTest
    static final ArchRule domainHasNoOrmOrJson = noClasses()
            .that().resideInAPackage("..application.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(ORM_AND_JSON);

    @ArchTest
    static final ArchRule modelIsFrameworkFreeAndSelfContained = noClasses()
            .that().resideInAPackage("..application.domain.model..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.domain.service..", "..application.port..", "..adapter..",
                    "org.springframework..");

    @ArchTest
    static final ArchRule servicesDoNotUseAdapters = noClasses()
            .that().resideInAPackage("..application.domain.service..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    // @Service and @Component are forbidden on services; only @Transactional (with its Propagation/Isolation members) is allowed.
    @ArchTest
    static final ArchRule servicesUseSpringOnlyForTransactional = noClasses()
            .that().resideInAPackage("..application.domain.service..")
            .should().dependOnClassesThat(
                    resideInAPackage("org.springframework..")
                            .and(not(resideInAPackage("org.springframework.transaction.annotation"))));

    @ArchTest
    static final ArchRule commonIsIndependent = noClasses()
            .that().resideInAPackage("..common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..adapter..", "com.example.shop..configuration..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

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

## Test examples

Service test with a mocked outgoing port:

```java
package com.example.shop.sales.order.application.domain.service;

import static org.mockito.Mockito.*;

import com.example.shop.sales.order.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.order.application.port.out.SaveOrderPort;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceOrderServiceTest {

    private final SaveOrderPort saveOrderPort = mock(SaveOrderPort.class);
    private final PlaceOrderService service = new PlaceOrderService(saveOrderPort);

    @Test
    void savesTheOrder() {
        service.placeOrder(new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 2))));

        verify(saveOrderPort).save(any());
    }
}
```

Command validation test:

```java
package com.example.shop.sales.order.application.port.in;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlaceOrderCommandTest {

    @Test
    void rejectsNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand.Line("SKU-1", 0));
    }
}
```
