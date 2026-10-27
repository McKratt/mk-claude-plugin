---
name: hexagonal-helidon
description: Scaffold or restructure a Helidon SE 4.x (LTS) project on Java 25 using hexagonal architecture as taught in "Get Your Hands Dirty on Clean Architecture" (Tom Hombergs). Use when the user wants to start a new Helidon service, add a use case or bounded context to one, or reorganize a Helidon codebase into ports and adapters.
---

# Hexagonal Helidon SE (4.x LTS, Java 25)

Structure Helidon SE projects with the ports-and-adapters style from
*Get Your Hands Dirty on Clean Architecture*. The business domain sits at the center;
web, persistence and other technology are replaceable adapters around it.

Ready-to-adapt code lives in [references/templates.md](references/templates.md). Read it before generating code.
A runnable project built from these templates is `examples/helidon-single-context/` in this plugin's repository.

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
- **Component** name (for example `sales`) and its first **bounded context** name (for example `order`)
- Persistence: Helidon DbClient over JDBC (default), none, or other
- Database engine (PostgreSQL by default) and whether the schema is managed by Flyway (default)

## 2. Package structure

One top-level package per component. A component holds one or more **bounded contexts**, and each bounded context
is its own hexagon. When the component has **several** contexts, each gets its own package; when it has **a single**
context, the context level is omitted from the package name. A `common` package holds utilities usable anywhere
in the component:

```
<base>/<component>/
├── common/                      # utilities usable anywhere in the component (shared by its contexts)
└── [<context>/]                 # one hexagon per bounded context (level omitted for a single context)
```

Layout of a hexagon (`<hexagon>` = `<base>/<component>/[<context>/]`):

```
<hexagon>/
├── application/
│   ├── domain/
│   │   ├── model/               # entities, value objects, domain rules (plain Java)
│   │   └── service/             # *Service: implements incoming ports, uses outgoing ports
│   └── port/
│       ├── in/                  # incoming ports (use cases) + one command per use case
│       └── out/                 # outgoing ports, including TransactionRunner (rule 5)
├── adapter/                     # each adapter package exposes one public *AdapterConfiguration (rule 7)
│   ├── in/web/                  # *Controller implementing Helidon's HttpService, web models
│   ├── out/persistence/         # persistence adapter (DbClient), DbClientTransactionRunner, SQL
│   └── out/<target-context>/    # DomainAdapter calling another bounded context of the same component (rule 8)
└── configuration/               # <Hexagon>Configuration: wires adapters and services of this hexagon (rule 11)
```

The application entry point and the application-wide assembly sit directly under the base package:

```
<base>/
├── <App>Application.java       # main: Config.create(), WebServer.builder()...start()
├── configuration/              # <App>Configuration: shared objects (Clock...), one call per component
└── <component>/[<context>/]    # see above
```

There is **no dependency injection container**. The `configuration` classes are the composition root: they create
the objects with `new` and pass them along. They may depend on anything; only other configuration classes and the
main class may depend on them.

## 3. Rules

1. **Dependencies point inward.** `adapter` → `application` (ports and `domain/service`) → `domain/model`.
   - `domain/model` depends on nothing but `common`: no ports, services or adapters.
   - **The whole `application` package is plain Java**: no `io.helidon..` type, no container API (`jakarta.inject`,
     `jakarta.enterprise`, `jakarta.ws.rs`), no ORM (JPA, Hibernate...), no Jackson, JSON-P/JSON-B or other JSON
     library. Unlike Spring's `@Transactional`, there is **no exception**: the transaction boundary goes through a port (rule 5).
   - **Lombok and JSpecify are allowed in the whole application** (compile-time annotations only). Mark the domain and
     port packages `@NullMarked` in a `package-info.java` and annotate the rare nullable value with `@Nullable`.
2. **One narrow incoming port per use case** (an interface in `port/in`), named as a noun for what it offers
   (for example `OrderPlacement`, `InvoiceLookup`). Avoid "broad" service interfaces.
