# Templates: one vertical slice (Helidon SE 4.x)

Example slice: place an order. Base package `com.example.shop` with a **single** bounded context (the sales domain),
so the hexagon sits directly in the base package (`com.example.shop.domain`, `com.example.shop.application...`).
With several contexts, each gets its package directly under the base package: `com.example.shop.order.application...`.
There is one `com.example.shop.common` and one `com.example.shop.configuration` in both cases.
Replace names to fit the user's domain.

Every Java file below is copied from `examples/helidon-single-context/` or, for the section on joining bounded
contexts, `examples/helidon-multi-context/` of the McKratt/mk-claude-plugin repository, which CI builds and tests (`mvn verify`,
Java 25, Helidon 4.5.6, PostgreSQL in Testcontainers, WireMock), except the domain service section, marked
*not in the examples*.

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
  <description>Example: Helidon SE, a single bounded context: the hexagon sits directly in the base package</description>

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
      <!-- Connection pool; version managed by the Helidon BOM. -->
      <groupId>com.zaxxer</groupId>
      <artifactId>HikariCP</artifactId>
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
  # Graceful shutdown: running requests get up to 30 seconds to complete before the connection pool is closed.
  shutdown-grace-period: PT30S

# Plain JDBC through a HikariCP connection pool. Environment variables override these keys, for example
# DB_CONNECTION_URL.
db:
  connection:
    url: jdbc:postgresql://localhost:5432/shop
    username: shop
    password: shop
  pool:
    maximum-pool-size: 10

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

Mark the domain and port packages (`domain`, `application/service`, `application/port/in`,
`application/port/out`) as `@NullMarked`:

```java
// domain/package-info.java
@NullMarked
package com.example.shop.domain;

import org.jspecify.annotations.NullMarked;
```

## Common (`common`)

```java
// common/Preconditions.java
package com.example.shop.common;

/** Utilities usable anywhere in the application (all bounded contexts and layers). */
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

## Domain (`domain`)

Plain Java: Lombok and JSpecify only.

```java
// domain/Order.java
package com.example.shop.domain;

import com.example.shop.common.Preconditions;
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
// domain/OrderId.java
package com.example.shop.domain;

import com.example.shop.common.Preconditions;
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

## Domain service (optional, `domain/service`): *not in the examples*

Only when a domain operation does not belong to a single entity or value object, typically when it needs several
aggregates. Plain Java, stateless, named after the operation, no transaction, security or port: the application
service loads the aggregates through its ports, calls the domain service and saves the result. `Customer` below
stands for a second aggregate of the context.

```java
// domain/service/LoyaltyDiscount.java
package com.example.shop.domain.service;

import com.example.shop.domain.Customer;
import com.example.shop.domain.Order;

/** Discount rule that needs two aggregates, so it belongs to neither. */
public final class LoyaltyDiscount {

    /** @return the discount in percent: 5 for a customer with at least 10 previous orders, else 0 */
    public int percentFor(Order order, Customer customer) {
        return customer.previousOrderCount() >= 10 && !order.getLines().isEmpty() ? 5 : 0;
    }
}
```

## Incoming port and command (`application/port/in`)

```java
// application/port/in/OrderPlacement.java
package com.example.shop.application.port.in;

import com.example.shop.domain.OrderId;

public interface OrderPlacement {

    OrderId placeOrder(PlaceOrderCommand command);
}
```

```java
// application/port/in/PlaceOrderCommand.java
package com.example.shop.application.port.in;

import com.example.shop.common.Preconditions;
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

## Outgoing ports (`application/port/out`)

`TransactionRunner` replaces Spring's `@Transactional`: the service declares its transaction boundary through it.

```java
// application/port/out/OrderStore.java
package com.example.shop.application.port.out;

import com.example.shop.domain.Order;

public interface OrderStore {

    void save(Order order);
}
```

```java
// application/port/out/ShipmentRequester.java
package com.example.shop.application.port.out;

import com.example.shop.domain.Order;

/** Third-party service: asks the shipping company to deliver an order. */
public interface ShipmentRequester {

    void requestShipmentFor(Order order);
}
```

```java
// application/port/out/TransactionRunner.java
package com.example.shop.application.port.out;

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

## Application service (`application/service`)

```java
// application/service/PlaceOrderService.java
package com.example.shop.application.service;

import com.example.shop.domain.Order;
import com.example.shop.domain.OrderId;
import com.example.shop.application.port.in.OrderPlacement;
import com.example.shop.application.port.in.PlaceOrderCommand;
import com.example.shop.application.port.out.OrderStore;
import com.example.shop.application.port.out.ShipmentRequester;
import com.example.shop.application.port.out.TransactionRunner;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

// Application service: plain Java, no Helidon type, no annotation. It is created by the domain's configuration
// class, runs the use case and owns its cross-cutting concerns:
// the transaction boundary, through the TransactionRunner port.
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

## Web adapter (`adapter/in/web`)

A `*Controller` implements Helidon's `HttpService`. JSON goes through `helidon-http-media-jackson`, discovered from
the classpath. The package exposes only its `*AdapterConfiguration`.

```java
// adapter/in/web/PlaceOrderController.java
package com.example.shop.adapter.in.web;

import com.example.shop.application.port.in.OrderPlacement;
import com.example.shop.application.port.in.PlaceOrderCommand;
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
// adapter/in/web/SalesWebAdapterConfiguration.java
package com.example.shop.adapter.in.web;

