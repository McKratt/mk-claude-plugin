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
plugins/<plugin>/evals/<case>/             # eval cases of its skills and agents (prompt.md + graders/), run by `claude plugin eval`
tests/check_plugin.py                      # static checks: manifests, frontmatter, links, README, eval coverage
examples/                                  # runnable Spring Boot and Helidon projects that follow the skills, built by CI
```

## Develop and test locally

```bash
claude plugin validate .                    # the marketplace
claude plugin validate plugins/mk-spring    # one plugin
claude --plugin-dir plugins/mk-java --plugin-dir plugins/mk-spring
```

Run `/reload-plugins` inside a session after editing files.

## Tests

Two levels, both run by the [`plugin`](.github/workflows/plugin.yml) workflow:

- **Static checks** (free, every push): `claude plugin validate` on the marketplace and each plugin, and
  `python3 tests/check_plugin.py`. The script fails if a skill or agent has no eval case tagged with its name, if a
  plugin has no case tagged `negative`, or if a case does not load its plugin and the plugin's dependencies.
- **Evals** (real Claude sessions, on pull requests and on demand, one matrix job per plugin): one directory per case
  under `plugins/<plugin>/evals/`, with the prompt in `prompt.md` and one grader per file in `graders/` (mostly
  regexes on the answer, an LLM rubric where a regex cannot judge). In the `prompt.md` frontmatter, `tags` names the
  skill or agent the case covers and `plugins` loads the plugin and its dependencies (`mk-java`), relative to the case
  directory. The eval therefore runs from the repository root, with `--eval-dir` pointing at the plugin's cases. The reviewer cases
  copy a small project with seeded violations (`fixture/`) into the sandbox through `scaffold.sh`. Every case also
  runs without the plugins, so the report shows what they add. The workflow needs the repository secret
  `ANTHROPIC_API_KEY`; without it the eval jobs are skipped with a warning.

```bash
python3 tests/check_plugin.py
claude plugin eval . --eval-dir plugins/mk-spring/evals --scaffold --no-publish     # one plugin, 3 runs per case, with and without it
claude plugin eval . --eval-dir plugins/mk-spring/evals --scaffold --no-publish --case 'spring-testing-*' --runs 1 --ablation none   # quick iteration
```

When adding a skill or agent, add at least one eval case tagged with its name.

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
