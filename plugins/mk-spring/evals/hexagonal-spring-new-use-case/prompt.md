---
tags: [hexagonal-spring]
plugins: [../../../mk-spring, ../../../mk-java]
max_turns: 15
timeout_seconds: 300
allowed_tools: [Skill, Read, Glob, Grep]
---

New Spring Boot service on Java 25: group `com.acme`, artifact `shop`, base package `com.acme.shop`, a single bounded
context, Spring Data JPA, single Maven module. Its first use case is placing an order (a customer id and a list of
product ids with quantities; the order is saved).

Do not create any file and do not ask me anything. Answer with the list of Java source files of this vertical slice:
one line per file, with its full path under `src/main/java/` and what the file is.
