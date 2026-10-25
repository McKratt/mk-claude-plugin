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
- First **bounded context / component** name (for example `order`)
- Build tool: Maven (default) or Gradle
- Persistence: Spring Data JPA (default), none, or other
- Single module (default) or multi-module (see section 6)

## 2. Package structure

One top-level package per component, with this layout inside:

```
<base>/<component>/
├── application/
│   ├── domain/                  # entities, value objects, domain rules
│   ├── port/
│   │   ├── in/                  # incoming ports: *UseCase + one command per use case
│   │   └── out/                 # outgoing ports: *Port
│   └── service/                 # implements incoming ports, uses outgoing ports
└── adapter/
    ├── in/web/                  # controllers, web models
    └── out/persistence/         # persistence adapter, JPA entities, mappers, Spring Data repositories
```

Application-wide configuration lives in a `configuration` package directly under the base package, next to the
components and the `@SpringBootApplication` class:

```
<base>/
├── Application.java            # @SpringBootApplication
├── configuration/              # general Spring Java config (@Configuration classes)
└── <component>/                # see above
```

- `configuration` holds general `@Configuration` classes only (for example a `Clock` bean, security, web or
  Jackson settings).
- Configuration classes may depend on anything, since they assemble the application. **Nothing depends on
  `configuration`**: components never import it.
- Component-specific wiring stays inside the component. The `configuration` package
  is a convention of this skill, not a rule of the book.

## 3. Rules

1. **Dependencies point inward.** `adapter` → `application` (ports and services) → `application/domain`.
   The domain package sits inside `application` but depends on nothing else, not even on ports or services:
   no Spring, no ORM (JPA/Jakarta Persistence, Hibernate and similar) and no Jackson or JSON annotations or types of any kind.
   **Lombok is allowed in the domain** (and only compile-time annotations like it), because it adds no runtime dependency on a framework.
2. **One narrow incoming port per use case**, named `<Verb><Noun>UseCase`
   (for example `PlaceOrderUseCase`). Avoid "broad" service interfaces.
3. **Outgoing ports** are named `<Action><Noun>Port` (for example `SaveOrderPort`, `LoadOrderPort`)
   and are defined by the application, implemented by adapters.
4. **Each use case has its own input model** (a command). It validates itself in its constructor.
   Use a Java `record` with a compact constructor. Business rules belong in the domain model, not in the command.
5. **Services** in `application/service` implement the incoming port, orchestrate domain objects and outgoing ports,
   and own the transaction boundary.
6. **Adapters own their models.** Web request/response records and JPA entities are separate from domain classes,
   with explicit mappers. Choose the mapping strategy per boundary (no mapping, two-way, full, one-way)
   and record any shortcut consciously, for example in an `ARCHITECTURE.md` or an ADR.
7. **Adapters never depend on each other.** Spring Data repositories and JPA entities stay package-private
   inside the persistence adapter package where possible.
8. **Naming**: `*UseCase`, `*Port`, `*Service`, `*Controller`, `*PersistenceAdapter`, `SpringData*Repository`.

## 4. Java 25 guidelines

- Records for commands, web models, value objects and IDs.
- Lombok may be used in `application/domain` (for example on entities). Declare it as compile-only with the annotation processor configured.
- Sealed interfaces and pattern-matching `switch` for closed domain variants (for example result types).
- `var` for obvious local types only; keep signatures explicit.
- Do not enable preview features unless the user asks.
- Set the Java release to 25 in the build file and use a JDK 25 toolchain.

For the Spring Boot version, use the latest stable release that supports Java 25 and confirm it on
https://start.spring.io rather than relying on memory.

## 5. Testing

Mirror the book's testing split:

- **Domain**: plain unit tests, no Spring.
- **Services**: unit tests with mocked outgoing ports (Mockito).
- **Web adapter**: `@WebMvcTest` slice tests with a mocked use case.
- **Persistence adapter**: `@DataJpaTest` (or Testcontainers when behaviour depends on the real database).
- **System**: a few `@SpringBootTest` tests through the whole stack.
- **Architecture**: an ArchUnit test that fails the build on dependency violations (template provided).

## 6. Assembly and boundaries

- Default to Spring classpath scanning (`@Service`, `@RestController`, `@Component` on adapters).
  Use explicit `@Configuration` with `@Bean` methods, kept inside the component, when you want the application layer free of Spring annotations.
- `@Transactional` and `@Service` on services are accepted pragmatic shortcuts. Note them in the architecture notes.
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
5. Add tests for each layer following section 5.
6. Run the build (`./mvnw verify` or `./gradlew build`) and fix failures.
7. Report: the package tree, the shortcuts taken, and what remains to implement.

## 8. When adding to an existing project

- Add a new use case: new `*UseCase` + command in `port/in`, service in `application/service`,
  any new `*Port` in `port/out`, then adapter changes. Never skip the port.
- Add a new component: new top-level package with the same internal layout. Communicate between components
  through ports, not by importing another component's domain or adapters.
- To check conformance afterwards, delegate to the `hexagonal-reviewer` agent.

## Source

Structure and naming follow the book's package layout (`adapter/in/web`, `adapter/out/persistence`,
`application/port/in`, `application/port/out`, `application/service`, `application/domain`) as in Hombergs' book.
The domain package being nested in `application` is a correction provided by the plugin author, who owns the book.
Chapter topics used: use cases and input validation, web and persistence adapters, testing, mapping strategies,
assembling the application, conscious shortcuts and boundary enforcement.
