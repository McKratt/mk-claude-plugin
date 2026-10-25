# Templates: one vertical slice

Example slice: place an order. Base package `com.example.shop`, component `order`.
Replace names to fit the user's domain. All code targets Java 25 and Spring Boot.

## Domain (`order/application/domain`)

```java
package com.example.shop.order.application.domain;

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
package com.example.shop.order.application.domain;

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

## Incoming port and command (`order/application/port/in`)

```java
package com.example.shop.order.application.port.in;

import com.example.shop.order.application.domain.OrderId;

public interface PlaceOrderUseCase {
    OrderId placeOrder(PlaceOrderCommand command);
}
```

```java
package com.example.shop.order.application.port.in;

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

## Outgoing port (`order/application/port/out`)

```java
package com.example.shop.order.application.port.out;

import com.example.shop.order.application.domain.Order;

public interface SaveOrderPort {
    void save(Order order);
}
```

## Service (`order/application/service`)

```java
package com.example.shop.order.application.service;

import com.example.shop.order.application.port.in.PlaceOrderCommand;
import com.example.shop.order.application.port.in.PlaceOrderUseCase;
import com.example.shop.order.application.port.out.SaveOrderPort;
import com.example.shop.order.application.domain.Order;
import com.example.shop.order.application.domain.OrderId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// @Service and @Transactional are a conscious shortcut: the application layer
// depends on Spring annotations. Document it, or wire the bean in a @Configuration class instead.
@Service
@Transactional
class PlaceOrderService implements PlaceOrderUseCase {

    private final SaveOrderPort saveOrderPort;

    PlaceOrderService(SaveOrderPort saveOrderPort) {
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

## Web adapter (`order/adapter/in/web`)

```java
package com.example.shop.order.adapter.in.web;

import com.example.shop.order.application.port.in.PlaceOrderCommand;
import com.example.shop.order.application.port.in.PlaceOrderUseCase;
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

## Persistence adapter (`order/adapter/out/persistence`)

```java
package com.example.shop.order.adapter.out.persistence;

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
package com.example.shop.order.adapter.out.persistence;

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
package com.example.shop.order.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {}
```

```java
package com.example.shop.order.adapter.out.persistence;

import com.example.shop.order.application.port.out.SaveOrderPort;
import com.example.shop.order.application.domain.Order;
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

## Architecture test (ArchUnit)

Add `com.tngtech.archunit:archunit-junit5` with test scope, using the latest release from Maven Central.

```java
package com.example.shop;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.shop", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnNothingOutward = noClasses()
            .that().resideInAPackage("..application.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.port..", "..application.service..", "..adapter..",
                    "org.springframework..", "jakarta.persistence..",
                    "com.fasterxml.jackson..", "tools.jackson..", "jakarta.json..", "com.google.gson..", "org.json..");
    // Lombok (lombok..) is deliberately not forbidden in the domain.

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
}
```

## Test examples

Service test with a mocked outgoing port:

```java
package com.example.shop.order.application.service;

import static org.mockito.Mockito.*;

import com.example.shop.order.application.port.in.PlaceOrderCommand;
import com.example.shop.order.application.port.out.SaveOrderPort;
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
package com.example.shop.order.application.port.in;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlaceOrderCommandTest {

    @Test
    void rejectsNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new PlaceOrderCommand.Line("SKU-1", 0));
    }
}
```