import com.example.shop.application.port.in.OrderPlacement;
import io.helidon.webserver.http.HttpRules;

public final class SalesWebAdapterConfiguration {

    private SalesWebAdapterConfiguration() {
    }

    /** Registers the web adapters of the domain; they depend on incoming ports only. */
    public static void routing(HttpRules rules, OrderPlacement orderPlacement) {
        rules.register("/orders", new PlaceOrderController(orderPlacement));
    }
}
```

## Persistence adapter (`adapter/out/persistence`)

Plain JDBC on a HikariCP pool (`HikariDataSource`), created from the `db` config node by the adapter configuration,
which also hands it to Flyway. `JdbcTransactionRunner` implements the `TransactionRunner` port: it takes a connection
from the pool, turns auto-commit off and binds the connection to the work with a `ScopedValue` (final in Java 25);
the persistence adapters of the package run their statements through `run(...)` / `call(...)`, which use the bound
connection, or a pooled connection in auto-commit outside of a transaction. A `SQLException` is rethrown as an
`IllegalStateException` (not an `IllegalArgumentException`, which the web adapters turn into a `400`). The PostgreSQL
driver binds `UUID` and `OffsetDateTime` natively with `setObject`, so the SQL needs no cast.

```java
// adapter/out/persistence/JdbcTransactionRunner.java
package com.example.shop.adapter.out.persistence;

import com.example.shop.application.port.out.TransactionRunner;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;

/**
 * Runs the work in one JDBC transaction on a connection of the Hikari pool. The connection is bound to the work
 * with a {@link ScopedValue}, so every persistence adapter of this package joins it through {@link #run} and
 * {@link #call}.
 */
@RequiredArgsConstructor
class JdbcTransactionRunner implements TransactionRunner {

    private final DataSource dataSource;
    private final ScopedValue<Connection> current = ScopedValue.newInstance();

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        if (current.isBound()) {
            return work.get();
        }
        // Closing hands the connection back to Hikari, which rolls back uncommitted work and restores auto-commit.
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            T result;
            try {
                result = ScopedValue.where(current, connection).call(work::get);
            } catch (RuntimeException | Error e) {
                rollback(connection, e);
                throw e;
            }
            connection.commit();
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Transaction failed: " + e.getMessage(), e);
        }
    }

    /** Runs the statements on the connection of the current transaction, or on a pooled connection in auto-commit. */
    <T> T call(SqlFunction<T> statements) {
        try {
            if (current.isBound()) {
                return statements.apply(current.get());
            }
            try (var connection = dataSource.getConnection()) {
                return statements.apply(connection);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL failed: " + e.getMessage(), e);
        }
    }

    void run(SqlConsumer statements) {
        call(connection -> {
            statements.accept(connection);
            return null;
        });
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            // Keep the original failure visible.
            failure.addSuppressed(rollbackFailure);
        }
    }

    @FunctionalInterface
    interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
```

```java
// adapter/out/persistence/OrderPersistenceAdapter.java
package com.example.shop.adapter.out.persistence;

import com.example.shop.domain.Order;
import com.example.shop.application.port.out.OrderStore;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;

/**
 * Maps the domain order to the orders and order_lines tables with plain JDBC. The SQL is owned by this adapter.
 * The PostgreSQL driver binds UUID and OffsetDateTime natively through setObject.
 */
@RequiredArgsConstructor
class OrderPersistenceAdapter implements OrderStore {

    private final JdbcTransactionRunner transactionRunner;

    @Override
    public void save(Order order) {
        var orderId = order.getId().value();
        transactionRunner.run(connection -> {
            try (var insertOrder = connection.prepareStatement("INSERT INTO orders (id, placed_at) VALUES (?, ?)");
                    var insertLine = connection.prepareStatement(
                            "INSERT INTO order_lines (order_id, sku, quantity) VALUES (?, ?, ?)")) {
                insertOrder.setObject(1, orderId);
                insertOrder.setObject(2, OffsetDateTime.ofInstant(order.getPlacedAt(), ZoneOffset.UTC));
                insertOrder.executeUpdate();
                for (var line : order.getLines()) {
                    insertLine.setObject(1, orderId);
                    insertLine.setString(2, line.sku());
                    insertLine.setInt(3, line.quantity());
                    insertLine.addBatch();
                }
                insertLine.executeBatch();
            }
        });
    }
}
```

```java
// adapter/out/persistence/SalesPersistenceAdapterConfiguration.java
package com.example.shop.adapter.out.persistence;

import com.example.shop.application.port.out.OrderStore;
import com.example.shop.application.port.out.TransactionRunner;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.helidon.config.Config;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * The only public class of the package: it creates the Hikari connection pool and the persistence adapters,
 * which stay package-private, and hands them out as ports. Used only by the domain's configuration class, which
 * closes it when the server stops.
 */
