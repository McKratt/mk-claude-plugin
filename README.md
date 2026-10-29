# mk-claude-plugin

A Claude Code marketplace (`mckratt-plugins`) with three plugins for Java projects:

| Plugin | Depends on | Contents |
| ------ | ---------- | -------- |
| `mk-java` | | Java skills shared by the other plugins |
| `mk-spring` | `mk-java` | Spring Boot skills and agent |
| `mk-helidon` | `mk-java` | Helidon SE 4.x (LTS) skills and agent |

## Contents

| Plugin | Type | Name | Invocation | Purpose |
| ------ | ---- | ---- | ---------- | ------- |
| `mk-java` | Skill | `java-testing` | `/mk-java:java-testing` | Java tests, whatever the framework: use case tests first (Testcontainers, WireMock, failsafe), unit tests (AssertJ, BDD Mockito), slice tests as the exception |
| `mk-spring` | Skill | `hexagonal-spring` | `/mk-spring:hexagonal-spring` | Structure a Spring Boot / Java 25 project following *Get Your Hands Dirty on Clean Architecture* (first-edition layout) and Vernon's application vs. domain services |
| `mk-spring` | Skill | `spring-testing` | `/mk-spring:spring-testing` | Spring Boot 4 test mechanics: `@SpringBootTest` use case tests, `@ServiceConnection`, `wiremock-spring-boot`, `RestTestClient`, slice tests |
| `mk-spring` | Agent | `spring-hexagonal-reviewer` | `mk-spring:spring-hexagonal-reviewer` | Read-only review of a codebase against the `hexagonal-spring` rules |
| `mk-helidon` | Skill | `hexagonal-helidon` | `/mk-helidon:hexagonal-helidon` | Structure a Helidon SE 4.x (LTS) / Java 25 project following the same book, wired by hand without a container |
| `mk-helidon` | Agent | `helidon-hexagonal-reviewer` | `mk-helidon:helidon-hexagonal-reviewer` | Read-only review of a Helidon codebase against the `hexagonal-helidon` rules |

## Install

In a Claude Code session, add the marketplace, then install the plugin of your framework; `mk-java` is installed with
it as a dependency:

```
/plugin marketplace add McKratt/mk-claude-plugin
/plugin install mk-spring@mckratt-plugins
/plugin install mk-helidon@mckratt-plugins
```

To share it with everyone working in a repository, run from that repository's shell and commit the `.claude/settings.json` they write:

```bash
claude plugin marketplace add McKratt/mk-claude-plugin --scope project
claude plugin install mk-spring@mckratt-plugins --scope project   # or mk-helidon
```

The former single plugin `mk-claude-plugin` no longer exists: uninstall it (`/plugin uninstall mk-claude-plugin@mckratt-plugins`)
and install the new ones.

## Layout

```
.claude-plugin/marketplace.json            # makes this repository a marketplace listing the three plugins
plugins/<plugin>/.claude-plugin/plugin.json # manifest of each plugin (name, version, dependencies)
plugins/<plugin>/skills/<name>/SKILL.md    # one directory per skill
plugins/<plugin>/skills/<name>/references/ # supporting files a skill reads on demand (for example templates.md)
plugins/<plugin>/agents/<name>.md          # one file per subagent
examples/                                  # runnable Spring Boot and Helidon projects that follow the skills, built by CI
```

## Develop and test locally

```bash
claude plugin validate .                    # the marketplace
claude plugin validate plugins/mk-spring    # one plugin
claude --plugin-dir plugins/mk-java --plugin-dir plugins/mk-spring
```

Run `/reload-plugins` inside a session after editing files.

## Versioning

Each plugin follows [Semantic Versioning](https://semver.org/). Its version lives in its own
`plugins/<plugin>/.claude-plugin/plugin.json` and is bumped on every release of that plugin:

- **MAJOR**: breaking change (renamed or removed skill/agent, changed behavior users rely on)
- **MINOR**: new skill or agent, backwards-compatible
- **PATCH**: backwards-compatible fixes and wording improvements

Current versions: `mk-java` `0.0.5`, `mk-spring` `0.0.5`, `mk-helidon` `0.0.5` (initial development, anything may change;
they continue the numbering of the former single plugin `mk-claude-plugin`, last at `0.0.4`).

## References

- Claude Code docs: [Create a plugin](https://code.claude.com/docs/en/plugins/create.md), [Plugins reference](https://code.claude.com/docs/en/plugins-reference.md), [Plugin dependencies](https://code.claude.com/docs/en/plugins/dependencies.md), [Create a marketplace](https://code.claude.com/docs/en/plugin-marketplaces.md), [Host and maintain a marketplace](https://code.claude.com/docs/en/plugins/host-marketplace.md), [Subagents](https://code.claude.com/docs/en/sub-agents.md)
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture* (Packt): package layout of the first edition, as in the book repository [`thombergs/buckpal`, branch `single-module`](https://github.com/thombergs/buckpal/tree/single-module)
- Vaughn Vernon, *Implementing Domain-Driven Design* (Addison-Wesley, 2013): application services vs. domain services
- Java interface naming: [Baeldung, Java interface naming conventions](https://www.baeldung.com/java-interface-naming-conventions)
- Spring Boot: [REST clients (`RestClient`)](https://docs.spring.io/spring-boot/reference/io/rest-client.html), [Testing Spring Boot applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html), [4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- Helidon 4.x (LTS): [documentation](https://helidon.io/docs/v4/) (Markdown sources in [`helidon-io/helidon`, tag 4.5.6, `docs/`](https://github.com/helidon-io/helidon/tree/4.5.6/docs)), [examples, branch `helidon-4.x`](https://github.com/helidon-io/helidon-examples/tree/helidon-4.x), LTS status in the [CHANGELOG](https://github.com/helidon-io/helidon/blob/main/CHANGELOG.md) (Helidon 27 entry)
- WireMock: [Spring Boot integration](https://wiremock.org/docs/spring-boot/); known h2c issue with the JDK HTTP client and the standalone WireMock: [#2637](https://github.com/wiremock/wiremock/issues/2637), [#3577](https://github.com/wiremock/wiremock/issues/3577)

## License

See [LICENSE](LICENSE).
