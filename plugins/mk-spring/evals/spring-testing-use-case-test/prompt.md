---
tags: [spring-testing]
plugins: [../../../mk-spring, ../../../mk-java]
max_turns: 15
timeout_seconds: 300
allowed_tools: [Skill, Read, Glob, Grep]
---

My Spring Boot 4 application on Java 25 (base package `com.acme.shop`, Maven) has a use case `POST /orders`: it stores
the order in PostgreSQL through Spring Data JPA, then calls a third-party shipping service over HTTP whose base URL is
the property `shipping.base-url`. Write the test that covers this use case, including the case where the shipping
service answers 503 (the order must then not be stored).

Do not create any file and do not ask me anything: put the complete test class in your answer, then one sentence on
which Maven plugin runs it.
