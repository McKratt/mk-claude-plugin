# Examples

Two runnable Spring Boot 4.1 / Java 25 projects that follow the `hexagonal-spring` skill and check it with ArchUnit.

| Project | Shows |
| --- | --- |
| `single-context/` | Component `sales` with one bounded context: the context level is omitted from the packages. |
| `multi-context/` | Component `sales` with contexts `order` and `billing`, joined by `BillingDomainAdapter` (rule 8 of `hexagonal-spring`). |

## Run

Requires JDK 25, Maven 3.10+ (enforced by maven-enforcer) and a running Docker daemon (Testcontainers starts PostgreSQL).

```bash
cd examples/single-context   # or multi-context
mvn verify
```

Two kinds of tests, two Maven phases:

- `*Test` (surefire, `test` phase): fast tests without a web server, namely the domain and service unit tests and the ArchUnit rules.
- `*IT` (failsafe, `integration-test` phase): `@SpringBootTest` tests that start the web server and test a whole use
  case against a real PostgreSQL in a container (Testcontainers). `single-context` also replaces the third-party shipping API
  with WireMock (`PlaceOrderIT`).

`ArchitectureRulesDetectViolationsTest` feeds deliberately bad fixture classes (in test sources, package `archfixtures`)
to the rules, so a rule that silently stops matching makes the build fail.

## Lombok

Each project has a `lombok.config` that turns `@Builder`, `@Data` and `@Value` into compile errors
(`lombok.<feature>.flagUsage = error`). Check it with:

```bash
examples/verify-lombok-ban.sh
```

## Status

The CI workflow `.github/workflows/examples.yml` runs both projects and the Lombok check.
