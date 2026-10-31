---
name: hexagonal-helidon
description: Scaffold or restructure a Helidon SE 4.x (LTS) project on Java 25 using hexagonal architecture as taught in "Get Your Hands Dirty on Clean Architecture" (Tom Hombergs, first-edition package layout), with application services distinct from domain services as in "Implementing Domain-Driven Design" (Vaughn Vernon). Use when the user wants to start a new Helidon service, add a use case or bounded context to one, or reorganize a Helidon codebase into ports and adapters.
---

# Hexagonal Helidon SE (4.x LTS, Java 25)

Structure Helidon SE projects with the ports-and-adapters style from
*Get Your Hands Dirty on Clean Architecture*, in the package layout of its **first edition**. The business domain
sits at the center; web, persistence and other technology are replaceable adapters around it.

Following *Implementing Domain-Driven Design* (Vaughn Vernon), **application services** (in `application/service`)
run the use cases and own the cross-cutting concerns (transactions, security), while the **domain** stays plain Java;
**domain services** (in `domain/service`) are optional and exist only for a domain operation that spans several
aggregates. This deliberately departs from the book's second edition, which nests `domain/model` and `domain/service`
inside `application` and puts the use-case services in the domain, where cross-cutting concerns would then have to be
tolerated. The same layout is used by the `hexagonal-spring` skill of the `mk-spring` plugin.

Ready-to-adapt code lives in [references/templates.md](references/templates.md). Read it before generating code.
Runnable projects built from these templates are in the McKratt/mk-claude-plugin repository: `examples/helidon-single-context/` (one
bounded context) and `examples/helidon-multi-context/` (contexts `order` and `billing` joined by a `DomainAdapter`).

## 0. Helidon version: the LTS line, not the latest

**Target Helidon 4.x, the LTS line** (4.5.6 when this skill was written), not the newest release.
Since Helidon 27 (September 2026), Helidon follows the OpenJDK "Tip & Tail" model: 27, 28... are short-lived
feature ("tip") releases that require the matching JDK (Java 27 for Helidon 27), while Helidon 4 is the LTS ("tail")
line and Helidon 29 is announced as the next LTS. Helidon 27 also moved MicroProfile and the integrations out of the
main repository and changed APIs, so code and documentation of 27+ do **not** apply here.

- Use the parent `io.helidon.applications:helidon-se` with the latest **4.x** version on Maven Central
  (`https://repo1.maven.org/maven2/io/helidon/helidon-bom/maven-metadata.xml`); never a 27+ version unless the user asks.
- Read the 4.x documentation: `https://helidon.io/docs/v4/` (not `/docs/latest/`, which follows the newest release).
  The same pages are versioned as Markdown in `helidon-io/helidon`, tag `4.5.x`, folder `docs/`; the 4.x examples live in
  `helidon-io/helidon-examples`, branch `helidon-4.x`.
- Helidon 4 needs Java 21 at minimum and recommends Java 25: set `maven.compiler.release` to 25 (the parent sets 21).
- Stay on stable APIs. In 4.5, Helidon Injection, Helidon Declarative and Helidon Data are marked **preview**
  in the documentation: do not use them unless the user asks. This skill wires everything by hand.

## 1. Gather inputs

Ask only for what is missing, in one short message:

- Group ID, artifact ID and **base package** (for example `com.example.shop`)
- The **bounded contexts** of the application: a single one (default) or their names (for example `order`, `billing`)
- Persistence: plain JDBC on a HikariCP connection pool (default), none, or other
- Database engine (PostgreSQL by default) and whether the schema is managed by Flyway (default)

## 2. Package structure

The base package is the application. It holds the main class, **one** `common` package, **one** `configuration`
package, and the hexagon(s):

- **a single bounded context**: the hexagon sits directly in the base package;
- **several bounded contexts**: one package per context, directly under the base package, each its own hexagon.

```
<base>/                              <base>/
├── <App>Application.java            ├── <App>Application.java
├── common/                          ├── common/
├── configuration/                   │   └── persistence/    # several contexts: the shared DbTransactions (rule 10)
├── domain/                          ├── configuration/
├── application/                     ├── <context-a>/        # domain/, application/, adapter/ as below
└── adapter/                         └── <context-b>/
     (one bounded context)                (several bounded contexts)
```