public final class SalesPersistenceAdapterConfiguration implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final JdbcTransactionRunner transactionRunner;
    private final OrderPersistenceAdapter orderPersistenceAdapter;

    /** @param dbConfig the {@code db} node of the configuration (connection and pool settings) */
    public SalesPersistenceAdapterConfiguration(Config dbConfig) {
        dataSource = dataSource(dbConfig);
        try {
            migrate(dataSource);
        } catch (RuntimeException e) {
            // The caller gets no instance to close.
            dataSource.close();
            throw e;
        }
        transactionRunner = new JdbcTransactionRunner(dataSource);
        orderPersistenceAdapter = new OrderPersistenceAdapter(transactionRunner);
    }

    public OrderStore orderStore() {
        return orderPersistenceAdapter;
    }

    public TransactionRunner transactionRunner() {
        return transactionRunner;
    }

    /** Closes the connection pool. */
    @Override
    public void close() {
        dataSource.close();
    }

    private static HikariDataSource dataSource(Config dbConfig) {
        var connection = dbConfig.get("connection");
        var hikari = new HikariConfig();
        hikari.setPoolName("shop");
        hikari.setJdbcUrl(connection.get("url").asString().get());
        hikari.setUsername(connection.get("username").asString().get());
        hikari.setPassword(connection.get("password").asString().get());
        dbConfig.get("pool").get("maximum-pool-size").asInt().ifPresent(hikari::setMaximumPoolSize);
        return new HikariDataSource(hikari);
    }

    /** The adapter owns its schema: Flyway applies src/main/resources/db/migration before first use. */
    private static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
    }
}
```

## Calling another application through its public API (`adapter/out/shipping`)

Helidon `WebClient`, configured from the `shipping` config node. It does not throw on an error status.

```java
// adapter/out/shipping/ShippingRestAdapter.java
package com.example.shop.adapter.out.shipping;

import com.example.shop.domain.Order;
import com.example.shop.application.port.out.ShipmentRequester;
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
// adapter/out/shipping/SalesShippingRestAdapterConfiguration.java
package com.example.shop.adapter.out.shipping;

import com.example.shop.application.port.out.ShipmentRequester;
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

## Wiring of the hexagon (`configuration`)

```java
// configuration/SalesConfiguration.java
package com.example.shop.configuration;

import com.example.shop.adapter.in.web.SalesWebAdapterConfiguration;
import com.example.shop.adapter.out.persistence.SalesPersistenceAdapterConfiguration;
import com.example.shop.adapter.out.shipping.SalesShippingRestAdapterConfiguration;
import com.example.shop.application.service.PlaceOrderService;
import io.helidon.config.Config;
import io.helidon.webserver.http.HttpRouting;
import java.time.Clock;

/**
 * Wiring of the sales domain: outgoing adapters, then services, then incoming adapters. No container. The connection
 * pool is closed when the server stops.
 */
public final class SalesConfiguration {

    private SalesConfiguration() {
    }

    static void routing(HttpRouting.Builder routing, Config config, Clock clock, ApplicationResources resources) {
        // Before the pool: a wrong setting fails the start without opening connections.
        var shipmentRequester = SalesShippingRestAdapterConfiguration.shipmentRequester(config.get("shipping"));
        var persistence = new SalesPersistenceAdapterConfiguration(config.get("db"));
        resources.add(persistence);

        var orderPlacement = new PlaceOrderService(
                persistence.orderStore(), shipmentRequester, persistence.transactionRunner(), clock);

        SalesWebAdapterConfiguration.routing(routing, orderPlacement);
    }
}
```

## Graceful shutdown of the connection pool (`configuration`)

