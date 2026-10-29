---
name: hexagonal-spring
description: Scaffold or restructure a Spring Boot project on Java 25 using hexagonal architecture as taught in "Get Your Hands Dirty on Clean Architecture" (Tom Hombergs, first-edition package layout), with application services distinct from domain services as in "Implementing Domain-Driven Design" (Vaughn Vernon). Use when the user wants to start a new Spring Boot service, add a use case or bounded context, or reorganize packages into ports and adapters.
---

# Hexagonal Spring Boot (Java 25)

Structure Spring Boot projects with the ports-and-adapters style from
*Get Your Hands Dirty on Clean Architecture*, in the package layout of its **first edition**. The business domain
sits at the center; web, persistence and other technology are replaceable adapters around it.

Following *Implementing Domain-Driven Design* (Vaughn Vernon), **application services** (in `application/service`)
run the use cases and own the cross-cutting concerns (transactions, security), while the **domain** stays plain Java;
**domain services** (in `domain/service`) are optional and exist only for a domain operation that spans several
aggregates. See "Source" at the end for why this departs from the second edition.

Ready-to-adapt code lives in [references/templates.md](references/templates.md). Read it before generating code.

## 1. Gather inputs

Ask only for what is missing, in one short message:

- Group ID, artifact ID and **base package** (for example `com.example.shop`)
- The **bounded contexts** of the application: a single one (default) or their names (for example `order`, `billing`)
- Persistence: Spring Data JPA (default), none, or other
- Single module (default) or multi-module (see section 6)

## 2. Package structure

The base package is the application. It holds the `@SpringBootApplication` class, **one** `common` package,
**one** `configuration` package, and the hexagon(s):

- **a single bounded context**: the hexagon sits directly in the base package;
- **several bounded contexts**: one package per context, directly under the base package, each its own hexagon.

```
<base>/                              <base>/
├── Application.java                 ├── Application.java
├── common/                          ├── common/
├── configuration/                   ├── configuration/
├── domain/                          ├── <context-a>/        # domain/, application/, adapter/ as below
├── application/                     └── <context-b>/
└── adapter/
     (one bounded context)                (several bounded contexts)
```

Layout of a hexagon (`<hexagon>` = `<base>` or `<base>/<context>`):

```
<hexagon>/
├── domain/                      # entities, value objects, domain rules (plain Java, no Spring at all)
│   └── service/                 # optional domain services: a domain operation spanning several aggregates
├── application/
│   ├── port/
│   │   ├── in/                  # incoming ports (use cases) + one command per use case
│   │   └── out/                 # outgoing ports
│   └── service/                 # application services *Service: implement incoming ports, own transactions and security
└── adapter/                     # outgoing adapter packages also hold their own *AdapterConfiguration (rule 7)
    ├── in/web/                  # controllers, web models
    ├── out/persistence/         # persistence adapter, JPA entities, mappers, Spring Data repositories
    └── out/<target-context>/    # DomainAdapter calling another bounded context of the application (rule 8)
```

- `common` holds utilities usable anywhere in the application (rule 10).
- `configuration` holds **one file per bounded context** (`<Context>Configuration`, for example `OrderConfiguration`,
  `BillingConfiguration`; with a single context, named after its domain, for example `SalesConfiguration`) with the
  `@Bean` methods that create its application services (rule 11), plus the general `@Configuration` classes (for
  example a `Clock` bean, security, web or Jackson settings).
- Configuration classes may depend on anything, since they assemble the application. **Nothing depends on
  `configuration`**: the hexagons never import it.
- The adapters are not created there: each adapter package holds its own `*AdapterConfiguration` (rule 7), so its
  classes stay package-private.
- The single `configuration` package and the per-adapter configuration classes are conventions of this skill, not
  rules of the book.

## 3. Rules

