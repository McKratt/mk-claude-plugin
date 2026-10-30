---
type: llm
---
The answer passes only if all of these hold:
1. It reports, as violations, at least these four problems: the domain class `Order` uses JPA (`jakarta.persistence`,
   `@Entity`); the incoming port `PlaceOrderUseCase` has a `UseCase` suffix; the application service
   `PlaceOrderService` imports the adapter `OrderPersistenceAdapter`; `PlaceOrderService` carries `@Service`.
2. It does not report the use of JSpecify (`@NullMarked` in `package-info.java`) or Lombok in the domain as a
   violation.
3. Findings are grouped as violations and observations, and each one names the file or package it concerns (a finding about something missing may name where it is expected instead).