The configuration that creates the pool (here `SalesConfiguration`; with several contexts, `ShopConfiguration`,
which creates `DbTransactions`) adds it to `ApplicationResources`, and `ShopConfiguration.start` registers those on the
routing through `CloseOnStop`, an `HttpFeature` without routes, so Helidon closes them in `afterStop()`. On stop, Helidon 4 first stops accepting connections, lets running requests complete for at most
`server.shutdown-grace-period` (default `PT0.5S`; `PT30S` in `application.yaml`, so keep the platform's termination
grace period above it, for example Kubernetes' `terminationGracePeriodSeconds`, 30 by default), then calls `afterStop()` on the
routing and its features. The server registers a JVM shutdown hook by default (`server.shutdown-hook: true`), so
`SIGTERM` triggers this sequence, and `RunningApplication.close()`, which the `*IT` tests call, runs the same one.
`main` therefore registers no shutdown hook of its own: a second hook would run concurrently with the server's.

```java
// configuration/CloseOnStop.java
package com.example.shop.configuration;

import io.helidon.webserver.http.HttpFeature;
import io.helidon.webserver.http.HttpRouting;
import lombok.RequiredArgsConstructor;

/**
 * Closes a resource, such as the connection pool, when the web server stops gracefully. Helidon calls
 * {@link #afterStop()} once the server accepts no more connections and the running requests have completed or the
 * shutdown grace period ({@code server.shutdown-grace-period}) has elapsed. The server's shutdown hook, on by
 * default, stops it when the JVM exits.
 */
@RequiredArgsConstructor
final class CloseOnStop implements HttpFeature {

    private static final System.Logger LOGGER = System.getLogger(CloseOnStop.class.getName());

    private final AutoCloseable resource;

    @Override
    public void setup(HttpRouting.Builder routing) {
        // No route: the feature only takes part in the server's lifecycle.
    }

    @Override
    public void afterStop() {
        try {
            resource.close();
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to close " + resource, e);
        }
    }
}
```

A failed start never reaches `afterStop()`, which is why the resources are held outside the feature: `start` closes
them itself when the startup fails.

```java
// configuration/ApplicationResources.java
package com.example.shop.configuration;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * What the application must release, such as the connection pool, closed in the reverse order of creation. It covers
 * both ends of the application's life: {@link CloseOnStop} closes it when the server stops, and
 * {@link ShopConfiguration#start} closes it when the startup fails, before there is a server to stop.
 */
final class ApplicationResources implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(ApplicationResources.class.getName());

    private final Deque<AutoCloseable> resources = new ConcurrentLinkedDeque<>();

    void add(AutoCloseable resource) {
        resources.addFirst(resource);
    }

    /** Closes each resource once: a failure is logged and does not prevent closing the others. */
    @Override
    public void close() {
        AutoCloseable resource;
        while ((resource = resources.pollFirst()) != null) {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Failed to close " + resource, e);
            }
        }
    }
}
```

The lifecycle is checked against a real `WebServer` (no database needed, run by surefire):

```java
// src/test/java/com/example/shop/configuration/CloseOnStopTest.java
package com.example.shop.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class CloseOnStopTest {

    @Test
    void should_close_the_resource_when_the_server_stops() {
        var closed = new AtomicBoolean();
        var server = WebServer.builder()
                .port(0)
                .routing(routing -> routing.addFeature(new CloseOnStop(() -> closed.set(true))))
                .build()
                .start();
        assertThat(closed).isFalse();

        server.stop();

        assertThat(closed).isTrue();
    }

    @Test
    void should_let_running_requests_complete_for_the_configured_grace_period() {
        var config = Config.just(ConfigSources.classpath("application.yaml"));

        var serverConfig = WebServerConfig.create(config.get("server"));

        assertThat(serverConfig.shutdownGracePeriod()).isEqualTo(Duration.ofSeconds(30));
    }
}
```

## Application assembly and entry point (`com.example.shop`)

`ShopConfiguration.start(config)` is the one way to start the application: `main` and every `*IT` test call it, so
what the tests start is what production starts. **Helidon's `WebServer.start()` does not throw when the server cannot
start**, for example when the port is taken: it logs the error and returns a stopped server, so `start` checks
`isRunning()`. Without this check the process would carry on with no server.

```java
// configuration/ShopConfiguration.java
package com.example.shop.configuration;

import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import java.time.Clock;

/** Application-wide assembly: the one way to start the application, used by main and by the *IT tests. */
public final class ShopConfiguration {

    private ShopConfiguration() {
    }

    /**
     * Wires the application and starts its server.
     *
     * @param config the whole configuration, whose {@code server} node configures the server
     * @return the started application; closing it stops the server and releases the resources
     * @throws RuntimeException if the startup fails, once what was already created is released
     */
    public static RunningApplication start(Config config) {
        var resources = new ApplicationResources();
        try {
            var clock = Clock.systemUTC();
            var server = WebServer.builder()
                    .config(config.get("server"))
                    .routing(routing -> {
                        routing.addFeature(new CloseOnStop(resources));
                        SalesConfiguration.routing(routing, config, clock, resources);
                    })
                    .build()
                    .start();
            // Helidon logs a failed start, such as a port already in use, and returns a stopped server.
            if (!server.isRunning()) {
                throw new IllegalStateException("The web server failed to start, see the log");
            }
            return new RunningApplication(server);
        } catch (RuntimeException | Error e) {
            resources.close();
            throw e;
        }
    }
}
```

```java
// configuration/RunningApplication.java
package com.example.shop.configuration;

import io.helidon.webserver.WebServer;

/** The started application, as returned by {@link ShopConfiguration#start}. Closing it stops the server gracefully. */
public final class RunningApplication implements AutoCloseable {

    private final WebServer server;

    RunningApplication(WebServer server) {
        this.server = server;
    }

    /** The port the server listens on, which is the actual one when {@code server.port} is 0. */
    public int port() {
        return server.port();
    }

    /** Stops the server, which then closes the application's resources through {@link CloseOnStop}. */
    @Override
    public void close() {
        server.stop();
    }
}
```

```java
// ShopApplication.java
package com.example.shop;

import com.example.shop.configuration.ShopConfiguration;
import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;

public final class ShopApplication {

    private ShopApplication() {
    }

    public static void main(String[] args) {
        LogConfig.configureRuntime();
        // System properties, then environment variables (DB_CONNECTION_URL...), then application.yaml.
        var application = ShopConfiguration.start(Config.create());
        // Not closed here: on SIGTERM, the server's own shutdown hook stops it and closes the resources.
        System.out.println("Shop is up: http://localhost:" + application.port() + "/orders");
    }
}
```

## Joining two bounded contexts of the application (`DomainAdapter`)

From `examples/helidon-multi-context/`: contexts `order` and `billing`, directly under the base package
(`com.example.shop.order...`, `com.example.shop.billing...`), with one `common` and one `configuration` package
holding one file per context. When an order is placed, `order` asks `billing` to create the invoice, in the same
transaction. Domain, commands and ports are written as in the slice above.

`order` defines its own outgoing port; the adapter calls `billing`'s incoming port and nothing else of it:

```java
// order/application/port/out/InvoiceIssuer.java
package com.example.shop.order.application.port.out;

import com.example.shop.order.domain.Order;

public interface InvoiceIssuer {

    void createInvoiceFor(Order order);
}
```

```java
// order/adapter/out/billing/BillingDomainAdapter.java
package com.example.shop.order.adapter.out.billing;

import com.example.shop.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.billing.application.port.in.InvoiceCreation;
import com.example.shop.order.domain.Order;
import com.example.shop.order.application.port.out.InvoiceIssuer;
import lombok.RequiredArgsConstructor;

/**
 * Joins the order context to the billing context of the same application: it implements order's outgoing
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
// order/adapter/out/billing/OrderBillingDomainAdapterConfiguration.java
package com.example.shop.order.adapter.out.billing;

import com.example.shop.billing.application.port.in.InvoiceCreation;
import com.example.shop.order.application.port.out.InvoiceIssuer;

public final class OrderBillingDomainAdapterConfiguration {

    private OrderBillingDomainAdapterConfiguration() {
    }

    /** @param invoiceCreation billing's incoming port, created by billing's configuration */
    public static InvoiceIssuer invoiceIssuer(InvoiceCreation invoiceCreation) {
        return new BillingDomainAdapter(invoiceCreation);
    }
}
```

### One transaction across contexts (`common/persistence`)

The pool and the bound connection move from the persistence adapter to the application's `common/persistence`, so that
billing's `CreateInvoiceService` joins the transaction opened by order's `PlaceOrderService`. Each context keeps its
own `TransactionRunner` port (same code as in the slice) and a thin adapter:

```java
// common/persistence/DbTransactions.java
package com.example.shop.common.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.helidon.config.Config;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;

/**
 * One Hikari connection pool for the whole application, with the connection of the open transaction bound to the
 * work through a {@link ScopedValue}. The persistence adapters of every bounded context run their statements
 * through {@link #run} and {@link #call}, so a use case of one context that calls another context through a
 * DomainAdapter keeps a single transaction. Only persistence adapters and configuration classes may use it; the
 * application's configuration closes it when the server stops.
 */
public final class DbTransactions implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final ScopedValue<Connection> current = ScopedValue.newInstance();

    private DbTransactions(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** @param dbConfig the {@code db} node of the configuration (connection and pool settings) */
    public static DbTransactions create(Config dbConfig) {
        var connection = dbConfig.get("connection");
        var hikari = new HikariConfig();
        hikari.setPoolName("shop");
        hikari.setJdbcUrl(connection.get("url").asString().get());
        hikari.setUsername(connection.get("username").asString().get());
        hikari.setPassword(connection.get("password").asString().get());
        dbConfig.get("pool").get("maximum-pool-size").asInt().ifPresent(hikari::setMaximumPoolSize);
        return new DbTransactions(new HikariDataSource(hikari));
    }

    /** The pool, for the schema migrations of the persistence adapter configurations. */
    public DataSource dataSource() {
        return dataSource;
    }

    /** Runs the work in the open transaction, or in a new one that commits when it returns and rolls back when it throws. */
    public <T> T inTransaction(Supplier<T> work) {
        if (current.isBound()) {
            return work.get();
        }
        // Closing hands the connection back to Hikari, which rolls back uncommitted work and restores auto-commit.
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            T result;
            try {
                result = ScopedValue.where(current, connection).call(work::get);
            } catch (RuntimeException | Error e) {
                rollback(connection, e);
                throw e;
            }
            connection.commit();
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Transaction failed: " + e.getMessage(), e);
        }
    }

    /** Runs the statements on the connection of the current transaction, or on a pooled connection in auto-commit. */
    public <T> T call(SqlFunction<T> statements) {
        try {
            if (current.isBound()) {
                return statements.apply(current.get());
            }
            try (var connection = dataSource.getConnection()) {
                return statements.apply(connection);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL failed: " + e.getMessage(), e);
        }
    }

    public void run(SqlConsumer statements) {
        call(connection -> {
            statements.accept(connection);
            return null;
        });
    }

    /** Closes the connection pool. */
    @Override
    public void close() {
        dataSource.close();
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            // Keep the original failure visible.
            failure.addSuppressed(rollbackFailure);
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
```

```java
// billing/adapter/out/persistence/JdbcTransactionRunner.java
package com.example.shop.billing.adapter.out.persistence;

import com.example.shop.common.persistence.DbTransactions;
import com.example.shop.billing.application.port.out.TransactionRunner;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

/** The billing context's transaction port, backed by the application's shared {@link DbTransactions}. */
@RequiredArgsConstructor
class JdbcTransactionRunner implements TransactionRunner {

    private final DbTransactions dbTransactions;

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return dbTransactions.inTransaction(work);
    }
}
```

```java
// billing/application/service/CreateInvoiceService.java
package com.example.shop.billing.application.service;

import com.example.shop.billing.domain.Invoice;
import com.example.shop.billing.application.port.in.CreateInvoiceCommand;
import com.example.shop.billing.application.port.in.InvoiceCreation;
import com.example.shop.billing.application.port.out.InvoiceStore;
import com.example.shop.billing.application.port.out.TransactionRunner;
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
crosses contexts. Reading a row: `UUID` and `TIMESTAMP WITH TIME ZONE` are read with
`getObject(column, UUID.class)` and `getObject(column, OffsetDateTime.class)`.

```java
// billing/adapter/out/persistence/BillingPersistenceAdapterConfiguration.java
package com.example.shop.billing.adapter.out.persistence;

import com.example.shop.billing.application.port.out.InvoiceFinder;
import com.example.shop.billing.application.port.out.InvoiceStore;
import com.example.shop.billing.application.port.out.TransactionRunner;
import com.example.shop.common.persistence.DbTransactions;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * The only public class of the package: it creates the persistence adapters of the billing context, which stay
 * package-private, and hands them out as ports. Used only by the context's configuration class.
 */
public final class BillingPersistenceAdapterConfiguration {

    private final InvoicePersistenceAdapter invoicePersistenceAdapter;
    private final JdbcTransactionRunner transactionRunner;

    /** @param dbTransactions the application's shared connection pool and transaction */
    public BillingPersistenceAdapterConfiguration(DbTransactions dbTransactions) {
        migrate(dbTransactions.dataSource());
        invoicePersistenceAdapter = new InvoicePersistenceAdapter(dbTransactions);
        transactionRunner = new JdbcTransactionRunner(dbTransactions);
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

    /** The context owns its schema (shop_billing) and its own Flyway history in it. */
    private static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas("shop_billing")
                .locations("classpath:db/migration/billing")
                .load()
                .migrate();
    }
}
```

```java
// billing/adapter/out/persistence/InvoicePersistenceAdapter.java
package com.example.shop.billing.adapter.out.persistence;

import com.example.shop.billing.domain.Invoice;
import com.example.shop.billing.application.port.out.InvoiceFinder;
import com.example.shop.billing.application.port.out.InvoiceStore;
import com.example.shop.common.persistence.DbTransactions;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Maps invoices to the billing context's schema with plain JDBC. The PostgreSQL driver binds and reads UUID and
 * OffsetDateTime natively through setObject and getObject.
 */
@RequiredArgsConstructor
class InvoicePersistenceAdapter implements InvoiceStore, InvoiceFinder {

    private final DbTransactions dbTransactions;

    @Override
    public void save(Invoice invoice) {
        dbTransactions.run(connection -> {
            try (var insert = connection.prepareStatement(
                    "INSERT INTO shop_billing.invoices (id, order_id, item_count, issued_at) VALUES (?, ?, ?, ?)")) {
                insert.setObject(1, invoice.getId());
                insert.setObject(2, invoice.getOrderId());
                insert.setInt(3, invoice.getItemCount());
                insert.setObject(4, OffsetDateTime.ofInstant(invoice.getIssuedAt(), ZoneOffset.UTC));
                insert.executeUpdate();
            }
        });
    }

    @Override
    public Optional<Invoice> loadByOrderId(UUID orderId) {
        return dbTransactions.call(connection -> {
            try (var select = connection.prepareStatement(
                    "SELECT id, item_count, issued_at FROM shop_billing.invoices WHERE order_id = ?")) {
                select.setObject(1, orderId);
                try (var rows = select.executeQuery()) {
                    return rows.next() ? Optional.of(toInvoice(rows, orderId)) : Optional.<Invoice>empty();
                }
            }
        });
    }

    private static Invoice toInvoice(ResultSet row, UUID orderId) throws SQLException {
        return Invoice.restore(
                row.getObject("id", UUID.class),
                orderId,
                row.getInt("item_count"),
                row.getObject("issued_at", OffsetDateTime.class).toInstant());
    }
}
```

```sql
-- src/main/resources/db/migration/billing/V1__create_invoices.sql
-- No foreign key to shop_order: the billing context does not depend on the order context's tables.
CREATE TABLE invoices (
    id         UUID PRIMARY KEY,
    order_id   UUID                     NOT NULL UNIQUE,
    item_count INTEGER                  NOT NULL,
    issued_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
```

### Wiring: target context first

Each context has its file in the `configuration` package, an instance class; `ShopConfiguration`, the
application-wide assembly, creates `billing` before `order` and hands billing's incoming port to order:

```java
// configuration/BillingConfiguration.java
package com.example.shop.configuration;

import com.example.shop.billing.adapter.in.web.BillingWebAdapterConfiguration;
import com.example.shop.billing.adapter.out.persistence.BillingPersistenceAdapterConfiguration;
import com.example.shop.billing.application.service.CreateInvoiceService;
import com.example.shop.billing.application.service.FindInvoiceByOrderService;
import com.example.shop.billing.application.port.in.InvoiceCreation;
import com.example.shop.billing.application.port.in.InvoiceLookup;
import com.example.shop.common.persistence.DbTransactions;
import io.helidon.webserver.http.HttpRules;
import java.time.Clock;

/** Wiring of the billing bounded context. Its incoming port InvoiceCreation is handed to the order context. */
public final class BillingConfiguration {

    private final InvoiceCreation invoiceCreation;
    private final InvoiceLookup invoiceLookup;

    public BillingConfiguration(DbTransactions dbTransactions, Clock clock) {
        var persistence = new BillingPersistenceAdapterConfiguration(dbTransactions);
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
// configuration/OrderConfiguration.java
package com.example.shop.configuration;

import com.example.shop.billing.application.port.in.InvoiceCreation;
import com.example.shop.common.persistence.DbTransactions;
import com.example.shop.order.adapter.in.web.OrderWebAdapterConfiguration;
import com.example.shop.order.adapter.out.billing.OrderBillingDomainAdapterConfiguration;
import com.example.shop.order.adapter.out.persistence.OrderPersistenceAdapterConfiguration;
import com.example.shop.order.application.service.PlaceOrderService;
import com.example.shop.order.application.port.in.OrderPlacement;
import io.helidon.webserver.http.HttpRules;
import java.time.Clock;

/** Wiring of the order bounded context. Billing is reached through billing's incoming port, given by ShopConfiguration. */
public final class OrderConfiguration {

    private final OrderPlacement orderPlacement;

    public OrderConfiguration(DbTransactions dbTransactions, Clock clock, InvoiceCreation invoiceCreation) {
        var persistence = new OrderPersistenceAdapterConfiguration(dbTransactions);
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
// configuration/ShopConfiguration.java
package com.example.shop.configuration;

import com.example.shop.common.persistence.DbTransactions;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import java.time.Clock;

/**
 * Application-wide assembly: the one way to start the application, used by main and by the *IT tests. It creates the
 * shared objects (Clock, one connection pool), then one configuration per bounded context, the target context (billing)
 * before the context that calls it (order), which receives billing's incoming port.
 */
public final class ShopConfiguration {

    private ShopConfiguration() {
    }

    /**
     * Wires the application and starts its server.
     *
     * @param config the whole configuration, whose {@code server} node configures the server
     * @return the started application; closing it stops the server and releases the resources
     * @throws RuntimeException if the startup fails, once what was already created is released
     */
    public static RunningApplication start(Config config) {
        var resources = new ApplicationResources();
        try {
            var server = WebServer.builder()
                    .config(config.get("server"))
                    .routing(routing -> {
                        routing.addFeature(new CloseOnStop(resources));
                        routing(routing, config, resources);
                    })
                    .build()
                    .start();
            // Helidon logs a failed start, such as a port already in use, and returns a stopped server.
            if (!server.isRunning()) {
                throw new IllegalStateException("The web server failed to start, see the log");
            }
            return new RunningApplication(server);
        } catch (RuntimeException | Error e) {
            resources.close();
            throw e;
        }
    }

    private static void routing(HttpRouting.Builder routing, Config config, ApplicationResources resources) {
        var clock = Clock.systemUTC();
        var dbTransactions = DbTransactions.create(config.get("db"));
        resources.add(dbTransactions);

        var billing = new BillingConfiguration(dbTransactions, clock);
        var order = new OrderConfiguration(dbTransactions, clock, billing.invoiceCreation());

        billing.routing(routing);
        order.routing(routing);
    }
}
```

### Architecture rules added for several contexts

Added to the `ArchitectureTest` below (with `import static com.tngtech.archunit.base.DescribedPredicate.not;` and
`import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;`), each with a fixture in
`ArchitectureRulesDetectViolationsTest` (for example `com.example.shop.order.fixture.ReachesIntoBilling`, a test
class of order holding a billing domain object):

```java
    // The application's shared connection pool (common.persistence) is technology: only persistence adapters and
    // configuration classes may use it, never the application.
    @ArchTest
    static final ArchRule sharedPersistenceIsUsedOnlyByPersistenceAdapters = noClasses()
            .that().resideOutsideOfPackages("..adapter.out.persistence..", "..configuration..", "..common.persistence..")
            .should().dependOnClassesThat().resideInAPackage("..common.persistence..");

    // Bounded contexts of the same application: order -> billing only through the DomainAdapter, and only
    // through billing's incoming ports. The configuration package, outside both contexts, wires them.
    @ArchTest
    static final ArchRule orderReachesBillingOnlyThroughItsDomainAdapter = noClasses()
            .that().resideInAPackage("com.example.shop.order..")
            .and().resideOutsideOfPackage("com.example.shop.order.adapter.out.billing..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop.billing..");

    @ArchTest
    static final ArchRule orderUsesOnlyBillingIncomingPorts = classes()
            .that().resideInAPackage("com.example.shop.order..")
            .should().onlyDependOnClassesThat(
                    not(resideInAPackage("com.example.shop.billing.."))
                            .or(resideInAPackage("com.example.shop.billing.application.port.in..")));

    @ArchTest
    static final ArchRule billingDoesNotUseOrder = noClasses()
            .that().resideInAPackage("com.example.shop.billing..")
            .should().dependOnClassesThat().resideInAPackage("com.example.shop.order..");

```

### Proving the shared transaction (`*IT`)

A deferred constraint trigger makes order's commit fail after the invoice was written: with one transaction per
context, the invoice would stay. The full test, started like `PlaceOrderIT` below, is
`examples/helidon-multi-context/src/test/java/com/example/shop/OrderToInvoiceIT.java`.

```java
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

    // Lombok (lombok..) and JSpecify (org.jspecify..) are deliberately not forbidden in domain and application.
    @ArchTest
    static final ArchRule domainAndApplicationHaveNoOrmOrJson = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(ORM_AND_JSON);

    // Domain and application are free of Helidon and of any container API. The application services own the
    // transaction boundary, through an outgoing port (TransactionRunner).
    @ArchTest
    static final ArchRule domainAndApplicationAreFrameworkFree = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "io.helidon..", "jakarta.inject..", "jakarta.enterprise..", "jakarta.ws.rs..",
                    "org.springframework..");

    // JDBC and the connection pool are persistence technology: they stay in the persistence adapters.
    @ArchTest
    static final ArchRule domainAndApplicationHaveNoJdbc = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage("java.sql..", "javax.sql..", "com.zaxxer.hikari..");

    @ArchTest
    static final ArchRule domainIsSelfContained = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..");

    @ArchTest
    static final ArchRule servicesDoNotUseAdapters = noClasses()
            .that().resideInAPackage("..application.service..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule commonIsIndependent = noClasses()
            .that().resideInAPackage("..common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..domain..", "..application..", "..adapter..", "..configuration..");

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
    void should_detect_a_violation_when_the_domain_uses_helidon() {
        assertViolation(ArchitectureTest.domainAndApplicationAreFrameworkFree, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_a_service_uses_jdbc() {
        assertViolation(ArchitectureTest.domainAndApplicationHaveNoJdbc, VIOLATIONS);
    }

    @Test
    void should_detect_a_violation_when_the_domain_uses_json() {
        assertViolation(ArchitectureTest.domainAndApplicationHaveNoOrmOrJson, VIOLATIONS);
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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
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

    /**
     * The application's configuration on a random port, with the containers' addresses taking precedence over
     * application.yaml.
     */
    static Config config() {
        return config(Map.of());
    }

    /** The same configuration, with these keys replaced. */
    static Config config(Map<String, String> overrides) {
        var values = new HashMap<>(Map.of(
                "server.port", "0",
                "db.connection.url", POSTGRES.getJdbcUrl(),
                "db.connection.username", POSTGRES.getUsername(),
                "db.connection.password", POSTGRES.getPassword(),
                "shipping.base-url", SHIPPING.baseUrl()));
        values.putAll(overrides);
        return Config.builder()
                .addSource(ConfigSources.create(values))
                .addSource(ConfigSources.classpath("application.yaml"))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();
    }

    /** Runs a statement directly on the database, for the tests' setup. */
    static void execute(String sql) {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The number of rows of a table, for the tests' assertions. */
    static long count(String table) {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var resultSet = connection.createStatement().executeQuery("SELECT count(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The number of connections open in PostgreSQL under this JDBC application name. */
    static long connectionsOf(String applicationName) {
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var statement = connection.prepareStatement(
                        "SELECT count(*) FROM pg_stat_activity WHERE application_name = ?")) {
            statement.setString(1, applicationName);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Waits for PostgreSQL to see the connections gone: a backend ends shortly after its client closes it. */
    static void awaitNoConnectionsOf(String applicationName) {
        var deadline = Instant.now().plusSeconds(10);
        while (connectionsOf(applicationName) > 0) {
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("Connections of " + applicationName + " are still open");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
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

import com.example.shop.configuration.RunningApplication;
import com.example.shop.configuration.ShopConfiguration;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The "place an order" use case through the whole stack: the application started as main starts it, on a random
 * port, with a real PostgreSQL (Testcontainers) and the shipping API replaced by WireMock.
 */
class PlaceOrderIT {

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
    void clean_database_and_stubs() {
        TestInfrastructure.execute("DELETE FROM order_lines");
        TestInfrastructure.execute("DELETE FROM orders");
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

    private static io.helidon.webclient.http1.Http1ClientResponse postOrder(String json) {
        return client.post("/orders")
                .header(HeaderNames.CONTENT_TYPE, "application/json")
                .submit(json);
    }

    private static long count(String table) {
        return TestInfrastructure.count(table);
    }
}
```

The application's startup and shutdown have their own test: closing the application closes the pool, and a failed
start reports the failure and leaves no connection open. The pool is named in the JDBC URL (`ApplicationName`), so
PostgreSQL itself shows whether its connections are gone. With several contexts, the pool belongs to
`ApplicationResources` from its creation, so the migration case adds nothing and the example leaves it out.

```java
// src/test/java/com/example/shop/ApplicationLifecycleIT.java
package com.example.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.shop.configuration.ShopConfiguration;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The application's own startup and shutdown. Each test names its connection pool in the JDBC URL, so that PostgreSQL
 * shows whether the connections of that pool are really closed.
 */
class ApplicationLifecycleIT {

    @Test
    void should_close_the_connection_pool_when_the_application_is_closed() {
        var pool = "closed-application";
        var application = ShopConfiguration.start(TestInfrastructure.config(Map.of("db.connection.url", jdbcUrl(pool))));
        assertThat(TestInfrastructure.connectionsOf(pool)).isPositive();

        application.close();

        TestInfrastructure.awaitNoConnectionsOf(pool);
    }

    @Test
    void should_fail_and_close_the_connection_pool_when_the_port_is_taken() throws IOException {
        var pool = "port-taken";
        try (var taken = new ServerSocket(0, 1, InetAddress.getByName("0.0.0.0"))) {
            var config = TestInfrastructure.config(Map.of(
                    "server.port", String.valueOf(taken.getLocalPort()),
                    "db.connection.url", jdbcUrl(pool)));

            assertThatThrownBy(() -> ShopConfiguration.start(config)).isInstanceOf(IllegalStateException.class);
        }

        TestInfrastructure.awaitNoConnectionsOf(pool);
    }

    @Test
    void should_fail_and_close_the_connection_pool_when_the_migration_fails() {
        var pool = "failed-migration";
        TestInfrastructure.execute("CREATE SCHEMA broken");
        TestInfrastructure.execute("CREATE TABLE broken.flyway_schema_history (not_what_flyway_expects INTEGER)");
        var config = TestInfrastructure.config(Map.of(
                "db.connection.url", jdbcUrl(pool) + "&currentSchema=broken"));

        assertThatThrownBy(() -> ShopConfiguration.start(config)).isInstanceOf(RuntimeException.class);

        TestInfrastructure.awaitNoConnectionsOf(pool);
    }

    private static String jdbcUrl(String pool) {
        return TestInfrastructure.POSTGRES.getJdbcUrl() + "&ApplicationName=" + pool;
    }
}
```

## Unit tests

Domain and command tests are plain JUnit + AssertJ. A service test mocks the outgoing ports (BDD Mockito) and runs
the work without a transaction:

```java
// src/test/java/com/example/shop/application/service/PlaceOrderServiceTest.java
package com.example.shop.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;

import com.example.shop.domain.Order;
import com.example.shop.application.port.in.PlaceOrderCommand;
import com.example.shop.application.port.out.OrderStore;
import com.example.shop.application.port.out.ShipmentRequester;
import com.example.shop.application.port.out.TransactionRunner;
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