1. **Dependencies point inward.** `adapter` → `application` (ports and services) → `domain`.
   - `domain` (including `domain/service`) depends on nothing but `common`: no `application`, no adapters,
     **no Spring at all** (not even `@Transactional`), no ORM (JPA/Jakarta Persistence, Hibernate and similar),
     no Jackson or JSON annotations or types of any kind.
   - `application` (ports and `application/service`) may use `domain` and `common`, never the adapters. It is free
     of ORM and JSON. From Spring it may only use the **annotations of cross-cutting concerns** on application
     services: `@Transactional` (`org.springframework.transaction.annotation`) and method security (`@PreAuthorize`,
     `@Secured`...). **No `@Service` or `@Component`.**
   - **Lombok and JSpecify are allowed in `domain` and `application`** (compile-time annotations only, no runtime
     framework dependency). JSpecify (`org.jspecify.annotations`) carries the null-safety contract: mark the domain,
     service and port packages `@NullMarked` in a `package-info.java` and annotate the rare nullable value with `@Nullable`.
2. **One narrow incoming port per use case** (an interface in `port/in`), named as a noun for what it offers
   (for example `OrderPlacement`, `InvoiceLookup`). Avoid "broad" service interfaces.
3. **Outgoing ports** (interfaces in `port/out`) are named as a noun for the role they play, for example
   `OrderStore`, `InvoiceFinder`, `InvoiceIssuer`, and are defined by the application, implemented by adapters.
   **Interface names follow the usual Java conventions** ([Baeldung](https://www.baeldung.com/java-interface-naming-conventions)):
   a noun for a polymorphic interface (`List`, `Connection`) or an adjective for a capability (`Comparable`), the
   implementation combining its specialty with the interface name. **Never** a `Port` or `UseCase` suffix, an `I`
   prefix or an `Impl` suffix. Ports are recognized by their package, not by their name.
4. **Each use case has its own input model** (a command). It validates itself in its constructor.
   Use a Java `record` with a compact constructor. Business rules belong in the domain model, not in the command.
5. **Application services** in `application/service` implement the incoming port, orchestrate domain objects and
   outgoing ports, and own the cross-cutting concerns of the use case. The transaction boundary: `@Transactional` goes
   **on the methods, never on the class**, so each use case picks its own transaction (`@Transactional(readOnly = true)`
   for queries) instead of every public method opening a read-write transaction. Authorization, when needed, goes on
   the same methods (`@PreAuthorize`). They hold no business rule (that is the domain's job), carry no stereotype
   annotation and are created by `@Bean` methods (rule 11), so they are `public` with a public constructor.
   **Domain services** in `domain/service` are optional: create one only for a domain operation that does not belong
   to a single entity or value object, typically one spanning several aggregates. They are stateless plain Java,
   named after the operation in the ubiquitous language, use only `domain` types, and know nothing of transactions,
   security or ports; an application service calls them.
6. **Adapters own their models.** Web request/response records and JPA entities are separate from domain classes,
   with explicit mappers. Choose the mapping strategy per boundary (no mapping, two-way, full, one-way)
   and record any shortcut consciously, for example in an `ARCHITECTURE.md` or an ADR.
7. **Adapters never depend on each other, carry no stereotype and are package-private.** No `@Component`, `@Service`
   or `@Repository` on an adapter. Each outgoing adapter package holds its own `@Configuration` class (named
   `<Context><Kind>AdapterConfiguration`, for example `OrderPersistenceAdapterConfiguration`) with the `@Bean`
   methods that create its adapters, injecting the ports and use cases they need. Because the configuration class is
   in the same package, every adapter class, JPA entity and Spring Data repository can stay package-private.
   **Exception: controllers.** Since Spring Framework 6, MVC only detects handlers annotated with `@Controller` or
   `@RestController` (a type-level `@RequestMapping` is not enough), so a controller keeps `@RestController`, is
   found by component scanning and is not created in a configuration class. It is still package-private and handles
   errors with `@ExceptionHandler` methods in itself (not `@RestControllerAdvice`, which would be a second stereotype).
8. **Joining two bounded contexts of the application.** The source context defines an outgoing port
   (`application/port/out`, in its own language and types). It is implemented by a `<Target>DomainAdapter` in
   `adapter/out/<target-context>/`, which calls the target context's **incoming port** (a use case interface) directly and
   converts the source's types to the target's command. Nothing else of the target context may be used: not its
   domain, services, outgoing ports or adapters. The source's application layer only ever sees its own port.
9. **Calling another application** (another service, for example a third party) goes through its **public API**, for
   example a REST service, but it can be another technology (messaging, gRPC...). The source defines an outgoing port in its
   `application/port/out`, implemented by an adapter in `adapter/out/<target>/` (for example
   `<Target>RestAdapter`) that calls the target's public API and maps the response to the source's own model. Like every adapter, it is
   created by a `@Configuration` class in its package (rule 7).
   Outgoing HTTP goes through Spring's `RestClient`, built from the injected auto-configured `RestClient.Builder`, never
   through a raw HTTP client (`java.net.http`, Apache or OkHttp types in application code).
   There is **no compile-time dependency** between the two applications. If the API technology is not stated, ask the user.
10. **`common`** (one package, directly under the base package) holds utilities usable anywhere in the application
    (all its contexts and layers). It depends on neither `domain`, `application`, `adapter` nor `configuration`.
    It is not shared with other applications (rule 9).
11. **Wiring of a bounded context.** Its file in the `configuration` package, `<Context>Configuration`, declares one
    `@Bean` per application service (for example `orderPlacement(OrderStore store)` returning
    `new PlaceOrderService(store)`). Adapters are created in their own adapter packages (rule 7). Nothing depends on
    a `@Configuration` class.
12. **Naming**: interfaces as in rule 3, `*Service` (application services), `*Controller`, `*PersistenceAdapter`, `*DomainAdapter`, `<Target>RestAdapter` (or the technology used), `SpringData*Repository`, `<Context><Kind>AdapterConfiguration`, `<Context>Configuration`.
13. **Rich domain model, immutable value objects.** The domain is **not anemic** (an *anemic domain model* is one whose
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

- Records for commands, web models, value objects and IDs: they are immutable (rule 13). Entities are classes
  with behavior, not records.
- Lombok may be used in `domain` and `application` (for example on entities). Declare it as compile-only with the annotation processor configured.
- JSpecify may be used in `domain` and `application`: `@NullMarked` on packages, `@Nullable` where null is a legitimate value. Add `org.jspecify:jspecify` (version managed by the Spring Boot BOM) as a regular dependency.
- Sealed interfaces and pattern-matching `switch` for closed domain variants (for example result types).
- `var` for obvious local types only; keep signatures explicit.
- Do not enable preview features unless the user asks.
- Set the Java release to 25 in the build file and use a JDK 25 toolchain.
- Application configuration is YAML: `src/main/resources/application.yaml`, never `application.properties`.

For the Spring Boot version, use the latest stable release that supports Java 25 and confirm it on
https://start.spring.io rather than relying on memory.

## 5. Testing

For detailed testing guidance, use the `java-testing` skill of the `mk-java` plugin (general rules) and the
`spring-testing` skill of this plugin (Spring Boot mechanics).

**Priority: `@SpringBootTest` tests that cover a whole use case**, not slice tests. Each important use case gets tests
that start the application on a random port, call it through its real entry point (HTTP) and check the outcome:

- the database is a **real one in a container** (Testcontainers, same engine as production, wired with
  `@ServiceConnection`), never an embedded substitute such as H2;
- **third-party services are mocked with WireMock** (`@EnableWireMock` from `wiremock-spring-boot`, which starts it on a random port
  and publishes its URL as a property); verify the requests the adapter sends, and test the failure cases (for example a
  503 must roll the transaction back);
- clean the tables in `@BeforeEach` so tests do not depend on each other;
- put the shared setup in a composed annotation (for example `@IntegrationTest`) and a `@TestConfiguration` with the container.

**These tests run in the Maven integration-test phase.** Every `@SpringBootTest` (and so every test that starts the web server) is named `*IT` and is
run by `maven-failsafe-plugin` (`integration-test` and `verify` goals), which runs after packaging. Surefire keeps the
fast `*Test` tests (default naming). `mvn verify` runs both.

Complement with, in this order of importance:

- **Domain**: plain unit tests, no Spring (entities, value objects, self-validating commands).
- **Architecture**: an ArchUnit test that fails the build on dependency violations (template provided). It needs no
  server, so it is a `*Test` run by surefire.
- **Application services**: unit tests with mocked outgoing ports (Mockito, BDD notation), only for branching logic that is awkward to reach through the use case tests.
- **Slice tests** (`@WebMvcTest`, `@DataJpaTest`): only when a use case test cannot reach the behavior, for example
  exhaustive request validation or a complex query. They are the exception, not the default.

## 6. Assembly and boundaries

- Outgoing adapters are created by `@Bean` methods in the `@Configuration` class of their adapter package (rule 7).
  Controllers (`@RestController`), JPA entities and Spring Data repositories are found by Spring Boot's scanning.
- Application services carry no stereotype: wire them with `@Bean` methods in `configuration/<Context>Configuration` (rule 11).
  `@Transactional` (and method security) on their methods, never on the class, are the only Spring annotations in
  `application`; the domain has none.
- **Multi-module option**: separate Maven modules (`application` including its `domain` package, `adapter-web`,
  `adapter-persistence`, `bootstrap`) make illegal dependencies impossible to compile. Offer it for larger projects;
  single module plus ArchUnit is the default.

## 7. Workflow

1. Gather inputs (section 1).
2. Create the project: if the working directory is empty, generate the build file and `@SpringBootApplication` class by hand
   (or from Spring Initializr if reachable). If the project exists, inspect it first and do not overwrite files.
3. Create the package tree (section 2) and one **vertical slice** from `templates.md`
   (use case, command, application service, outgoing port, web adapter, persistence adapter, mappers, configuration).
4. Add the ArchUnit test and its dependency (latest release on Maven Central).
5. Add the use case integration tests (`*IT`, Testcontainers, WireMock) and the domain unit tests following section 5.
6. Run the build (`./mvnw verify`; Docker must be running for Testcontainers) and fix failures.
7. Report: the package tree, the shortcuts taken, and what remains to implement.

## 8. When adding to an existing project

- Add a new use case: new incoming port + command in `port/in`, application service in `application/service` (plus its
  `@Bean` in `configuration/<Context>Configuration`), any new outgoing port in `port/out`, then adapter changes. Never skip the port.
  Business rules go in `domain` (or a domain service in `domain/service` when they span several aggregates), not in
  the application service.
- Add a new bounded context: new `<base>/<context>/` package with the same internal layout and its file
  `configuration/<Context>Configuration`. If the application had a single context until now, move its `domain`,
  `application` and `adapter` packages from `<base>/` into `<base>/<context>/` (`common` and `configuration` stay).
- Connect two bounded contexts of the application: follow rule 8 (outgoing port + `*DomainAdapter`).
  Never import the target context's domain, services or adapters.
- Call another application: follow rule 9 (outgoing port + adapter calling the target's public API).
- To check conformance afterwards, delegate to the `spring-hexagonal-reviewer` agent.

## Source

- **Package layout: first edition** of Tom Hombergs' *Get Your Hands Dirty on Clean Architecture* (Packt), as in
  the book's code repository `thombergs/buckpal`, branch `single-module` (2021): `account/adapter/in/web`,
  `account/adapter/out/persistence`, `account/application/port/in`, `account/application/port/out`,
  `account/application/service` (`SendMoneyService`), `account/domain` (`Account`, `Money`... directly, no `model`).
  The repository switched to the second edition on 2023-06-09 (commit `ebdb694`, "Upgrade to 2nd edition").
- **Deliberate departure from the second edition**, whose layout (page 28, confirmed by the plugin author with a
  photo) nests `domain/model` and `domain/service` inside `application` and puts the use-case services in
  `domain/service`. There, cross-cutting concerns such as transactions and security end up in the domain and have to
  be tolerated as exceptions. Keeping `application/service` apart from `domain` puts them in the application layer.
- **Application services versus domain services**: Vaughn Vernon, *Implementing Domain-Driven Design*
  (Addison-Wesley, 2013), chapters "Application" (application services: use cases, transactions, security) and
  "Services" (domain services: optional, for domain operations that do not belong to one entity or value object).
- Chapter topics of Hombergs' book used: use cases and input validation, web and persistence adapters, testing,
  mapping strategies, assembling the application, conscious shortcuts and boundary enforcement.
- Rich domain model and value objects: Martin Fowler, "AnemicDomainModel" (https://martinfowler.com/bliki/AnemicDomainModel.html);
  value objects as immutable, compared by value: Eric Evans, *Domain-Driven Design* (Addison-Wesley, 2003), and Vaughn
  Vernon, *Implementing Domain-Driven Design*, chapter "Value Objects"; records: the Javadoc of `java.lang.Record`
  ("A record class is a shallowly immutable, transparent carrier for a fixed set of values").
