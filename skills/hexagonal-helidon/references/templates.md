# Templates: one vertical slice (Helidon SE 4.x)

Example slice: place an order. Base package `com.example.shop`, component `sales` with a **single** bounded context,
so the context level is omitted from the packages (`com.example.shop.sales.application...`). With several contexts,
insert it: `com.example.shop.sales.order.application...`. Utilities usable anywhere in the component go in
`com.example.shop.sales.common`. Replace names to fit the user's domain.

Every Java file below is copied from `examples/helidon-single-context/` or, for the section on joining bounded
contexts, `examples/helidon-multi-context/` of this plugin's repository, which CI builds and tests (`mvn verify`,
Java 25, Helidon 4.5.6, PostgreSQL in Testcontainers, WireMock).

## Build (`pom.xml`)

Parent `io.helidon.applications:helidon-se`, latest **4.x** (LTS) release. The versions of Lombok, JSpecify, Flyway,
ArchUnit, AssertJ, Mockito and WireMock are not managed by the Helidon BOM: look up the latest release on Maven Central.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <!-- Helidon 4.x is the LTS line (Helidon 27+ are the short-lived "tip" releases). -->
  <parent>
    <groupId>io.helidon.applications</groupId>
    <artifactId>helidon-se</artifactId>
    <version>4.5.6</version>
    <relativePath/>
  </parent>

  <groupId>com.example.shop</groupId>
  <artifactId>shop-helidon-single-context</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <name>shop-helidon-single-context</name>
  <description>Example: Helidon SE, one component with a single bounded context (no context level in packages)</description>

  <properties>
    <mainClass>com.example.shop.ShopApplication</mainClass>
    <!-- The Helidon parent sets 21 (the Helidon 4 minimum); Java 25 is the recommended release. -->
    <maven.compiler.source>25</maven.compiler.source>
    <maven.compiler.release>25</maven.compiler.release>
    <!-- Not managed by the Helidon BOM. -->
    <lombok.version>1.18.48</lombok.version>
    <jspecify.version>1.0.1</jspecify.version>
    <flyway.version>13.11.1</flyway.version>
    <archunit.version>1.5.1</archunit.version>
    <assertj.version>3.27.7</assertj.version>
    <mockito.version>5.24.0</mockito.version>
    <wiremock.version>3.13.2</wiremock.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>io.helidon.webserver</groupId>
      <artifactId>helidon-webserver</artifactId>
    </dependency>
    <dependency>
      <groupId>io.helidon.http.media</groupId>
      <artifactId>helidon-http-media-jackson</artifactId>
    </dependency>
    <dependency>
      <groupId>io.helidon.webclient</groupId>
      <artifactId>helidon-webclient</artifactId>
    </dependency>
    <dependency>
      <groupId>io.helidon.config</groupId>
      <artifactId>helidon-config-yaml</artifactId>
    </dependency>
    <dependency>
      <groupId>io.helidon.dbclient</groupId>
      <artifactId>helidon-dbclient</artifactId>
    </dependency>
    <dependency>
      <groupId>io.helidon.dbclient</groupId>
      <artifactId>helidon-dbclient-jdbc</artifactId>
    </dependency>
    <dependency>
      <groupId>io.helidon.dbclient</groupId>
      <artifactId>helidon-dbclient-hikari</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
      <version>${flyway.version}</version>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
      <version>${flyway.version}</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.helidon.logging</groupId>
      <artifactId>helidon-logging-jul</artifactId>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-jdk14</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <version>${lombok.version}</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.jspecify</groupId>
      <artifactId>jspecify</artifactId>
      <version>${jspecify.version}</version>
    </dependency>

    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>io.helidon.webserver.testing.junit5</groupId>
      <artifactId>helidon-webserver-testing-junit5</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <!-- Same version as the Testcontainers modules the Helidon BOM manages. -->
      <version>${version.lib.testcontainers}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.wiremock</groupId>
      <artifactId>wiremock-standalone</artifactId>
      <version>${wiremock.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.assertj</groupId>
      <artifactId>assertj-core</artifactId>
      <version>${assertj.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-core</artifactId>
      <version>${mockito.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>com.tngtech.archunit</groupId>
      <artifactId>archunit-junit5</artifactId>
      <version>${archunit.version}</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-enforcer-plugin</artifactId>
        <version>3.6.3</version>
        <executions>
          <execution>
            <id>enforce-toolchain</id>
            <goals>
              <goal>enforce</goal>
            </goals>
            <configuration>
              <rules>
                <requireMavenVersion>
                  <version>[3.10.0,)</version>
                </requireMavenVersion>
                <requireJavaVersion>
                  <version>[25,)</version>
                </requireJavaVersion>
              </rules>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <annotationProcessorPaths>
            <path>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <version>${lombok.version}</version>
            </path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <!-- Mockito attaches an agent at runtime; JDK 25 may warn without this flag. -->
          <argLine>-XX:+EnableDynamicAgentLoading</argLine>
        </configuration>
      </plugin>
      <plugin>
        <!-- Tests named *IT start the web server, a PostgreSQL container and WireMock: they run in the
             integration-test phase, after packaging, not in the test phase. -->
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
      <plugin>
        <!-- Copies the runtime dependencies to target/libs, so target/<artifact>.jar runs with java -jar. -->
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
          <execution>
            <id>copy-libs</id>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

Copy also `lombok.config` (bans `@Builder`, `@Data`, `@Value`) and `.gitignore` (`target/`).

```properties
# lombok.config
# Lombok is allowed, but these three annotations are banned: compilation fails when they are used.
# Use records, explicit constructors and factory methods instead.
config.stopBubbling = true
lombok.builder.flagUsage = error
lombok.data.flagUsage = error
lombok.value.flagUsage = error
```

## Configuration (`src/main/resources/application.yaml`)

Helidon Config reads system properties, then environment variables, then `application.yaml`
(`DB_CONNECTION_URL` overrides `db.connection.url`). Flyway migrations go in `src/main/resources/db/migration`.

```yaml
# src/main/resources/application.yaml
server:
  port: 8080
  host: 0.0.0.0

# Helidon DbClient (JDBC + Hikari). Environment variables override these keys, for example DB_CONNECTION_URL.
db:
  source: jdbc
  connection:
    url: jdbc:postgresql://localhost:5432/shop
    username: shop
    password: shop

shipping:
  base-url: http://localhost:8081
  client:
    connect-timeout-millis: 2000
    read-timeout-millis: 5000
```

```properties
# src/main/resources/logging.properties (read by LogConfig.configureRuntime() in main)
handlers=io.helidon.logging.jul.HelidonConsoleHandler
java.util.logging.SimpleFormatter.format=%1$tY.%1$tm.%1$td %1$tH:%1$tM:%1$tS.%1$tL %5$s%6$s%n
.level=INFO
```

```sql
-- src/main/resources/db/migration/V1__create_orders.sql
CREATE TABLE orders (
    id        UUID PRIMARY KEY,
    placed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_lines (
    order_id UUID         NOT NULL REFERENCES orders (id),
    sku      VARCHAR(64)  NOT NULL,
    quantity INTEGER      NOT NULL
);
```

## Null-safety (JSpecify)

Mark the domain and port packages (`application/domain/model`, `application/domain/service`, `application/port/in`,
`application/port/out`) as `@NullMarked`:

```java
// sales/application/domain/model/package-info.java
@NullMarked
package com.example.shop.sales.application.domain.model;

import org.jspecify.annotations.NullMarked;
```

## Common (`sales/common`)

```java
// sales/common/Preconditions.java
package com.example.shop.sales.common;

/** Utilities usable anywhere in the sales component. */
public final class Preconditions {

    private Preconditions() {
    }

    public static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    public static String requireNotBlank(String value, String name) {
        requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
```

## Domain (`sales/application/domain/model`)

Plain Java: Lombok and JSpecify only.

```java
// sales/application/domain/model/Order.java
package com.example.shop.sales.application.domain.model;

import com.example.shop.sales.common.Preconditions;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Order {

    public record Line(String sku, int quantity) {

        public Line {
            Preconditions.requireNotBlank(sku, "sku");
            Preconditions.requirePositive(quantity, "quantity");
        }
    }

    @EqualsAndHashCode.Include
    private final OrderId id;
    private final List<Line> lines;
    private final Instant placedAt;

    /** The business rule lives here, not in the command. */
    public static Order place(List<Line> lines, Instant placedAt) {
        Preconditions.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one line");
        }
        return new Order(OrderId.newId(), List.copyOf(lines),
                Preconditions.requireNonNull(placedAt, "placedAt"));
    }

    /** Re-creates an existing order, for example when loading it from persistence. */
    public static Order restore(OrderId id, List<Line> lines, Instant placedAt) {
        return new Order(id, List.copyOf(lines), placedAt);
    }
}
```

```java
// sales/application/domain/model/OrderId.java
package com.example.shop.sales.application.domain.model;

import com.example.shop.sales.common.Preconditions;
import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        Preconditions.requireNonNull(value, "value");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }
}
```

## Incoming port and command (`sales/application/port/in`)

```java
// sales/application/port/in/OrderPlacement.java
package com.example.shop.sales.application.port.in;

import com.example.shop.sales.application.domain.model.OrderId;

public interface OrderPlacement {

    OrderId placeOrder(PlaceOrderCommand command);
}
```

```java
// sales/application/port/in/PlaceOrderCommand.java
package com.example.shop.sales.application.port.in;

import com.example.shop.sales.common.Preconditions;
import java.util.List;

/** Input model of exactly one use case. It validates itself on construction. */
public record PlaceOrderCommand(List<Line> lines) {

    public PlaceOrderCommand {
        Preconditions.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
    }

    public record Line(String sku, int quantity) {

        public Line {
            Preconditions.requireNotBlank(sku, "sku");
            Preconditions.requirePositive(quantity, "quantity");
        }
    }
}
```

## Outgoing ports (`sales/application/port/out`)

`TransactionRunner` replaces Spring's `@Transactional`: the service declares its transaction boundary through it.

```java
// sales/application/port/out/OrderStore.java
package com.example.shop.sales.application.port.out;

import com.example.shop.sales.application.domain.model.Order;

public interface OrderStore {

    void save(Order order);
}
```

```java
// sales/application/port/out/ShipmentRequester.java
package com.example.shop.sales.application.port.out;

import com.example.shop.sales.application.domain.model.Order;

/** Third-party service: asks the shipping company to deliver an order. */
public interface ShipmentRequester {

    void requestShipmentFor(Order order);
}
```

```java
// sales/application/port/out/TransactionRunner.java
package com.example.shop.sales.application.port.out;

import java.util.function.Supplier;

/**
 * Transaction boundary of a use case. Without a container there is no {@code @Transactional}: the service
 * says where the transaction starts and ends through this port, and the persistence adapter implements it.
 */
public interface TransactionRunner {

    /** Runs the work in one transaction: commits when it returns, rolls back when it throws. */
    <T> T inTransaction(Supplier<T> work);
}
```

## Service (`sales/application/domain/service`)

```java
// sales/application/domain/service/PlaceOrderService.java
package com.example.shop.sales.application.domain.service;

import com.example.shop.sales.application.domain.model.Order;
import com.example.shop.sales.application.domain.model.OrderId;
import com.example.shop.sales.application.port.in.OrderPlacement;
import com.example.shop.sales.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.application.port.out.OrderStore;
import com.example.shop.sales.application.port.out.ShipmentRequester;
import com.example.shop.sales.application.port.out.TransactionRunner;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

// Plain Java: no Helidon type, no annotation. It is created by the hexagon's configuration class and owns
// the transaction boundary through the TransactionRunner port.
@RequiredArgsConstructor
public class PlaceOrderService implements OrderPlacement {

    private final OrderStore orderStore;
    private final ShipmentRequester shipmentRequester;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    @Override
    public OrderId placeOrder(PlaceOrderCommand command) {
        var lines = command.lines().stream()
                .map(line -> new Order.Line(line.sku(), line.quantity()))
                .toList();
        return transactionRunner.inTransaction(() -> {
            var order = Order.place(lines, clock.instant());
            orderStore.save(order);
            shipmentRequester.requestShipmentFor(order);
            return order.getId();
        });
    }
}
```

## Web adapter (`sales/adapter/in/web`)

A `*Controller` implements Helidon's `HttpService`. JSON goes through `helidon-http-media-jackson`, discovered from
the classpath. The package exposes only its `*AdapterConfiguration`.

```java
// sales/adapter/in/web/PlaceOrderController.java
package com.example.shop.sales.adapter.in.web;

import com.example.shop.sales.application.port.in.OrderPlacement;
import com.example.shop.sales.application.port.in.PlaceOrderCommand;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** Web adapter: a Helidon {@link HttpService} that owns its request and response models (JSON via Jackson). */
@RequiredArgsConstructor
class PlaceOrderController implements HttpService {

    record Request(List<Line> lines) {

        record Line(String sku, int quantity) {
        }
    }

    record Response(UUID id) {
    }

    record Problem(String title, int status, String detail) {
    }

    private final OrderPlacement orderPlacement;

    @Override
    public void routing(HttpRules rules) {
        rules.post("/", this::place);
    }

    private void place(ServerRequest request, ServerResponse response) {
        var body = request.content().as(Request.class);
        var lines = body.lines() == null ? List.<Request.Line>of() : body.lines();
        try {
            var command = new PlaceOrderCommand(lines.stream()
                    .map(line -> new PlaceOrderCommand.Line(line.sku(), line.quantity()))
                    .toList());
            var id = orderPlacement.placeOrder(command).value();
            response.status(Status.CREATED_201).send(new Response(id));
        } catch (IllegalArgumentException e) {
            // Self-validating commands and domain rules throw IllegalArgumentException.
            response.status(Status.BAD_REQUEST_400)
                    .send(new Problem("Bad Request", Status.BAD_REQUEST_400.code(), e.getMessage()));
        }
    }
}
```

```java
// sales/adapter/in/web/SalesWebAdapterConfiguration.java
package com.example.shop.sales.adapter.in.web;

import com.example.shop.sales.application.port.in.OrderPlacement;
import io.helidon.webserver.http.HttpRules;

public final class SalesWebAdapterConfiguration {

    private SalesWebAdapterConfiguration() {
    }

    /** Registers the web adapters of the hexagon; they depend on incoming ports only. */
    public static void routing(HttpRules rules, OrderPlacement orderPlacement) {
        rules.register("/orders", new PlaceOrderController(orderPlacement));
    }
}
```

## Persistence adapter (`sales/adapter/out/persistence`)

DbClient (JDBC + Hikari). `DbClientTransactionRunner` implements the `TransactionRunner` port and binds the open
`DbTransaction` to the current thread; the persistence adapters of the package run their statements on
`executor()`. Note the `CAST(? AS UUID)`: DbClient binds a `UUID` as a string.

```java
// sales/adapter/out/persistence/DbClientTransactionRunner.java
package com.example.shop.sales.adapter.out.persistence;

import com.example.shop.sales.application.port.out.TransactionRunner;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.DbTransaction;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/**
 * Binds the open DbClient transaction to the current thread, so every persistence adapter of this package
 * joins it through {@link #executor()}. Helidon serves each request on its own virtual thread.
 */
@RequiredArgsConstructor
class DbClientTransactionRunner implements TransactionRunner {

    private final DbClient dbClient;
    private final ThreadLocal<DbTransaction> current = new ThreadLocal<>();

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();
        }
        var transaction = dbClient.transaction();
        current.set(transaction);
        try {
            T result = work.get();
            transaction.commit();
            return result;
        } catch (RuntimeException | Error e) {
            try {
                transaction.rollback();
            } catch (RuntimeException rollbackFailure) {
                // A failed statement already closed the connection (the pool then discards the work):
                // keep the original failure visible.
                e.addSuppressed(rollbackFailure);
            }
            throw e;
        } finally {
            current.remove();
        }
    }

    /** The current transaction, or a non-transactional executor outside of one. */
    DbExecute executor() {
        var transaction = current.get();
        return transaction != null ? transaction : dbClient.execute();
    }
}
```

```java
// sales/adapter/out/persistence/OrderPersistenceAdapter.java
package com.example.shop.sales.adapter.out.persistence;

import com.example.shop.sales.application.domain.model.Order;
import com.example.shop.sales.application.port.out.OrderStore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;

/**
 * Maps the domain order to the orders and order_lines tables. The SQL is owned by this adapter.
 * DbClient binds a UUID as a string, hence the explicit casts.
 */
@RequiredArgsConstructor
class OrderPersistenceAdapter implements OrderStore {

    private final DbClientTransactionRunner transactionRunner;

    @Override
    public void save(Order order) {
        var db = transactionRunner.executor();
        var orderId = order.getId().value();
        db.insert("INSERT INTO orders (id, placed_at) VALUES (CAST(? AS UUID), ?)",
                orderId, OffsetDateTime.ofInstant(order.getPlacedAt(), ZoneOffset.UTC));
        for (var line : order.getLines()) {
            db.insert("INSERT INTO order_lines (order_id, sku, quantity) VALUES (CAST(? AS UUID), ?, ?)",
                    orderId, line.sku(), line.quantity());
        }
    }
}
```

```java
// sales/adapter/out/persistence/SalesPersistenceAdapterConfiguration.java
package com.example.shop.sales.adapter.out.persistence;

import com.example.shop.sales.application.port.out.OrderStore;
import com.example.shop.sales.application.port.out.TransactionRunner;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import org.flywaydb.core.Flyway;

/**
 * The only public class of the package: it creates the persistence adapters, which stay package-private,
 * and hands them out as ports. Used only by the hexagon's configuration class.
 */
public final class SalesPersistenceAdapterConfiguration {

    private final DbClientTransactionRunner transactionRunner;
    private final OrderPersistenceAdapter orderPersistenceAdapter;

    /** @param dbConfig the {@code db} node of the configuration (DbClient settings) */
    public SalesPersistenceAdapterConfiguration(Config dbConfig) {
        migrate(dbConfig.get("connection"));
        transactionRunner = new DbClientTransactionRunner(DbClient.create(dbConfig));
        orderPersistenceAdapter = new OrderPersistenceAdapter(transactionRunner);
    }

    public OrderStore orderStore() {
        return orderPersistenceAdapter;
    }

    public TransactionRunner transactionRunner() {
        return transactionRunner;
    }

    /** The adapter owns its schema: Flyway applies src/main/resources/db/migration before first use. */
    private static void migrate(Config connection) {
        Flyway.configure()
                .dataSource(connection.get("url").asString().get(),
                        connection.get("username").asString().get(),
                        connection.get("password").asString().get())
                .load()
                .migrate();
    }
}
```

## Calling another component through its public API (`sales/adapter/out/shipping`)

Helidon `WebClient`, configured from the `shipping` config node. It does not throw on an error status.

```java
// sales/adapter/out/shipping/ShippingRestAdapter.java
package com.example.shop.sales.adapter.out.shipping;

import com.example.shop.sales.application.domain.model.Order;
import com.example.shop.sales.application.port.out.ShipmentRequester;
import io.helidon.http.Status;
import io.helidon.webclient.api.WebClient;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** Calls the public REST API of the shipping company. The payload is owned by this adapter. */
@RequiredArgsConstructor
class ShippingRestAdapter implements ShipmentRequester {

    record ShipmentRequest(UUID orderId, int itemCount) {
    }

    private final WebClient webClient;

    @Override
    public void requestShipmentFor(Order order) {
        int itemCount = order.getLines().stream().mapToInt(Order.Line::quantity).sum();
        try (var response = webClient.post("/shipments")
                .submit(new ShipmentRequest(order.getId().value(), itemCount))) {
            // Unlike Spring's RestClient, Helidon's WebClient does not throw on an error status.
            if (response.status().family() != Status.Family.SUCCESSFUL) {
                throw new IllegalStateException("Shipping API answered " + response.status());
            }
        }
    }
}
```

```java
// sales/adapter/out/shipping/SalesShippingRestAdapterConfiguration.java
package com.example.shop.sales.adapter.out.shipping;

import com.example.shop.sales.application.port.out.ShipmentRequester;
import io.helidon.config.Config;
import io.helidon.webclient.api.WebClient;

public final class SalesShippingRestAdapterConfiguration {

    private SalesShippingRestAdapterConfiguration() {
    }

    /** @param shippingConfig the {@code shipping} node: {@code base-url} and optional WebClient settings under {@code client} */
    public static ShipmentRequester shipmentRequester(Config shippingConfig) {
        // Helidon's WebClient, configured from Helidon Config: never a raw java.net.http.HttpClient.
        var webClient = WebClient.builder()
                .config(shippingConfig.get("client"))
                .baseUri(shippingConfig.get("base-url").asString().get())
                .build();
        return new ShippingRestAdapter(webClient);
    }
}
```

## Wiring of the hexagon (`sales/configuration`)

```java
// sales/configuration/SalesConfiguration.java
package com.example.shop.sales.configuration;

import com.example.shop.sales.adapter.in.web.SalesWebAdapterConfiguration;
import com.example.shop.sales.adapter.out.persistence.SalesPersistenceAdapterConfiguration;
import com.example.shop.sales.adapter.out.shipping.SalesShippingRestAdapterConfiguration;
import com.example.shop.sales.application.domain.service.PlaceOrderService;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRules;
import java.time.Clock;

/** Wiring of the sales hexagon: outgoing adapters, then services, then incoming adapters. No container. */
public final class SalesConfiguration {

    private SalesConfiguration() {
    }

    public static void routing(HttpRules rules, Config config, Clock clock) {
        var persistence = new SalesPersistenceAdapterConfiguration(config.get("db"));
        var shipmentRequester = SalesShippingRestAdapterConfiguration.shipmentRequester(config.get("shipping"));

        var orderPlacement = new PlaceOrderService(
                persistence.orderStore(), shipmentRequester, persistence.transactionRunner(), clock);

        SalesWebAdapterConfiguration.routing(rules, orderPlacement);
    }
}
```

## Application assembly and entry point (`com.example.shop`)

```java
// configuration/ShopConfiguration.java
package com.example.shop.configuration;

import com.example.shop.sales.configuration.SalesConfiguration;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRouting;
import java.time.Clock;

/** Application-wide assembly: shared objects, then one call per component. Used by main and by the *IT tests. */
public final class ShopConfiguration {

    private ShopConfiguration() {
    }

    public static void routing(HttpRouting.Builder routing, Config config) {
        var clock = Clock.systemUTC();
        SalesConfiguration.routing(routing, config, clock);
    }
}
```

```java
// ShopApplication.java
package com.example.shop;

import com.example.shop.configuration.ShopConfiguration;
import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;
import io.helidon.webserver.WebServer;

public final class ShopApplication {

    private ShopApplication() {
    }

    public static void main(String[] args) {
        LogConfig.configureRuntime();
        // System properties, then environment variables (DB_CONNECTION_URL...), then application.yaml.
        var config = Config.create();
        var server = WebServer.builder()
                .config(config.get("server"))
                .routing(routing -> ShopConfiguration.routing(routing, config))
                .build()
                .start();
        System.out.println("Shop is up: http://localhost:" + server.port() + "/orders");
    }
}
```

## Joining two bounded contexts of the same component (`DomainAdapter`)

From `examples/helidon-multi-context/`: component `sales` with contexts `order` and `billing`. When an order is placed,
`order` asks `billing` to create the invoice, in the same transaction. The packages gain the context level
(`com.example.shop.sales.order...`, `com.example.shop.sales.billing...`); domain, commands and ports are written as in
the slice above.

`order` defines its own outgoing port; the adapter calls `billing`'s incoming port and nothing else of it:

```java
// sales/order/application/port/out/InvoiceIssuer.java
package com.example.shop.sales.order.application.port.out;

import com.example.shop.sales.order.application.domain.model.Order;

public interface InvoiceIssuer {

    void createInvoiceFor(Order order);
}
```

```java
// sales/order/adapter/out/billing/BillingDomainAdapter.java
package com.example.shop.sales.order.adapter.out.billing;

import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import com.example.shop.sales.order.application.domain.model.Order;
import com.example.shop.sales.order.application.port.out.InvoiceIssuer;
import lombok.RequiredArgsConstructor;

/**
 * Joins the order context to the billing context of the same component: it implements order's outgoing
 * port and calls billing's incoming port directly, converting order types into billing's command.
 */
@RequiredArgsConstructor
class BillingDomainAdapter implements InvoiceIssuer {

    private final InvoiceCreation invoiceCreation;

    @Override
    public void createInvoiceFor(Order order) {
        int itemCount = order.getLines().stream().mapToInt(Order.Line::quantity).sum();
        invoiceCreation.createInvoice(new CreateInvoiceCommand(order.getId().value(), itemCount));
    }
}
```

```java
// sales/order/adapter/out/billing/OrderBillingDomainAdapterConfiguration.java
package com.example.shop.sales.order.adapter.out.billing;

import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import com.example.shop.sales.order.application.port.out.InvoiceIssuer;

public final class OrderBillingDomainAdapterConfiguration {

    private OrderBillingDomainAdapterConfiguration() {
    }

    /** @param invoiceCreation billing's incoming port, created by billing's configuration */
    public static InvoiceIssuer invoiceIssuer(InvoiceCreation invoiceCreation) {
        return new BillingDomainAdapter(invoiceCreation);
    }
}
```

### One transaction across contexts (`sales/common/persistence`)

The thread-bound transaction moves from the persistence adapter to the component's `common/persistence`, so that
billing's `CreateInvoiceService` joins the transaction opened by order's `PlaceOrderService`. Each context keeps its
own `TransactionRunner` port (same code as in the slice) and a thin adapter:

```java
// sales/common/persistence/DbTransactions.java
package com.example.shop.sales.common.persistence;

import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;
import io.helidon.dbclient.DbTransaction;
import java.util.function.Supplier;

/**
 * One DbClient for the whole component, with the open transaction bound to the current thread (Helidon serves
 * each request on its own virtual thread). The persistence adapters of every bounded context run their
 * statements on {@link #executor()}, so a use case of one context that calls another context through a
 * DomainAdapter keeps a single transaction. Only persistence adapters and configuration classes may use it.
 */
public final class DbTransactions {

    private final DbClient dbClient;
    private final ThreadLocal<DbTransaction> current = new ThreadLocal<>();

    private DbTransactions(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    /** @param dbConfig the {@code db} node of the configuration (DbClient settings) */
    public static DbTransactions create(Config dbConfig) {
        return new DbTransactions(DbClient.create(dbConfig));
    }

    /** Runs the work in the open transaction, or in a new one that commits when it returns and rolls back when it throws. */
    public <T> T inTransaction(Supplier<T> work) {
        if (current.get() != null) {
            return work.get();
        }
        var transaction = dbClient.transaction();
        current.set(transaction);
        try {
            T result = work.get();
            transaction.commit();
            return result;
        } catch (RuntimeException | Error e) {
            try {
                transaction.rollback();
            } catch (RuntimeException rollbackFailure) {
                // A failed statement already closed the connection (the pool then discards the work):
                // keep the original failure visible.
                e.addSuppressed(rollbackFailure);
            }
            throw e;
        } finally {
            current.remove();
        }
    }

    /** The current transaction, or a non-transactional executor outside of one. */
    public DbExecute executor() {
        var transaction = current.get();
        return transaction != null ? transaction : dbClient.execute();
    }
}
```

```java
// sales/billing/adapter/out/persistence/DbClientTransactionRunner.java
package com.example.shop.sales.billing.adapter.out.persistence;

import com.example.shop.sales.common.persistence.DbTransactions;
import com.example.shop.sales.billing.application.port.out.TransactionRunner;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/** The billing context's transaction port, backed by the component's shared {@link DbTransactions}. */
@RequiredArgsConstructor
class DbClientTransactionRunner implements TransactionRunner {

    private final DbTransactions dbTransactions;

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return dbTransactions.inTransaction(work);
    }
}
```

```java
// sales/billing/application/domain/service/CreateInvoiceService.java
package com.example.shop.sales.billing.application.domain.service;

import com.example.shop.sales.billing.application.domain.model.Invoice;
import com.example.shop.sales.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import com.example.shop.sales.billing.application.port.out.InvoiceStore;
import com.example.shop.sales.billing.application.port.out.TransactionRunner;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateInvoiceService implements InvoiceCreation {

    private final InvoiceStore invoiceStore;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    @Override
    public void createInvoice(CreateInvoiceCommand command) {
        transactionRunner.inTransaction(() -> {
            invoiceStore.save(Invoice.issue(command.orderId(), command.itemCount(), clock.instant()));
            return null;
        });
    }
}
```

### One schema per context

Each context migrates its own schema with its own Flyway history; the SQL names the schema, and no foreign key
crosses contexts. Reading a row: `UUID` and `TIMESTAMP WITH TIME ZONE` are read as `String` and `java.sql.Timestamp`.

```java
// sales/billing/adapter/out/persistence/BillingPersistenceAdapterConfiguration.java
package com.example.shop.sales.billing.adapter.out.persistence;

import com.example.shop.sales.billing.application.port.out.InvoiceFinder;
import com.example.shop.sales.billing.application.port.out.InvoiceStore;
import com.example.shop.sales.billing.application.port.out.TransactionRunner;
import com.example.shop.sales.common.persistence.DbTransactions;
import io.helidon.config.Config;
import org.flywaydb.core.Flyway;

/**
 * The only public class of the package: it creates the persistence adapters of the billing context, which stay
 * package-private, and hands them out as ports. Used only by the context's configuration class.
 */
public final class BillingPersistenceAdapterConfiguration {

    private final InvoicePersistenceAdapter invoicePersistenceAdapter;
    private final DbClientTransactionRunner transactionRunner;

    /**
     * @param dbConfig       the {@code db} node of the configuration, for the schema migration
     * @param dbTransactions the component's shared DbClient
     */
    public BillingPersistenceAdapterConfiguration(Config dbConfig, DbTransactions dbTransactions) {
        migrate(dbConfig.get("connection"));
        invoicePersistenceAdapter = new InvoicePersistenceAdapter(dbTransactions);
        transactionRunner = new DbClientTransactionRunner(dbTransactions);
    }

    public InvoiceStore invoiceStore() {
        return invoicePersistenceAdapter;
    }

    public InvoiceFinder invoiceFinder() {
        return invoicePersistenceAdapter;
    }

    public TransactionRunner transactionRunner() {
        return transactionRunner;
    }

    /** The context owns its schema (sales_billing) and its own Flyway history in it. */
    private static void migrate(Config connection) {
        Flyway.configure()
                .dataSource(connection.get("url").asString().get(),
                        connection.get("username").asString().get(),
                        connection.get("password").asString().get())
                .schemas("sales_billing")
                .locations("classpath:db/migration/billing")
                .load()
                .migrate();
    }
}
```

```java
// sales/billing/adapter/out/persistence/InvoicePersistenceAdapter.java
package com.example.shop.sales.billing.adapter.out.persistence;

import com.example.shop.sales.billing.application.domain.model.Invoice;
import com.example.shop.sales.billing.application.port.out.InvoiceFinder;
import com.example.shop.sales.billing.application.port.out.InvoiceStore;
import com.example.shop.sales.common.persistence.DbTransactions;
import io.helidon.dbclient.DbRow;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** Maps invoices to the billing context's schema. DbClient binds a UUID as a string, hence the explicit casts. */
@RequiredArgsConstructor
class InvoicePersistenceAdapter implements InvoiceStore, InvoiceFinder {

    private final DbTransactions dbTransactions;

    @Override
    public void save(Invoice invoice) {
        dbTransactions.executor().insert(
                "INSERT INTO sales_billing.invoices (id, order_id, item_count, issued_at)"
                        + " VALUES (CAST(? AS UUID), CAST(? AS UUID), ?, ?)",
                invoice.getId(), invoice.getOrderId(), invoice.getItemCount(),
                OffsetDateTime.ofInstant(invoice.getIssuedAt(), ZoneOffset.UTC));
    }

    @Override
    public Optional<Invoice> loadByOrderId(UUID orderId) {
        return dbTransactions.executor()
                .get("SELECT CAST(id AS VARCHAR) AS id, item_count, issued_at FROM sales_billing.invoices"
                        + " WHERE order_id = CAST(? AS UUID)", orderId)
                .map(row -> toInvoice(row, orderId));
    }

    private static Invoice toInvoice(DbRow row, UUID orderId) {
        return Invoice.restore(
                UUID.fromString(row.column("id").get(String.class)),
                orderId,
                row.column("item_count").get(Integer.class),
                row.column("issued_at").get(java.sql.Timestamp.class).toInstant());
    }
}
```

```sql
-- src/main/resources/db/migration/billing/V1__create_invoices.sql
-- No foreign key to sales_order: the billing context does not depend on the order context's tables.
CREATE TABLE invoices (
    id         UUID PRIMARY KEY,
    order_id   UUID                     NOT NULL UNIQUE,
    item_count INTEGER                  NOT NULL,
    issued_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
```

### Wiring: target context first

Each context's configuration is an instance class; the component's configuration creates `billing` before `order`
and hands billing's incoming port to order:

```java
// sales/billing/configuration/BillingConfiguration.java
package com.example.shop.sales.billing.configuration;

import com.example.shop.sales.billing.adapter.in.web.BillingWebAdapterConfiguration;
import com.example.shop.sales.billing.adapter.out.persistence.BillingPersistenceAdapterConfiguration;
import com.example.shop.sales.billing.application.domain.service.CreateInvoiceService;
import com.example.shop.sales.billing.application.domain.service.FindInvoiceByOrderService;
import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import com.example.shop.sales.billing.application.port.in.InvoiceLookup;
import com.example.shop.sales.common.persistence.DbTransactions;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRules;
import java.time.Clock;

/** Wiring of the billing hexagon. Its incoming port InvoiceCreation is handed to the order context. */
public final class BillingConfiguration {

    private final InvoiceCreation invoiceCreation;
    private final InvoiceLookup invoiceLookup;

    public BillingConfiguration(Config dbConfig, DbTransactions dbTransactions, Clock clock) {
        var persistence = new BillingPersistenceAdapterConfiguration(dbConfig, dbTransactions);
        invoiceCreation = new CreateInvoiceService(persistence.invoiceStore(), persistence.transactionRunner(), clock);
        invoiceLookup = new FindInvoiceByOrderService(persistence.invoiceFinder());
    }

    public InvoiceCreation invoiceCreation() {
        return invoiceCreation;
    }

    public void routing(HttpRules rules) {
        BillingWebAdapterConfiguration.routing(rules, invoiceLookup);
    }
}
```

```java
// sales/order/configuration/OrderConfiguration.java
package com.example.shop.sales.order.configuration;

import com.example.shop.sales.billing.application.port.in.InvoiceCreation;
import com.example.shop.sales.common.persistence.DbTransactions;
import com.example.shop.sales.order.adapter.in.web.OrderWebAdapterConfiguration;
import com.example.shop.sales.order.adapter.out.billing.OrderBillingDomainAdapterConfiguration;
import com.example.shop.sales.order.adapter.out.persistence.OrderPersistenceAdapterConfiguration;
import com.example.shop.sales.order.application.domain.service.PlaceOrderService;
import com.example.shop.sales.order.application.port.in.OrderPlacement;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRules;
import java.time.Clock;

/** Wiring of the order hexagon. Billing is reached through billing's incoming port, given by the component. */
public final class OrderConfiguration {

    private final OrderPlacement orderPlacement;

    public OrderConfiguration(Config dbConfig, DbTransactions dbTransactions, Clock clock,
            InvoiceCreation invoiceCreation) {
        var persistence = new OrderPersistenceAdapterConfiguration(dbConfig, dbTransactions);
        var invoiceIssuer = OrderBillingDomainAdapterConfiguration.invoiceIssuer(invoiceCreation);
        orderPlacement = new PlaceOrderService(
                persistence.orderStore(), invoiceIssuer, persistence.transactionRunner(), clock);
    }

    public void routing(HttpRules rules) {
        OrderWebAdapterConfiguration.routing(rules, orderPlacement);
    }
}
```

```java
// sales/configuration/SalesConfiguration.java
package com.example.shop.sales.configuration;

import com.example.shop.sales.billing.configuration.BillingConfiguration;
import com.example.shop.sales.common.persistence.DbTransactions;
import com.example.shop.sales.order.configuration.OrderConfiguration;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRules;
import java.time.Clock;

/**
 * Wiring of the sales component: one shared DbClient, then the target context (billing) before the context
 * that calls it (order), which receives billing's incoming port.
 */
public final class SalesConfiguration {

    private SalesConfiguration() {
    }

    public static void routing(HttpRules rules, Config config, Clock clock) {
        var dbConfig = config.get("db");
        var dbTransactions = DbTransactions.create(dbConfig);

        var billing = new BillingConfiguration(dbConfig, dbTransactions, clock);
        var order = new OrderConfiguration(dbConfig, dbTransactions, clock, billing.invoiceCreation());

        billing.routing(rules);
        order.routing(rules);
    }
}
```

### Architecture rules added for several contexts

Added to the `ArchitectureTest` below (with `import static com.tngtech.archunit.base.DescribedPredicate.not;` and
`import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;`), each with a fixture in
`ArchitectureRulesDetectViolationsTest` (for example `com.example.shop.sales.order.fixture.ReachesIntoBilling`, a test
class of order holding a billing domain object):

```java
    // The component's shared DbClient (common.persistence) is technology: only persistence adapters and
    // configuration classes may use it, never the application.
    @ArchTest
    static final ArchRule sharedPersistenceIsUsedOnlyByPersistenceAdapters = noClasses()
            .that().resideOutsideOfPackages("..adapter.out.persistence..", "..configuration..", "..common.persistence..")
            .should().dependOnClassesThat().resideInAPackage("..common.persistence..");

    // Bounded contexts of the same component: order -> billing only through the DomainAdapter (and the
    // configuration, which wires it), and only through billing's incoming ports.
    @ArchTest
    static final ArchRule orderReachesBillingOnlyThroughItsDomainAdapter = noClasses()
            .that().resideInAPackage("com.example.shop.sales.order..")
            .and().resideOutsideOfPackages("com.example.shop.sales.order.adapter.out.billing..",
                    "com.example.shop.sales.order.configuration..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop.sales.billing..");

    @ArchTest
    static final ArchRule orderUsesOnlyBillingIncomingPorts = classes()
            .that().resideInAPackage("com.example.shop.sales.order..")
            .should().onlyDependOnClassesThat(
                    not(resideInAPackage("com.example.shop.sales.billing.."))
                            .or(resideInAPackage("com.example.shop.sales.billing.application.port.in..")));

    @ArchTest
    static final ArchRule billingDoesNotUseOrder = noClasses()
            .that().resideInAPackage("com.example.shop.sales.billing..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop.sales.order..");

```

### Proving the shared transaction (`*IT`)

A deferred constraint trigger makes order's commit fail after the invoice was written: with one transaction per
context, the invoice would stay. The full test is `examples/helidon-multi-context/src/test/java/com/example/shop/OrderToInvoiceIT.java`.

```java
    /**
     * The invoice is written before order's transaction commits: a failure at commit time must take the
     * invoice with it. With one transaction per context, billing's would already be committed.
     */
    @Test
    void should_store_no_invoice_when_the_order_transaction_fails_at_commit() {
        DB.execute().dml("CREATE OR REPLACE FUNCTION sales_order.fail_at_commit() RETURNS trigger"
                + " LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'commit refused'; END $$");
        DB.execute().dml("CREATE CONSTRAINT TRIGGER fail_at_commit AFTER INSERT ON sales_order.orders"
                + " DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION sales_order.fail_at_commit()");
        try (var response = postOrder("{\"lines\":[{\"sku\":\"SKU-1\",\"quantity\":1}]}")) {
            assertThat(response.status().family()).isEqualTo(Status.Family.SERVER_ERROR);
        } finally {
            DB.execute().dml("DROP TRIGGER fail_at_commit ON sales_order.orders");
        }

        assertThat(count("sales_order.orders")).isZero();
        assertThat(count("sales_billing.invoices")).isZero();
    }
```

## Architecture test (ArchUnit)

`ImportOption.DoNotIncludeTests` keeps the fixtures out of the analyzed classes.

```java
// src/test/java/com/example/shop/ArchitectureTest.java
package com.example.shop;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.shop", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String[] ORM_AND_JSON = {
            "jakarta.persistence..", "javax.persistence..", "org.hibernate..",
            "org.eclipse.persistence..", "org.mybatis..",
            "com.fasterxml.jackson..", "tools.jackson..", "jakarta.json..", "com.google.gson..", "org.json.."};

    // Lombok (lombok..) and JSpecify (org.jspecify..) are deliberately not forbidden anywhere in the application.
    @ArchTest
    static final ArchRule domainHasNoOrmOrJson = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(ORM_AND_JSON);

    // Helidon SE has no @Transactional to tolerate: the whole application (domain and ports) is free of
    // Helidon and of any container API. Transactions go through an outgoing port.
    @ArchTest
    static final ArchRule applicationIsFrameworkFree = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "io.helidon..", "jakarta.inject..", "jakarta.enterprise..", "jakarta.ws.rs..",
                    "org.springframework..");

    @ArchTest
    static final ArchRule modelIsSelfContained = noClasses()
            .that().resideInAPackage("..application.domain.model..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application.domain.service..", "..application.port..", "..adapter..");

    @ArchTest
    static final ArchRule servicesDoNotUseAdapters = noClasses()
            .that().resideInAPackage("..application.domain.service..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule commonIsIndependent = noClasses()
            .that().resideInAPackage("..common..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..", "..configuration..");

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

    // Without a container, the configuration classes are the composition root: only other configuration
    // classes and the main class may call them.
    @ArchTest
    static final ArchRule onlyTheCompositionRootUsesConfiguration = noClasses()
            .that().resideOutsideOfPackage("..configuration..")
            .and().haveSimpleNameNotEndingWith("Configuration")
            .and().doNotHaveFullyQualifiedName("com.example.shop.ShopApplication")
            .should().dependOnClassesThat().resideInAPackage("..configuration..")
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("AdapterConfiguration");

    // Each adapter package exposes one public *AdapterConfiguration that creates its adapters, so every
    // adapter class stays package-private.
    @ArchTest
    static final ArchRule adaptersArePackagePrivate = classes()
            .that().resideInAPackage("..adapter..")
            .and().areTopLevelClasses()
            .and().haveSimpleNameNotEndingWith("AdapterConfiguration")
            .should().notBePublic();

    // Outgoing HTTP goes through Helidon's WebClient, never through a raw HTTP client.
    @ArchTest
    static final ArchRule noRawHttpClient = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("java.net.http..", "org.apache.hc..", "okhttp3..")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("java.net.HttpURLConnection");

    @ArchTest
    static final ArchRule portsAreInterfaces = classes()
            .that().resideInAPackage("..application.port..")
            .and().areNotAssignableTo(Record.class)
            .should().beInterfaces();

    // Interface names follow the usual Java conventions (https://www.baeldung.com/java-interface-naming-conventions):
    // a noun or an adjective, no "I" prefix, no "Impl" suffix, and no Port / UseCase suffix.
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
}
```

Prove that the rules still match: deliberately bad classes in the test sources (package `archfixtures`, for example
a model class holding an `io.helidon.config.Config`, a public adapter, an adapter using a configuration class,
an `IThing` interface) and one test per rule.

```java
// src/test/java/com/example/shop/ArchitectureRulesDetectViolationsTest.java
package com.example.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Checks that the rules in {@link ArchitectureTest} really fail on bad code. The bad classes live in the
 * test sources (package archfixtures) and are not part of the analyzed application.
 */
class ArchitectureRulesDetectViolationsTest {

    private static final JavaClasses VIOLATIONS = new ClassFileImporter().importPackages("archfixtures");

    private static void assertViolation(ArchRule rule, JavaClasses classes) {
        assertThat(rule.evaluate(classes).hasViolation())
                .as("Expected a violation for rule: " + rule.getDescription())
                .isTrue();
    }

    @Test
    void should_detect_a_violation_when_the_model_uses_helidon() {
        assertViolation(ArchitectureTest.applicationIsFrameworkFree, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_the_domain_uses_json() {
        assertViolation(ArchitectureTest.domainHasNoOrmOrJson, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_a_service_uses_an_adapter() {
        assertViolation(ArchitectureTest.servicesDoNotUseAdapters, VIOLATIONS);
        assertViolation(ArchitectureTest.applicationDoesNotDependOnAdapters, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_common_uses_the_application() {
        assertViolation(ArchitectureTest.commonIsIndependent, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_an_inbound_adapter_uses_an_outbound_adapter() {
        assertViolation(ArchitectureTest.inboundAdaptersDoNotUseOutboundAdapters, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_an_adapter_uses_a_configuration_class() {
        assertViolation(ArchitectureTest.onlyTheCompositionRootUsesConfiguration, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_an_adapter_is_public() {
        assertViolation(ArchitectureTest.adaptersArePackagePrivate, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_an_interface_is_named_with_a_port_suffix_or_i_prefix() {
        assertViolation(ArchitectureTest.interfaceNamesFollowJavaConventions, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_a_class_has_an_impl_suffix() {
        assertViolation(ArchitectureTest.noImplSuffixOnClasses, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_a_raw_http_client_is_used() {
        assertViolation(ArchitectureTest.noRawHttpClient, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_a_port_is_not_an_interface() {
        assertViolation(ArchitectureTest.portsAreInterfaces, VIOLATIONS);
    }
}
```

## Use case integration test (`*IT`)

Shared infrastructure, started once per JVM:

```java
// src/test/java/com/example/shop/TestInfrastructure.java
package com.example.shop;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.dbclient.DbClient;
import java.util.Map;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared by every *IT test of the JVM: a real PostgreSQL in a container (Testcontainers) and WireMock for the
 * third-party shipping API, both on random ports. Started once, stopped by Testcontainers' Ryuk and the JVM.
 */
final class TestInfrastructure {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    static final WireMockServer SHIPPING = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        POSTGRES.start();
        SHIPPING.start();
    }

    private TestInfrastructure() {
    }

    /** The application's configuration, with the containers' addresses taking precedence over application.yaml. */
    static Config config() {
        return Config.builder()
                .addSource(ConfigSources.create(Map.of(
                        "db.connection.url", POSTGRES.getJdbcUrl(),
                        "db.connection.username", POSTGRES.getUsername(),
                        "db.connection.password", POSTGRES.getPassword(),
                        "shipping.base-url", SHIPPING.baseUrl())))
                .addSource(ConfigSources.classpath("application.yaml"))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();
    }

    /** Direct database access for the tests' setup and assertions. */
    static DbClient dbClient() {
        return DbClient.create(config().get("db"));
    }
}
```

```java
// src/test/java/com/example/shop/PlaceOrderIT.java
package com.example.shop;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.shop.configuration.ShopConfiguration;
import io.helidon.dbclient.DbClient;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The "place an order" use case through the whole stack: real Helidon web server on a random port, the
 * application's own wiring, real PostgreSQL (Testcontainers) and the shipping API replaced by WireMock.
 */
@ServerTest
class PlaceOrderIT {

    private static final DbClient DB = TestInfrastructure.dbClient();

    private final Http1Client client;

    PlaceOrderIT(Http1Client client) {
        this.client = client;
    }

    /** The same assembly as main, fed with the test configuration. */
    @SetUpRoute
    static void routing(HttpRouting.Builder routing) {
        ShopConfiguration.routing(routing, TestInfrastructure.config());
    }

    @BeforeEach
    void clean_database_and_stubs() {
        DB.execute().delete("DELETE FROM order_lines");
        DB.execute().delete("DELETE FROM orders");
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

    private io.helidon.webclient.http1.Http1ClientResponse postOrder(String json) {
        return client.post("/orders")
                .header(HeaderNames.CONTENT_TYPE, "application/json")
                .submit(json);
    }

    private static long count(String table) {
        return DB.execute().get("SELECT count(*) AS n FROM " + table)
                .orElseThrow()
                .column("n").get(Long.class);
    }
}
```

## Unit tests

Domain and command tests are plain JUnit + AssertJ. A service test mocks the outgoing ports (BDD Mockito) and runs
the work without a transaction:

```java
// src/test/java/com/example/shop/sales/application/domain/service/PlaceOrderServiceTest.java
package com.example.shop.sales.application.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;

import com.example.shop.sales.application.domain.model.Order;
import com.example.shop.sales.application.port.in.PlaceOrderCommand;
import com.example.shop.sales.application.port.out.OrderStore;
import com.example.shop.sales.application.port.out.ShipmentRequester;
import com.example.shop.sales.application.port.out.TransactionRunner;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlaceOrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    /** Runs the work directly: the real transaction is covered by PlaceOrderIT. */
    private static final TransactionRunner NO_TRANSACTION = new TransactionRunner() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    private final OrderStore orderStore = mock(OrderStore.class);
    private final ShipmentRequester shipmentRequester = mock(ShipmentRequester.class);
    private final PlaceOrderService service =
            new PlaceOrderService(orderStore, shipmentRequester, NO_TRANSACTION, CLOCK);

    @Test
    void should_save_the_order_with_the_current_instant_when_it_is_placed() {
        var orderId = service.placeOrder(
                new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 2))));

        var captor = ArgumentCaptor.forClass(Order.class);
        then(orderStore).should().save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(orderId);
        assertThat(captor.getValue().getPlacedAt()).isEqualTo(NOW);
    }

    @Test
    void should_request_a_shipment_when_the_order_is_placed() {
        service.placeOrder(new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 1))));

        then(shipmentRequester).should().requestShipmentFor(any(Order.class));
    }
}
```
