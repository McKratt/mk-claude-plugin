---
name: hexagonal-spring
description: Scaffold or restructure a Spring Boot project on Java 25 using hexagonal architecture as taught in "Get Your Hands Dirty on Clean Architecture" (Tom Hombergs). Use when the user wants to start a new Spring Boot service, add a use case or bounded context, or reorganize packages into ports and adapters.
---

# Hexagonal Spring Boot (Java 25)

Structure Spring Boot projects with the ports-and-adapters style from
*Get Your Hands Dirty on Clean Architecture*. The business domain sits at the center;
web, persistence and other technology are replaceable adapters around it.

Ready-to-adapt code lives in [references/templates.md](references/templates.md). Read it before generating code.

## 1. Gather inputs

Ask only for what is missing, in one short message:

- Group ID, artifact ID and **base package** (for example `com.example.shop`)
- **Component** name (for example `sales`) and its first **bounded context** name (for example `order`)
- Build tool: Maven (default) or Gradle (the templates and the test setup are written for Maven; translate them for Gradle)
- Persistence: Spring Data JPA (default), none, or other
- Single module (default) or multi-module (see section 6)

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
│   │   ├── model/               # entities, value objects, domain rules (framework-free)
│   │   └── service/             # *Service: implements incoming ports, uses outgoing ports
│   └── port/
│       ├── in/                  # incoming ports (use cases) + one command per use case
│       └── out/                 # outgoing ports
├── adapter/                     # outgoing adapter packages also hold their own *AdapterConfiguration (rule 7)
│   ├── in/web/                  # controllers, web models
│   ├── out/persistence/         # persistence adapter, JPA entities, mappers, Spring Data repositories
│   └── out/<target-context>/    # DomainAdapter calling another bounded context of the same component (rule 8)
└── configuration/               # @Configuration with @Bean methods wiring this hexagon's services (rule 11)
```

Application-wide configuration lives in a `configuration` package directly under the base package, next to the
components and the `@SpringBootApplication` class:

```
<base>/
├── Application.java            # @SpringBootApplication
├── configuration/              # general Spring Java config (@Configuration classes)
└── <component>/[<context>/]    # see above
```

- `configuration` holds general `@Configuration` classes only (for example a `Clock` bean, security, web or
  Jackson settings).
- Configuration classes may depend on anything, since they assemble the application. **Nothing depends on
  `configuration`**: components never import it.
- The services of a hexagon are wired in that hexagon's own `configuration` package (rule 11), not here.
  Both `configuration` packages are conventions of this skill, not rules of the book.

## 3. Rules

1. **Dependencies point inward.** `adapter` → `application` (ports and `domain/service`) → `domain/model`.
   - `domain/model` depends on nothing but `common`: no ports, services or adapters, no Spring, no ORM
     (JPA/Jakarta Persistence, Hibernate and similar), no Jackson or JSON annotations or types of any kind.
   - `domain/service` may use `domain/model`, the ports and `common`, never the adapters. It is also free of
     ORM and JSON, and of Spring annotations: **no `@Service` or `@Component`**. The single exception is
     `@Transactional`, an accepted exception to the purity of the domain.
   - **Lombok and JSpecify are allowed in the whole domain** (compile-time annotations only, no runtime framework
     dependency). JSpecify (`org.jspecify.annotations`) carries the null-safety contract: mark the domain and port
     packages `@NullMarked` in a `package-info.java` and annotate the rare nullable value with `@Nullable`.
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
5. **Services** in `application/domain/service` implement the incoming port, orchestrate domain objects and
   outgoing ports, and own the transaction boundary: `@Transactional` goes **on the methods, never on the class**, so each
   use case picks its own transaction (`@Transactional(readOnly = true)` for queries) instead of every public method
   opening a read-write transaction. They carry no stereotype annotation and are
   created by `@Bean` methods (rule 11), so they are `public` with a public constructor.
6. **Adapters own their models.** Web request/response records and JPA entities are separate from domain classes,
   with explicit mappers. Choose the mapping strategy per boundary (no mapping, two-way, full, one-way)
   and record any shortcut consciously, for example in an `ARCHITECTURE.md` or an ADR.
7. **Adapters never depend on each other, carry no stereotype and are package-private.** No `@Component`, `@Service`
   or `@Repository` on an adapter. Each outgoing adapter package holds its own `@Configuration` class (named
   `<Hexagon><Kind>AdapterConfiguration`, for example `OrderPersistenceAdapterConfiguration`) with the `@Bean`
   methods that create its adapters, injecting the ports and use cases they need. Because the configuration class is
   in the same package, every adapter class, JPA entity and Spring Data repository can stay package-private.
   **Exception: controllers.** Since Spring Framework 6, MVC only detects handlers annotated with `@Controller` or
   `@RestController` (a type-level `@RequestMapping` is not enough), so a controller keeps `@RestController`, is
   found by component scanning and is not created in a configuration class. It is still package-private and handles
   errors with `@ExceptionHandler` methods in itself (not `@RestControllerAdvice`, which would be a second stereotype).
8. **Joining two bounded contexts of the same component.** The source context defines an outgoing port
   (`application/port/out`, in its own language and types). It is implemented by a `<Target>DomainAdapter` in
   `adapter/out/<target-context>/`, which calls the target context's **incoming port** (a use case interface) directly and
   converts the source's types to the target's command. Nothing else of the target context may be used: not its
   domain, services, outgoing ports or adapters. The source's application layer only ever sees its own port.
9. **Joining two different components** goes through the **public API of the target component**, for example a REST
   service, but it can be another technology (messaging, gRPC...). The source defines an outgoing port in its
   `application/port/out`, implemented by an adapter in `adapter/out/<target-component>/` (for example
   `<Target>RestAdapter`) that calls the target's public API and maps the response to the source's own model. Like every adapter, it is
   created by a `@Configuration` class in its package (rule 7).
   Outgoing HTTP goes through Spring's `RestClient`, built from the injected auto-configured `RestClient.Builder`, never
   through a raw HTTP client (`java.net.http`, Apache or OkHttp types in application code).
   There is **no compile-time dependency** between the two components. If the API technology is not stated, ask the user.
10. **`common`** holds utilities usable anywhere in the component (all its contexts and layers). It depends on
    neither `application`, `adapter` nor any `configuration` package. It is not shared between different components (rule 9).
11. **Wiring of a hexagon.** A `@Configuration` class in `<hexagon>/configuration/` declares one `@Bean` per service
    (for example `orderPlacement(OrderStore store)` returning `new PlaceOrderService(store)`). Adapters are created
    in their own adapter packages (rule 7). Nothing depends on a `@Configuration` class.
12. **Naming**: interfaces as in rule 3, `*Service`, `*Controller`, `*PersistenceAdapter`, `*DomainAdapter`, `<Target>RestAdapter` (or the technology used), `SpringData*Repository`, `<Hexagon><Kind>AdapterConfiguration`.

## 4. Java 25 guidelines

- Records for commands, web models, value objects and IDs.
- Lombok may be used in `application/domain` (for example on entities in `model`). Declare it as compile-only with the annotation processor configured.
- JSpecify may be used in `application/domain` and the ports: `@NullMarked` on packages, `@Nullable` where null is a legitimate value. Add `org.jspecify:jspecify` (version managed by the Spring Boot BOM) as a regular dependency.
- Sealed interfaces and pattern-matching `switch` for closed domain variants (for example result types).
- `var` for obvious local types only; keep signatures explicit.
- Do not enable preview features unless the user asks.
- Set the Java release to 25 in the build file and use a JDK 25 toolchain.
- Application configuration is YAML: `src/main/resources/application.yaml`, never `application.properties`.

For the Spring Boot version, use the latest stable release that supports Java 25 and confirm it on
https://start.spring.io rather than relying on memory.

## 5. Testing

For detailed testing guidance, use the `java-testing` skill of this plugin.

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
- **Services**: unit tests with mocked outgoing ports (Mockito, BDD notation), only for branching logic that is awkward to reach through the use case tests.
- **Slice tests** (`@WebMvcTest`, `@DataJpaTest`): only when a use case test cannot reach the behavior, for example
  exhaustive request validation or a complex query. They are the exception, not the default.

## 6. Assembly and boundaries

- Outgoing adapters are created by `@Bean` methods in the `@Configuration` class of their adapter package (rule 7).
  Controllers (`@RestController`), JPA entities and Spring Data repositories are found by Spring Boot's scanning.
- Services are **not** annotated: wire them with `@Bean` methods in `<hexagon>/configuration/` (rule 11).
  `@Transactional` on service methods (never on the class) is the only Spring annotation allowed in the domain; document it in the architecture notes.
- **Multi-module option**: separate Maven/Gradle modules (`application` including its `domain` package, `adapter-web`,
  `adapter-persistence`, `bootstrap`) make illegal dependencies impossible to compile. Offer it for larger projects;
  single module plus ArchUnit is the default.

## 7. Workflow

1. Gather inputs (section 1).
2. Create the project: if the working directory is empty, generate the build file and `@SpringBootApplication` class by hand
   (or from Spring Initializr if reachable). If the project exists, inspect it first and do not overwrite files.
3. Create the package tree for the first component and one **vertical slice** from `templates.md`
   (use case, command, service, outgoing port, web adapter, persistence adapter, mappers).
4. Add the ArchUnit test and its dependency (latest release on Maven Central).
5. Add the use case integration tests (`*IT`, Testcontainers, WireMock) and the domain unit tests following section 5.
6. Run the build (`./mvnw verify` or `./gradlew build`; Docker must be running for Testcontainers) and fix failures.
7. Report: the package tree, the shortcuts taken, and what remains to implement.

## 8. When adding to an existing project

- Add a new use case: new incoming port + command in `port/in`, service in `application/domain/service` (plus its `@Bean` in
  `<hexagon>/configuration`), any new outgoing port in `port/out`, then adapter changes. Never skip the port.
- Add a new bounded context: new `<component>/<context>/` package with the same internal layout. If the component had a
  single context until now, move its packages from `<component>/` into `<component>/<context>/`.
- Connect two bounded contexts of the same component: follow rule 8 (outgoing port + `*DomainAdapter`).
  Never import the target context's domain, services or adapters.
- Connect two different components: follow rule 9 (outgoing port + adapter calling the target's public API).
- To check conformance afterwards, delegate to the `spring-hexagonal-reviewer` agent.

## Source

Structure and naming follow the book's package layout (`adapter/in/web`, `adapter/out/persistence`,
`application/port/in`, `application/port/out`, `application/domain/model`, `application/domain/service`, `common`) of Tom Hombergs'
*Get Your Hands Dirty on Clean Architecture*, 2nd edition (Packt, Java code examples). The plugin author owns this
edition and confirmed the layout with a photo of the package tree on page 28 (chapter "Organizing Code"):
`domain` nested in `application`, `model` and `service` inside `domain`, and a `common` package. The first edition may differ.
Chapter topics used: use cases and input validation, web and persistence adapters, testing, mapping strategies,
assembling the application, conscious shortcuts and boundary enforcement.
