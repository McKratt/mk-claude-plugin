---
tags: [negative]
plugins: [../../../mk-helidon, ../../../mk-java]
max_turns: 5
timeout_seconds: 120
allowed_tools: [Skill, Read, Glob, Grep]
---

Write a Python function `parse_duration(text: str) -> int` that converts strings such as `1h30m` or `45s` to a number
of seconds. Do not create any file: answer with the code only.
