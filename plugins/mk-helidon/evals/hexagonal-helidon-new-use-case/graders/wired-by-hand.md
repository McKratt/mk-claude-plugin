---
type: llm
---
The answer passes only if all of these hold:
1. The objects are created by hand (with `new`) in configuration classes or the main class; there is no dependency
   injection container.
2. The answer does not propose Helidon Injection (`@Service.Singleton`, `@Service.Inject`, the service registry),
   Helidon Declarative, Helidon Data, CDI or MicroProfile for this service.
3. The transaction is opened through an outgoing port (for example `TransactionRunner`) used by the application
   service, not by a Helidon or Jakarta annotation on the service.
