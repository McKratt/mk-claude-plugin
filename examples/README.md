# Examples

Runnable Java 25 projects that follow the hexagonal skills of this plugin and check them with ArchUnit.

| Project | Skill | Shows |
| --- | --- | --- |
| `single-context/` | `hexagonal-spring` | Spring Boot 4.1. Component `sales` with one bounded context: the context level is omitted from the packages. |
| `multi-context/` | `hexagonal-spring` | Spring Boot 4.1. Component `sales` with contexts `order` and `billing`, joined by `BillingDomainAdapter` (rule 8 of `hexagonal-spring`). |
| `helidon-single-context/` | `hexagonal-helidon` | Helidon SE 4.5 (LTS). The same `sales` use case, wired by hand without a container: DbClient + Flyway, WebClient, transactions through the `TransactionRunner` port. |
| `helidon-multi-context/` | `hexagonal-helidon` | Helidon SE 4.5 (LTS). Contexts `order` and `billing` joined by `BillingDomainAdapter`, one schema per context, one transaction shared through `sales/common/persistence/DbTransactions` (`OrderToInvoiceIT` proves the rollback across contexts). |

## Run

Requires JDK 25, Maven 3.10+ (enforced by maven-enforcer) and a running Docker daemon (Testcontainers starts PostgreSQL).

```bash
cd examples/single-context   # or multi-context, helidon-single-context, helidon-multi-context
mvn verify
```

Two kinds of tests, two Maven phases:

- `*Test` (surefire, `test` phase): fast tests without a web server, namely the domain and service unit tests and the ArchUnit rules.
- `*IT` (failsafe, `integration-test` phase): tests that start the web server and test a whole use
  case against a real PostgreSQL in a container (Testcontainers): `@SpringBootTest` in the Spring projects, `@ServerTest`
  in the Helidon one. `single-context` and `helidon-single-context` also replace the third-party shipping API with
  WireMock (`PlaceOrderIT`).

`ArchitectureRulesDetectViolationsTest` feeds deliberately bad fixture classes (in test sources, package `archfixtures`)
to the rules, so a rule that silently stops matching makes the build fail.

## Lombok

Each project has a `lombok.config` that turns `@Builder`, `@Data` and `@Value` into compile errors
(`lombok.<feature>.flagUsage = error`). Check it with:

```bash
examples/verify-lombok-ban.sh
```

## Status

The CI workflow `.github/workflows/examples.yml` runs the four projects and the Lombok check.
