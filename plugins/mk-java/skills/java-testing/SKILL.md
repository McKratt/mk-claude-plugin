---
name: java-testing
description: Use when writing, reviewing, or fixing Java tests, whatever the framework. Prioritizes tests that cover a whole use case through the application's real entry point (real database in a container with Testcontainers, third parties mocked with WireMock, run by Maven failsafe), then unit tests (JUnit, AssertJ, BDDMockito). Framework mechanics live in the spring-testing skill (mk-spring plugin) and in the testing section of the hexagonal-helidon skill (mk-helidon plugin).
---

# Java Testing

Framework-independent rules for Java tests. The framework mechanics (how to start the application in a test, wire the
container and WireMock, call the application) are in:

- **Spring Boot**: the `spring-testing` skill of the `mk-spring` plugin;
- **Helidon SE**: the "Testing" section of the `hexagonal-helidon` skill of the `mk-helidon` plugin.

Based on the `java-testing` skill of McKratt/claude-skills, with one deliberate change: **tests that cover a whole use
case come first; framework slice tests are the exception.**

## Stack

| Concern         | Library                                                                         |
|-----------------|---------------------------------------------------------------------------------|
| Test runner     | JUnit Jupiter (`@Test`, `@ExtendWith`, `@ParameterizedTest`), version from the framework's BOM |
| Assertions      | AssertJ (`assertThat(...)`)                                                     |
| Mocking         | Mockito via BDD notation (`BDDMockito`), for unit tests only                    |
| Database        | Real database in a container: Testcontainers (never H2 or another embedded substitute) |
| Third parties   | WireMock, on a random port                                                      |
| Build           | `maven-surefire-plugin` for `*Test`, `maven-failsafe-plugin` for `*IT`           |

---

## Which test for what

In order of priority:

1. **Use case tests (`*IT`)**: the application starts on a random port, the test enters through the real entry point
   (HTTP) and checks the outcome end to end: response, database content, requests sent to third parties. Cover the
   main path of every use case, plus its important failure paths (validation error, third party down, rollback).
2. **Unit tests (`*Test`)** for logic that needs no framework: domain objects, value objects, self-validating commands,
   mappers, and application services with branching logic that is awkward to reach through the use case tests.
3. **Architecture tests (ArchUnit, `*Test`)**: no server needed, so surefire runs them.
4. **Framework slice tests** (for example Spring's `@WebMvcTest`, Helidon's `@RoutingTest`): only when a use case test
   cannot reach the behavior reasonably, for example exhaustive request-validation combinations or a complex query with
   many edge cases. Say why in the test class. Never write one by default or "per layer".

Do not mock your own classes in use case tests: the point is to exercise the real wiring. Mock only what is outside the
application, and do it at the network boundary (WireMock), not by replacing an object of the application.

---

## Maven: unit tests in `test`, server tests in `integration-test`

Every test that starts the web server is named `*IT` and **runs in the Maven integration-test phase** through
failsafe. Surefire keeps the fast `*Test` classes. `mvn verify` runs both; `mvn test` runs only the fast ones.

```xml
<plugin>
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
```

Testcontainers needs a running Docker daemon.

---

## Use case tests: rules

- **Real database**: the same engine and major version as production, in a container. Never an embedded substitute.
- **One container for all tests**: start it once per JVM (framework-managed bean, or a static singleton), not per class.
- **Isolation**: clean the tables in `@BeforeEach` (or use unique ids and query by id), reset WireMock's stubs. Never
  rely on test order.
- **Third parties**: WireMock on a random port, its base URL injected into the application's configuration.
- **Verify what you send**: `wireMock.verify(...)` on the path, headers and body the adapter produces.
- **Failure paths are part of the use case**: stub 4xx/5xx and timeouts, and assert the consequence (HTTP status,
  rollback: nothing stored).
- **Real wiring**: the test starts the application with its own assembly (Spring context, Helidon configuration
  classes); only the addresses of the container and of WireMock change.

---

## Structure: Arrange / Act / Assert

Every test method follows AAA with explicit comments when the sections are non-trivial:

```java
@Test
void should_save_the_order_when_it_is_placed() {
    // Arrange
    var command = new PlaceOrderCommand(List.of(new PlaceOrderCommand.Line("SKU-1", 2)));

    // Act
    var orderId = placeOrderService.placeOrder(command);

    // Assert
    then(orderStore).should().save(argThat(order -> order.id().equals(orderId)));
}
```

---

## Unit tests

### Class setup

```java
@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {

    @Mock
    private OrderStore orderStore;            // outgoing port: mocked

    @InjectMocks
    private PlaceOrderService placeOrderService;
}
```

### BDD Mockito notation

Always use `BDDMockito` static imports, never `Mockito.when` / `Mockito.verify`:

```java
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
```

```java
given(repository.findById("123")).willReturn(Optional.of(entity));
given(repository.save(any(Order.class))).willAnswer(i -> i.getArgument(0));
willThrow(new DataAccessException("db down") {}).given(repository).findById("x");

then(repository).should().save(any(Order.class));
then(repository).should(never()).delete(any());
then(repository).shouldHaveNoMoreInteractions();
```

### AssertJ assertions

Prefer specific assertions over `isEqualTo(true)`:

```java
assertThat(results)
    .hasSize(2)
    .extracting(Order::getId)
    .containsExactly("1", "2");

assertThatThrownBy(() -> service.findById(null))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("id must not be null");

assertThat(result).isPresent().contains(expectedOrder);

assertSoftly(softly -> {
    softly.assertThat(order.getId()).isEqualTo("123");
    softly.assertThat(order.getStatus()).isEqualTo(Status.PENDING);
});
```

---

## Naming

Test method names use `snake_case` following `should_<expected_behavior>_when_<condition>`:

```
should_return_order_when_id_exists
should_throw_not_found_exception_when_id_missing
should_send_email_when_order_is_shipped
```

Class names: `PlaceOrderService` is tested by `PlaceOrderServiceTest`; a use case test that starts the application is named after
the use case with the `IT` suffix: `PlaceOrderIT`.

---

## What NOT to do

- Do not write slice tests by default, and do not test "per layer" with mocks of your own classes when a use case test would cover it
- Do not use H2 or any embedded database in place of the real engine
- Do not call real third-party services from tests: stub them with WireMock
- Do not run tests that start the web server in surefire: name them `*IT` so failsafe runs them in the integration-test phase
- Do not use `Mockito.when(...)` or `Mockito.verify(...)`: always use BDD notation
- Do not use `assertTrue(result != null)`: use AssertJ (`assertThat(result).isNotNull()`)
- Do not share mutable test state between test methods
- Do not use camelCase for test method names: always use snake_case

## Source

Adapted from `skills/java-testing/skill.md` in McKratt/claude-skills. The use case test rules follow the `examples/`
projects of the McKratt/mk-claude-plugin repository (Spring Boot and Helidon), which are built by CI.
