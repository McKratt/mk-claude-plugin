---
type: llm
---
The answer passes only if all of these hold:
1. It reports, as violations, at least these three problems: the domain class `Order` imports Helidon DbClient
   (`io.helidon.dbclient.DbRow`); the application service `PlaceOrderService` uses `java.net.http.HttpClient`
   instead of an outgoing port and adapter (or Helidon `WebClient`); the outgoing port `IStockChecker` has an `I`
   prefix.
2. It mentions the Helidon Injection annotation `@Service.Singleton` on `PlaceOrderService` (as a violation or an
   observation).
3. It does not report the use of Lombok or JSpecify in the domain as a violation.
4. Findings are grouped as violations and observations, and each one names the file or package it concerns (a finding about something missing may name where it is expected instead).
