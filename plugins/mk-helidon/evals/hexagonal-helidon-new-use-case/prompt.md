---
tags: [hexagonal-helidon]
plugins: [../../../mk-helidon, ../../../mk-java]
max_turns: 15
timeout_seconds: 300
allowed_tools: [Skill, Read, Glob, Grep]
---

New Helidon SE service on Java 25: group `com.acme`, artifact `shop`, base package `com.acme.shop`, a single bounded
context, Helidon DbClient over JDBC on PostgreSQL with Flyway. Its first use case is placing an order: the order is
saved and the stock of each product is decremented, both in the same database transaction.

Do not create any file and do not ask me anything. Answer with:
1. the list of Java source files of this vertical slice: one line per file, with its full path under `src/main/java/`
   and what the file is;
2. two or three sentences on how the objects are created and wired together.
