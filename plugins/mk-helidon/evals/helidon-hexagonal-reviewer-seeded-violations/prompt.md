---
tags: [helidon-hexagonal-reviewer]
plugins: [../../../mk-helidon, ../../../mk-java]
max_turns: 20
timeout_seconds: 600
allowed_tools: [Agent, Read, Glob, Grep]
---

Have the `helidon-hexagonal-reviewer` agent review the architecture of the Helidon SE project in the current
directory. Do not fix anything. Answer with the agent's findings in full, each with its file, grouped as violations
and observations.
