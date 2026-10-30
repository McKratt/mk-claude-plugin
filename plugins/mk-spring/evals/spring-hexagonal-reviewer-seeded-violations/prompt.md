---
tags: [spring-hexagonal-reviewer]
plugins: [../../../mk-spring, ../../../mk-java]
max_turns: 20
timeout_seconds: 600
allowed_tools: [Agent, Read, Glob, Grep]
---

Have the `spring-hexagonal-reviewer` agent review the architecture of the Spring Boot project in the current
directory. Do not fix anything. Answer with the agent's findings in full, each with its file, grouped as violations
and observations.
