# mk-claude-plugin

Personal Claude Code plugin: skills and agents for Java projects (Spring Boot, Helidon SE).

## Contents

| Type  | Name                | Invocation                         | Purpose                                                                                   |
| ----- | ------------------- | ---------------------------------- | ----------------------------------------------------------------------------------------- |
| Skill | `hexagonal-spring`  | `/mk-claude-plugin:hexagonal-spring` | Structure a Spring Boot / Java 25 project following *Get Your Hands Dirty on Clean Architecture* |
| Skill | `java-testing` | `/mk-claude-plugin:java-testing` | Java tests: use case `@SpringBootTest` tests first (Testcontainers, WireMock, failsafe), unit tests, slice tests as the exception |
| Skill | `hexagonal-helidon` | `/mk-claude-plugin:hexagonal-helidon` | Structure a Helidon SE 4.x (LTS) / Java 25 project following the same book, wired by hand without a container |
| Agent | `spring-hexagonal-reviewer` | `mk-claude-plugin:spring-hexagonal-reviewer` | Read-only review of a codebase against the `hexagonal-spring` rules                       |
| Agent | `helidon-hexagonal-reviewer` | `mk-claude-plugin:helidon-hexagonal-reviewer` | Read-only review of a Helidon codebase against the `hexagonal-helidon` rules |

## Install

In a Claude Code session:

```
/plugin marketplace add McKratt/mk-claude-plugin
/plugin install mk-claude-plugin@mckratt-plugins
```

To share it with everyone working in a repository, run from that repository's shell and commit the `.claude/settings.json` they write:

```bash
claude plugin marketplace add McKratt/mk-claude-plugin --scope project
claude plugin install mk-claude-plugin@mckratt-plugins --scope project
```

## Layout

```
.claude-plugin/plugin.json      # manifest
.claude-plugin/marketplace.json # makes this repository a marketplace listing the plugin
skills/<name>/SKILL.md          # one directory per skill
skills/<name>/references/       # supporting files a skill reads on demand (for example templates.md)
agents/<name>.md                # one file per subagent
examples/                       # runnable Spring Boot and Helidon projects that follow the skills, built by CI
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

- **MAJOR**: breaking change (renamed or removed skill/agent, changed behavior users rely on)
- **MINOR**: new skill or agent, backwards-compatible
- **PATCH**: backwards-compatible fixes and wording improvements

Current version: `0.1.0` (initial development, anything may change).

## References

- Claude Code docs: [Create a plugin](https://code.claude.com/docs/en/plugins/create.md), [Create a marketplace](https://code.claude.com/docs/en/plugin-marketplaces.md), [Host and maintain a marketplace](https://code.claude.com/docs/en/plugins/host-marketplace.md), [Subagents](https://code.claude.com/docs/en/sub-agents.md)
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture*, 2nd edition (Packt)
- Java interface naming: [Baeldung, Java interface naming conventions](https://www.baeldung.com/java-interface-naming-conventions)
- Spring Boot: [REST clients (`RestClient`)](https://docs.spring.io/spring-boot/reference/io/rest-client.html), [Testing Spring Boot applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html), [4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- Helidon 4.x (LTS): [documentation](https://helidon.io/docs/v4/) (Markdown sources in [`helidon-io/helidon`, tag 4.5.6, `docs/`](https://github.com/helidon-io/helidon/tree/4.5.6/docs)), [examples, branch `helidon-4.x`](https://github.com/helidon-io/helidon-examples/tree/helidon-4.x), LTS status in the [CHANGELOG](https://github.com/helidon-io/helidon/blob/main/CHANGELOG.md) (Helidon 27 entry)
- WireMock: [Spring Boot integration](https://wiremock.org/docs/spring-boot/); known h2c issue with the JDK HTTP client and the standalone WireMock: [#2637](https://github.com/wiremock/wiremock/issues/2637), [#3577](https://github.com/wiremock/wiremock/issues/3577)

## License

See [LICENSE](LICENSE).