Layout of a hexagon (`<hexagon>` = `<base>` or `<base>/<context>`):

```
<hexagon>/
├── domain/                      # entities, value objects, domain rules (plain Java)
│   └── service/                 # optional domain services: a domain operation spanning several aggregates
├── application/
│   ├── port/
│   │   ├── in/                  # incoming ports (use cases) + one command per use case
│   │   └── out/                 # outgoing ports, including TransactionRunner (rule 5)
│   └── service/                 # application services *Service: implement incoming ports, own transactions
└── adapter/                     # each adapter package exposes one public *AdapterConfiguration (rule 7)
    ├── in/web/                  # *Controller implementing Helidon's HttpService, web models
    ├── out/persistence/         # persistence adapter (JDBC + Hikari), JdbcTransactionRunner, SQL
    └── out/<target-context>/    # DomainAdapter calling another bounded context of the application (rule 8)
```

The `configuration` package holds **one file per bounded context**, `<Context>Configuration` (with a single context,
named after its domain, for example `SalesConfiguration`), which wires the adapters and application services of that
context (rule 11), and `<App>Configuration`, the application-wide assembly called by `main` and by the `*IT` tests.

There is **no dependency injection container**. The `configuration` classes are the composition root: they create
the objects with `new` and pass them along. They may depend on anything; only other configuration classes and the
main class may depend on them.

## 3. Rules

1. **Dependencies point inward.** `adapter` → `application` (ports and services) → `domain`.
   - `domain` (including `domain/service`) depends on nothing but `common`: no `application`, no adapters.
   - **`domain` and `application` are plain Java**: no `io.helidon..` type, no container API (`jakarta.inject`,
     `jakarta.enterprise`, `jakarta.ws.rs`), no ORM (JPA, Hibernate...), no JDBC (`java.sql`, `javax.sql`) or
     connection pool, no Jackson, JSON-P/JSON-B or other JSON
     library. The cross-cutting concerns of the application services go through outgoing ports: the transaction
     boundary through `TransactionRunner` (rule 5), and likewise, when needed, the current user for authorization.
   - **Lombok and JSpecify are allowed in `domain` and `application`** (compile-time annotations only). Mark the
     domain, service and port packages `@NullMarked` in a `package-info.java` and annotate the rare nullable value
     with `@Nullable`.
2. **One narrow incoming port per use case** (an interface in `port/in`), named as a noun for what it offers
   (for example `OrderPlacement`, `InvoiceLookup`). Avoid "broad" service interfaces.
