# mk-claude-plugin

Personal Claude Code plugin: skills and agents for Java / Spring Boot projects.

## Contents

| Type  | Name                | Invocation                         | Purpose                                                                                   |
| ----- | ------------------- | ---------------------------------- | ----------------------------------------------------------------------------------------- |
| Skill | `hexagonal-spring`  | `/mk-claude-plugin:hexagonal-spring` | Structure a Spring Boot / Java 25 project following *Get Your Hands Dirty on Clean Architecture* |
| Agent | `spring-hexagonal-reviewer` | `mk-claude-plugin:spring-hexagonal-reviewer` | Read-only review of a codebase against the same architecture rules                        |

## Layout

```
.claude-plugin/plugin.json      # manifest
skills/<name>/SKILL.md          # one directory per skill
agents/<name>.md                # one file per subagent
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

Current version: `0.0.1` (initial development, anything may change).

## References

- Claude Code docs: [Create a plugin](https://code.claude.com/docs/en/plugins/create.md), [Subagents](https://code.claude.com/docs/en/sub-agents.md)
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture*, 2nd edition (Packt)

## License

See [LICENSE](LICENSE).