3. **Outgoing ports** (interfaces in `port/out`) are named as a noun for the role they play, for example
   `OrderStore`, `InvoiceFinder`, `ShipmentRequester`, and are defined by the application, implemented by adapters.
   **Interface names follow the usual Java conventions** ([Baeldung](https://www.baeldung.com/java-interface-naming-conventions)):
   a noun for a polymorphic interface or an adjective for a capability, the implementation combining its specialty
   with the interface name. **Never** a `Port` or `UseCase` suffix, an `I` prefix or an `Impl` suffix.
   Ports are recognized by their package, not by their name.
4. **Each use case has its own input model** (a command). It validates itself in its constructor.
   Use a Java `record` with a compact constructor. Business rules belong in the domain model, not in the command.
5. **Services** in `application/domain/service` implement the incoming port and orchestrate domain objects and
   outgoing ports. They are `public`, carry no annotation, and are created with `new` by the hexagon's configuration.
   **A service owns its transaction boundary through the outgoing port `TransactionRunner`**
   (`<T> T inTransaction(Supplier<T> work)`): commit when the work returns, rollback when it throws. The persistence
   adapter implements it with `DbClient.transaction()` (rule 13). Queries that need no transaction do not use it.
   This port is a convention of this skill, not of the book, which uses Spring's `@Transactional`.
6. **Adapters own their models.** Web request/response records and SQL rows are separate from domain classes,
   with explicit mappers. Choose the mapping strategy per boundary (no mapping, two-way, full, one-way)
   and record any shortcut consciously, for example in an `ARCHITECTURE.md` or an ADR.
7. **Adapters never depend on each other and are package-private.** Without a container, each adapter package
   exposes **exactly one public class**, `<Hexagon><Kind>AdapterConfiguration` (for example
   `SalesPersistenceAdapterConfiguration`, `SalesWebAdapterConfiguration`), which creates the package's adapters and
   hands them out **typed as ports** (outgoing) or registers them on Helidon's `HttpRules` (incoming). Every other class
   of the package (adapters, rows, mappers, the transaction runner) stays package-private.
   **Web adapters** are named `*Controller` (the book's name) and implement `io.helidon.webserver.http.HttpService`.
   They call incoming ports only, read the body with `request.content().as(Request.class)` (Jackson media support),
   and translate `IllegalArgumentException` from commands and the domain into a `400` JSON response in the handler
   itself (`HttpRules` has no error handler; `HttpRouting.Builder.error(...)` is for application-wide handlers).
8. **Joining two bounded contexts of the same component.** The source context defines an outgoing port
   (`application/port/out`, in its own language and types). It is implemented by a `<Target>DomainAdapter` in
   `adapter/out/<target-context>/`, which calls the target context's **incoming port** (a use case interface) directly and
   converts the source's types to the target's command. Nothing else of the target context may be used: not its
   domain, services, outgoing ports or adapters. The component's configuration creates the target's use case first
   and passes it to the source's `*AdapterConfiguration`.
9. **Joining two different components** goes through the **public API of the target component**, for example a REST
   service, but it can be another technology (messaging, gRPC...). The source defines an outgoing port in its
   `application/port/out`, implemented by an adapter in `adapter/out/<target-component>/` (for example
   `<Target>RestAdapter`) that calls the target's public API and maps the response to the source's own model.
   Outgoing HTTP goes through **Helidon's `WebClient`** (`io.helidon.webclient.api.WebClient`), built from Helidon
   `Config` (base URI and client settings), never through a raw HTTP client (`java.net.http`, Apache or OkHttp).
   `WebClient` does **not** throw on an error status: check `response.status().family()` and throw.
   There is **no compile-time dependency** between the two components. If the API technology is not stated, ask the user.
10. **`common`** holds utilities usable anywhere in the component (all its contexts and layers). It depends on
    neither `application`, `adapter` nor any `configuration` package. It is not shared between different components (rule 9).
11. **Wiring of a hexagon.** `<hexagon>/configuration/<Hexagon>Configuration` is a `public final` class with a static
    method (for example `routing(HttpRules rules, Config config, Clock clock)`) that, in this order: creates the outgoing
    adapters through their `*AdapterConfiguration` (passing the relevant `Config` node, such as `config.get("db")`),
    creates the services with `new`, and registers the web adapters through the web `*AdapterConfiguration`.
    `<base>/configuration/<App>Configuration` calls each hexagon's configuration and is called by `main` and by the
    `*IT` tests, so the tests run the real wiring.
12. **Configuration** is Helidon Config in `src/main/resources/application.yaml` (dependency `helidon-config-yaml`),
    created once in `main` with `Config.create()` and passed down explicitly as `Config` nodes. Do not read it from
    adapters or services through `Services.get(Config.class)` or `Config.global()`. Environment variables override keys
    (`DB_CONNECTION_URL` overrides `db.connection.url`).
13. **Persistence with DbClient.** `helidon-dbclient` + `helidon-dbclient-jdbc` + `helidon-dbclient-hikari` and the JDBC
    driver; the `db` config node holds `source: jdbc` and `connection` (`url`, `username`, `password`). The adapter owns
    its SQL. Statements of a transaction run on the `DbTransaction` bound to the current thread by
    `DbClientTransactionRunner` (Helidon serves each request on its own virtual thread). Pitfalls verified on 4.5.6:
    - DbClient binds a `UUID` as a string: write `CAST(? AS UUID)` in PostgreSQL statements.
    - When a statement of a transaction fails, DbClient closes the connection itself, so the following `rollback()`
      throws: catch it and add it as suppressed to the original exception (template provided).
    - Pass an `OffsetDateTime` (UTC) for a `TIMESTAMP WITH TIME ZONE` column.
    The schema is applied by Flyway (`flyway-core` + `flyway-database-<engine>`) from the persistence adapter
    configuration, before the `DbClient` is used. Do not use embedded substitutes such as H2.
14. **Naming**: interfaces as in rule 3, `*Service`, `*Controller`, `*PersistenceAdapter`, `*DomainAdapter`,
    `<Target>RestAdapter` (or the technology used), `DbClientTransactionRunner`, `<Hexagon><Kind>AdapterConfiguration`,
    `<Hexagon>Configuration`, `<App>Configuration`, `<App>Application`.

## 4. Java 25 guidelines

- Records for commands, web models, value objects and IDs.
- Lombok may be used in `application/domain` (for example `@Getter` on entities, `@RequiredArgsConstructor` on services
  and adapters). Declare it `provided` with the annotation processor path configured; its version is not managed by the
  Helidon BOM. `@Builder`, `@Data` and `@Value` are banned through `lombok.config` (`flagUsage = error`), as in the examples.
- JSpecify (`org.jspecify:jspecify`, version not managed by the Helidon BOM) in `application`: `@NullMarked` on packages.
- Sealed interfaces and pattern-matching `switch` for closed domain variants (for example result types).
- `var` for obvious local types only; keep signatures explicit.
- Do not enable preview features unless the user asks.
- Set `maven.compiler.release` (and `maven.compiler.source`, which the Helidon parent also sets) to 25, and enforce
  Java 25 and Maven 3.10+ with `maven-enforcer-plugin`.

## 5. Testing

The priorities of the `java-testing` skill of this plugin apply (use case tests first, AssertJ, BDD Mockito,
snake_case test names, Arrange / Act / Assert); its Spring-specific mechanics are replaced as follows.

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
- **Services**: unit tests with mocked outgoing ports (Mockito, BDD notation) and a `TransactionRunner` that simply
  runs the work, only for branching logic that is awkward to reach through the use case tests.
- **`@RoutingTest`** (no socket, `DirectClient`): only when a use case test cannot reach the behavior, for example
  exhaustive request validation. It is the exception, not the default.

## 6. Workflow

1. Gather inputs (section 1) and look up the latest Helidon **4.x** version (section 0).
2. Create the project: if the working directory is empty, write the `pom.xml` (parent `helidon-se`) and the main class by hand.
   If the project exists, inspect it first and do not overwrite files.
3. Create the package tree for the first component and one **vertical slice** from `templates.md`
   (use case, command, service, outgoing ports, `TransactionRunner`, web adapter, persistence adapter, configurations).
4. Add the ArchUnit tests and their dependency (latest release on Maven Central).
5. Add the use case integration tests (`*IT`, Testcontainers, WireMock) and the domain unit tests following section 5.
6. Run the build (`mvn verify`; Docker must be running for Testcontainers) and fix failures.
7. Report: the package tree, the shortcuts taken, and what remains to implement.

## 7. When adding to an existing project

- Add a new use case: new incoming port + command in `port/in`, service in `application/domain/service` (created in
  `<Hexagon>Configuration`), any new outgoing port in `port/out`, then adapter changes. Never skip the port.
- Add a new bounded context: new `<component>/<context>/` package with the same internal layout and its own
  `<Context>Configuration`. If the component had a single context until now, move its packages from `<component>/` into
  `<component>/<context>/`.
- Connect two bounded contexts of the same component: follow rule 8 (outgoing port + `*DomainAdapter`).
- Connect two different components: follow rule 9 (outgoing port + adapter calling the target's public API with `WebClient`).
- To check conformance afterwards, delegate to the `helidon-hexagonal-reviewer` agent.

## Sources

- Architecture: Tom Hombergs, *Get Your Hands Dirty on Clean Architecture*, 2nd edition (Packt); the package layout and
  rules are the ones of the `hexagonal-spring` skill of this plugin, adapted to a container-less framework.
- Helidon LTS status: `CHANGELOG.md` of `helidon-io/helidon` on `main` (Helidon 27 entry: "Helidon 4 is considered an
  LTS release. Helidon 29 will be the next LTS release."), and the 4.5.6 entry ("A minimum of Java 21 is required to use
  Helidon 4. Java 25 is recommended.").
- Helidon 4.5.6 documentation (`docs/` of tag `4.5.6`, published at https://helidon.io/docs/v4/): SE WebServer
  (routing, `HttpService`, error handling, media support), Config, DbClient, WebClient, Testing (`@ServerTest`,
  `@RoutingTest`, `@SetUpRoute`), Dependency Management; preview status of Injection, Declarative and Data.
- Helidon examples, branch `helidon-4.x`: `quickstarts/helidon-quickstart-se`, `dbclient/postgres`.
- Helidon 4.5.6 sources for the DbClient transaction behavior (`JdbcTransaction`, `TransactionContext`,
  `JdbcStatementDml`), confirmed by running `examples/helidon-single-context` against PostgreSQL.