3. **Outgoing ports** (interfaces in `port/out`) are named as a noun for the role they play, for example
   `OrderStore`, `InvoiceFinder`, `ShipmentRequester`, and are defined by the application, implemented by adapters.
   **Interface names follow the usual Java conventions** ([Baeldung](https://www.baeldung.com/java-interface-naming-conventions)):
   a noun for a polymorphic interface or an adjective for a capability, the implementation combining its specialty
   with the interface name. **Never** a `Port` or `UseCase` suffix, an `I` prefix or an `Impl` suffix.
   Ports are recognized by their package, not by their name.
4. **Each use case has its own input model** (a command). It validates itself in its constructor.
   Use a Java `record` with a compact constructor. Business rules belong in the domain, not in the command.
5. **Application services** in `application/service` implement the incoming port, orchestrate domain objects and
   outgoing ports, and own the cross-cutting concerns of the use case; they hold no business rule. They are `public`,
   carry no annotation, and are created with `new` by their context's configuration.
   **Domain services** in `domain/service` are optional: create one only for a domain operation that does not belong
   to a single entity or value object, typically one spanning several aggregates. They are stateless plain Java,
   named after the operation, use only `domain` types and know nothing of transactions, security or ports.
   **An application service owns its transaction boundary through the outgoing port `TransactionRunner`**
   (`<T> T inTransaction(Supplier<T> work)`): commit when the work returns, rollback when it throws. The persistence
   adapter implements it on a JDBC connection of the Hikari pool (rule 13). Queries that need no transaction do not use it.
   With several bounded contexts on one database, each context keeps its own `TransactionRunner` port, and their
   adapters delegate to the application's shared `DbTransactions` (rule 10), so a use case that calls another context
   keeps **one** transaction: the callee joins the caller's, as with Spring's default propagation.
   This port is a convention of this skill, not of the book, which uses Spring's `@Transactional` on the
   application service.
6. **Adapters own their models.** Web request/response records and SQL rows are separate from domain classes,
   with explicit mappers. Choose the mapping strategy per boundary (no mapping, two-way, full, one-way)
   and record any shortcut consciously, for example in an `ARCHITECTURE.md` or an ADR.
7. **Adapters never depend on each other and are package-private.** Without a container, each adapter package
   exposes **exactly one public class**, `<Context><Kind>AdapterConfiguration` (for example
   `SalesPersistenceAdapterConfiguration`, `SalesWebAdapterConfiguration`), which creates the package's adapters and
   hands them out **typed as ports** (outgoing) or registers them on Helidon's `HttpRules` (incoming). Every other class
   of the package (adapters, rows, mappers, the transaction runner) stays package-private.
   **Web adapters** are named `*Controller` (the book's name) and implement `io.helidon.webserver.http.HttpService`.
   They call incoming ports only, read the body with `request.content().as(Request.class)` (Jackson media support),
   and translate `IllegalArgumentException` from commands and the domain into a `400` JSON response in the handler
   itself (`HttpRules` has no error handler; `HttpRouting.Builder.error(...)` is for application-wide handlers).
8. **Joining two bounded contexts of the application.** The source context defines an outgoing port
   (`application/port/out`, in its own language and types). It is implemented by a `<Target>DomainAdapter` in
   `adapter/out/<target-context>/`, which calls the target context's **incoming port** (a use case interface) directly and
   converts the source's types to the target's command. Nothing else of the target context may be used: not its
   domain, services, outgoing ports or adapters. Wiring: with several contexts, each `<Context>Configuration` is an
   instance class (its constructor creates adapters and services; it exposes the incoming ports other contexts need
   and a `routing(HttpRules)` method), and `<App>Configuration` creates the target context first, then the source
   context with the target's incoming port, which the source's configuration passes to its
   `<Context><Target>DomainAdapterConfiguration`. Inside the source context, nothing but that adapter may import the
   target.
9. **Calling another application** (another service, for example a third party) goes through its **public API**, for
   example a REST service, but it can be another technology (messaging, gRPC...). The source defines an outgoing port in its
   `application/port/out`, implemented by an adapter in `adapter/out/<target>/` (for example
   `<Target>RestAdapter`) that calls the target's public API and maps the response to the source's own model.
   Outgoing HTTP goes through **Helidon's `WebClient`** (`io.helidon.webclient.api.WebClient`), built from Helidon
   `Config` (base URI and client settings), never through a raw HTTP client (`java.net.http`, Apache or OkHttp).
   `WebClient` does **not** throw on an error status: check `response.status().family()` and throw.
   There is **no compile-time dependency** between the two applications. If the API technology is not stated, ask the user.
10. **`common`** (one package, directly under the base package) holds utilities usable anywhere in the application
    (all its contexts and layers). It depends on neither `domain`, `application`, `adapter` nor `configuration`.
    It is not shared with other applications (rule 9).
    **Exception for several contexts on one database: `common/persistence/DbTransactions`**, a public class holding the
    application's single Hikari pool and binding the connection of the open transaction to the work. It is technology, so only
    persistence adapters and configuration classes may use it, never `application` (enforced by ArchUnit). This is a
    conscious shortcut of this skill: without it, two package-private transaction runners could not share a transaction.
11. **Wiring of a bounded context.** Its file `configuration/<Context>Configuration` is a `public final` class: with a
    single context, a static method (for example `routing(HttpRouting.Builder routing, Config config, Clock clock)`); with several,
    an instance class (rule 8). In this order, it creates the outgoing adapters through their `*AdapterConfiguration`
    (passing the relevant `Config` node, such as `config.get("db")`), creates the application services with `new`, and
    registers the web adapters through the web `*AdapterConfiguration`. `configuration/<App>Configuration` creates the
    shared objects and calls each context's configuration; it is called by `main` and by the `*IT` tests, so the tests
    run the real wiring. The configuration that creates the connection pool (the context's with a single context,
    `<App>Configuration` with `DbTransactions`) registers it with `routing.addFeature(new CloseOnStop(pool))` for the
    graceful shutdown (rule 13).
12. **Configuration** is Helidon Config in `src/main/resources/application.yaml` (dependency `helidon-config-yaml`),
    created once in `main` with `Config.create()` and passed down explicitly as `Config` nodes. Do not read it from
    adapters or services through `Services.get(Config.class)` or `Config.global()`. Environment variables override keys
    (`DB_CONNECTION_URL` overrides `db.connection.url`).
13. **Persistence with JDBC and a Hikari pool.** `com.zaxxer:HikariCP` and the JDBC driver; no Helidon DbClient.
    **Never set the HikariCP version**: keep the one managed by the Helidon BOM (5.0.1 in 4.5.6), the version Helidon's
    own Hikari modules are built with (`helidon-dbclient-hikari`, `helidon-integrations-cdi-datasource-hikaricp`...),
    so the pool stays compatible with the Helidon release and moves with it on each Helidon upgrade. The `db` config node holds `connection` (`url`, `username`, `password`) and `pool`
    (`maximum-pool-size`), mapped explicitly onto a `HikariConfig` by the persistence adapter configuration, which
    creates the `HikariDataSource`. The adapter owns its SQL and uses `PreparedStatement` (never string concatenation).
    `JdbcTransactionRunner` takes a connection from the pool, sets auto-commit off and binds the connection to the work
    with a `ScopedValue` (final in Java 25, no preview flag), so every statement of the use case runs on it; outside a
    transaction, statements borrow a pooled connection in auto-commit. Details (template provided):
    - Wrap `SQLException` in an unchecked exception that is **not** `IllegalArgumentException` (the web adapters turn
      that one into a `400`); the examples use `IllegalStateException`.
    - Roll back when the work throws and add a failing `rollback()` as suppressed to the original exception.
    - Closing the connection hands it back to Hikari, which rolls back uncommitted work and restores auto-commit
      (HikariCP `ProxyConnection.close()`).
    - With PostgreSQL, bind `UUID` and `OffsetDateTime` (UTC, for `TIMESTAMP WITH TIME ZONE`) with `setObject` and read
      them with `getObject(column, UUID.class)` / `getObject(column, OffsetDateTime.class)`: no `CAST` needed.
    The schema is applied by Flyway (`flyway-core` + `flyway-database-<engine>`) on the same `DataSource`, from the
    persistence adapter configuration, before the adapters are used. Do not use embedded substitutes such as H2.
    **Graceful shutdown**: the class that creates the pool (the persistence adapter configuration, or `DbTransactions`)
    is `AutoCloseable` and closes the `HikariDataSource`; the configuration registers it on the routing through
    `configuration/CloseOnStop`, an `HttpFeature` without routes that closes it in `afterStop()`. Helidon calls it
    once the server accepts no more connections and running requests have completed or `server.shutdown-grace-period`
    (default `PT0.5S`, set it in `application.yaml`) has elapsed; the server's JVM shutdown hook (on by default) runs
    this sequence on `SIGTERM`. Do not close the pool from a separate `Runtime.addShutdownHook`, which may run while
    requests are still served.
    With several contexts, **each context owns a database schema** (for example `shop_order`, `shop_billing`) and
    migrates it with its own Flyway (`.schemas("shop_billing").locations("classpath:db/migration/billing")`, which
    also keeps its history table in that schema); its SQL names the schema, and no foreign key crosses contexts.
14. **Naming**: interfaces as in rule 3, `*Service`, `*Controller`, `*PersistenceAdapter`, `*DomainAdapter`,
    `<Target>RestAdapter` (or the technology used), `JdbcTransactionRunner`, `DbTransactions`, `CloseOnStop`,
    `<Context><Kind>AdapterConfiguration`, `<Context>Configuration`, `<App>Configuration`,
    `<App>Application`.
15. **Rich domain model, immutable value objects.** The domain is **not anemic** (an *anemic domain model* is one whose
    objects only hold data behind getters and setters while the logic lives in services): entities and aggregates carry
    their data *and* the behavior that changes it, and protect their invariants themselves, through factory methods
    and intention-revealing methods (`Order.place(lines, placedAt)`, `order.cancel()`), never through public setters
    that let an application service assemble a valid state. The application service only orchestrates: it loads,
    calls domain behavior, saves. **Value objects** (`OrderId`, `Order.Line`, an amount of money) have no identity, are
    compared by value and are **immutable**: a change produces a new instance. That is why they are Java **`record`s**:
    final fields, no setters, `equals`/`hashCode` by value, and validation in the compact constructor, so an invalid
    value cannot exist. A record is only *shallowly* immutable: copy mutable components in the compact constructor
    (`List.copyOf(...)`) and never expose mutable state. Entities have an identity and a lifecycle: they are classes, not
    records, with `private final` fields where possible and no `@Setter` (Lombok's `@Data` and `@Value` are banned anyway).

## 4. Java 25 guidelines

- Records for commands, web models, value objects and IDs: they are immutable (rule 15). Entities are classes
  with behavior, not records.
- Lombok may be used in `domain` and `application` (for example `@Getter` on entities, `@RequiredArgsConstructor` on services
  and adapters). Declare it `provided` with the annotation processor path configured; its version is not managed by the
  Helidon BOM. `@Builder`, `@Data` and `@Value` are banned through `lombok.config` (`flagUsage = error`), as in the examples.
- JSpecify (`org.jspecify:jspecify`, version not managed by the Helidon BOM) in `domain` and `application`: `@NullMarked` on packages.
- Sealed interfaces and pattern-matching `switch` for closed domain variants (for example result types).
- `var` for obvious local types only; keep signatures explicit.
- Do not enable preview features unless the user asks.
- Set `maven.compiler.release` (and `maven.compiler.source`, which the Helidon parent also sets) to 25, and enforce
  Java 25 and Maven 3.10+ with `maven-enforcer-plugin`.

## 5. Testing

The rules of the `java-testing` skill of the `mk-java` plugin apply (use case tests first, `*IT` run by failsafe,
real database, WireMock, AssertJ, BDD Mockito, snake_case test names, Arrange / Act / Assert). This section gives the
Helidon mechanics.

**Priority: tests that cover a whole use case**, named `*IT` and run by `maven-failsafe-plugin` in the
integration-test phase (`mvn verify`); surefire keeps the fast `*Test` tests.

- Annotate with Helidon's **`@ServerTest`** (`helidon-webserver-testing-junit5`): it starts the WebServer on a random
  port and injects an `Http1Client` bound to it.
- A static **`@SetUpRoute`** method calls the application's own `<App>Configuration.routing(routing, testConfig)`, so
  the real wiring is tested.
- The database is a **real one in a container** (Testcontainers, same engine as production; the Testcontainers version
  is managed by the Helidon BOM, use `${version.lib.testcontainers}` for modules it does not list, such as `postgresql`).
  **Third-party services are mocked with WireMock** (`org.wiremock:wiremock-standalone`, dynamic port). Start both once
  per JVM in a shared test class and build the test `Config` with their addresses first, then `application.yaml`.
- Clean the tables and reset WireMock in `@BeforeEach`; verify the requests the adapter sends and test the failure cases
  (for example a 503 from a third party must roll the transaction back and store nothing).

Complement with, in this order of importance:

- **Domain**: plain unit tests (entities, value objects, self-validating commands).
- **Architecture**: an ArchUnit test (template provided) plus a test that feeds deliberately bad fixture classes to the
  rules, so a rule that silently stops matching fails the build. It needs no server: a `*Test` run by surefire.
- **Application services**: unit tests with mocked outgoing ports (Mockito, BDD notation) and a `TransactionRunner` that simply
  runs the work, only for branching logic that is awkward to reach through the use case tests.
- **`@RoutingTest`** (no socket, `DirectClient`): only when a use case test cannot reach the behavior, for example
  exhaustive request validation. It is the exception, not the default.

## 6. Workflow

1. Gather inputs (section 1) and look up the latest Helidon **4.x** version (section 0).
2. Create the project: if the working directory is empty, write the `pom.xml` (parent `helidon-se`) and the main class by hand.
   If the project exists, inspect it first and do not overwrite files.
3. Create the package tree (section 2) and one **vertical slice** from `templates.md`
   (use case, command, application service, outgoing ports, `TransactionRunner`, web adapter, persistence adapter,
   configurations).
4. Add the ArchUnit tests and their dependency (latest release on Maven Central).
5. Add the use case integration tests (`*IT`, Testcontainers, WireMock) and the domain unit tests following section 5.
6. Run the build (`mvn verify`; Docker must be running for Testcontainers) and fix failures.
7. Report: the package tree, the shortcuts taken, and what remains to implement.

## 7. When adding to an existing project

- Add a new use case: new incoming port + command in `port/in`, application service in `application/service` (created
  in `configuration/<Context>Configuration`), any new outgoing port in `port/out`, then adapter changes. Never skip the port.
  Business rules go in `domain` (or a domain service in `domain/service` when they span several aggregates).
- Add a new bounded context: new `<base>/<context>/` package with the same internal layout and its file
  `configuration/<Context>Configuration`. If the application had a single context until now, move its `domain`,
  `application` and `adapter` packages from `<base>/` into `<base>/<context>/` (`common` and `configuration` stay),
  move the transaction binding to `common/persistence/DbTransactions` and give each context its own database schema.
- Connect two bounded contexts of the application: follow rule 8 (outgoing port + `*DomainAdapter`).
- Call another application: follow rule 9 (outgoing port + adapter calling the target's public API with `WebClient`).
- To check conformance afterwards, delegate to the `helidon-hexagonal-reviewer` agent.

## Sources

- Architecture: Tom Hombergs, *Get Your Hands Dirty on Clean Architecture* (Packt), package layout of the first
  edition (book repository `thombergs/buckpal`, branch `single-module`: `application/service`, `application/port/in|out`,
  `domain` without `model`); application services versus domain services from Vaughn Vernon, *Implementing
  Domain-Driven Design* (Addison-Wesley, 2013), chapters "Application" and "Services". The rules are the ones of the
  `hexagonal-spring` skill (`mk-spring` plugin), adapted to a container-less framework. The second-edition layout was
  confirmed by the author of these plugins with a photo of page 28; the book repository switched to it on 2023-06-09
  (commit `ebdb694`).
- Helidon LTS status: `CHANGELOG.md` of `helidon-io/helidon` on `main` (Helidon 27 entry: "Helidon 4 is considered an
  LTS release. Helidon 29 will be the next LTS release."), and the 4.5.6 entry ("A minimum of Java 21 is required to use
  Helidon 4. Java 25 is recommended.").
- Helidon 4.5.6 documentation (`docs/` of tag `4.5.6`, published at https://helidon.io/docs/v4/): SE WebServer
  (routing, `HttpService`, error handling, media support), Config, WebClient, Testing (`@ServerTest`,
  `@RoutingTest`, `@SetUpRoute`), Dependency Management; preview status of Injection, Declarative and Data.
- Helidon examples, branch `helidon-4.x`: `quickstarts/helidon-quickstart-se`.
- HikariCP: version 5.0.1 managed by `io.helidon:helidon-dependencies:4.5.6` (`version.lib.hikaricp`), Helidon's Hikari
  modules listed in `io.helidon:helidon-bom:4.5.6`; the rollback and auto-commit reset on close come from the sources
  of `com.zaxxer.hikari.pool.ProxyConnection` and `PoolBase` (5.0.1).
- Graceful shutdown: Helidon 4.5.6 sources of `io.helidon.webserver` (`ServerListener.stop()`: closes the server
  socket, waits up to `shutdownGracePeriod`, then calls `router.afterStop()`; `HttpRoutingImpl.afterStop()` calls the
  features; `WebServerConfig.shutdownHook()` defaults to `true`; `ListenerConfig.shutdownGracePeriod()` defaults to
  `PT0.5S`), confirmed by `CloseOnStopTest` and by Hikari's "Shutdown completed" log at the end of the `*IT` tests.
- `ScopedValue`: JEP 506 (final in JDK 25). The JDBC persistence, the shared transaction across contexts and the
  PostgreSQL type mappings were confirmed by running both Helidon examples (`mvn verify`) against PostgreSQL 17.
- Rich domain model and value objects: Martin Fowler, "AnemicDomainModel" (https://martinfowler.com/bliki/AnemicDomainModel.html);
  value objects as immutable, compared by value: Eric Evans, *Domain-Driven Design* (Addison-Wesley, 2003), and Vaughn
  Vernon, *Implementing Domain-Driven Design*, chapter "Value Objects"; records: the Javadoc of `java.lang.Record`
  ("A record class is a shallowly immutable, transparent carrier for a fixed set of values").
