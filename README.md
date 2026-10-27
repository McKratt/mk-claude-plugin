# mk-claude-plugin

Personal Claude Code plugin: skills and agents for Java / Spring Boot projects.

## Contents

| Type  | Name                | Invocation                         | Purpose                                                                                   |
| ----- | ------------------- | ---------------------------------- | ----------------------------------------------------------------------------------------- |
| Skill | `hexagonal-spring`  | `/mk-claude-plugin:hexagonal-spring` | Structure a Spring Boot / Java 25 project following *Get Your Hands Dirty on Clean Architecture* |
| Skill | `java-testing` | `/mk-claude-plugin:java-testing` | Java tests: use case `@SpringBootTest` tests first (Testcontainers, WireMock, failsafe), unit tests, slice tests as the exception |
| Agent | `spring-hexagonal-reviewer` | `mk-claude-plugin:spring-hexagonal-reviewer` | Read-only review of a codebase against the `hexagonal-spring` rules                       |

## Layout

```
.claude-plugin/plugin.json      # manifest
skills/<name>/SKILL.md          # one directory per skill
skills/<name>/references/       # supporting files a skill reads on demand (for example templates.md)
agents/<name>.md                # one file per subagent
examples/                       # runnable Spring Boot projects that follow the skills, built by CI
```

## Develop and test locally

```bash
claude plugin validate .
claude --plugin-dir .
```

Run `/reload-plugins` inside a session after editing files.

## Versioning

This plugin follows [Semantic Versioning](https://semver.org/). The version lives in
`.claude-plugin/plugin.json` and is bumped on every release:

- **MAJOR**: breaking change (renamed or removed skill/agent, changed behaviour users rely on)
- **MINOR**: new skill or agent, backwards-compatible
- **PATCH**: backwards-compatible fixes and wording improvements

Current version: `0.0.2` (initial development, anything may change).

## References

- Claude Code docs: [Create a plugin](https://code.claude.com/docs/en/plugins/create.md), [Subagents](https://code.claude.com/docs/en/sub-agents.md)
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture*, 2nd edition (Packt)
- Java interface naming: [Baeldung, Java interface naming conventions](https://www.baeldung.com/java-interface-naming-conventions)
- Spring Boot: [REST clients (`RestClient`)](https://docs.spring.io/spring-boot/reference/io/rest-client.html), [Testing Spring Boot applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html), [4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- WireMock: [Spring Boot integration](https://wiremock.org/docs/spring-boot/); known h2c issue with the JDK HTTP client and the standalone WireMock: [#2637](https://github.com/wiremock/wiremock/issues/2637), [#3577](https://github.com/wiremock/wiremock/issues/3577)

## License

See [LICENSE](LICENSE).
