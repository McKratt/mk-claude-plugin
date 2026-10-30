#!/usr/bin/env python3
"""Static checks of the marketplace and its plugins: manifests, skill and agent frontmatter, links, README, and
eval coverage.

Every skill and every agent of a plugin must be covered by at least one eval case under plugins/<plugin>/evals/
whose prompt.md frontmatter lists its name in `tags`, and each plugin needs at least one case tagged `negative` (a
prompt where none of its skills may fire). The evals themselves run with `claude plugin eval` (see "Tests" in
README.md).

Usage: python3 tests/check_plugin.py   (standard library only)
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRADER_TYPES = {"regex", "tool_order", "tool_used", "file_exists", "llm", "baseline"}
PROMPT_KEYS = {"schema_version", "name", "description", "tags", "plugins", "runs", "expected_outcome", "model",
               "max_turns", "timeout_seconds", "allowed_tools", "disallowed_tools", "artifact_publish",
               "growthbook_overrides", "append_system_prompt", "env"}
LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)\)")

errors = []


def error(path, message):
    errors.append(f"{path.relative_to(ROOT)}: {message}")


def frontmatter(path):
    """Returns (fields, body) of a Markdown file with a flat `key: value` YAML frontmatter."""
    text = path.read_text(encoding="utf-8")
    match = re.match(r"^---\n(.*?)\n---\n?(.*)$", text, re.DOTALL)
    if not match:
        error(path, "missing YAML frontmatter")
        return {}, text
    fields = {}
    for line in match.group(1).splitlines():
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        key, sep, value = line.partition(":")
        if not sep or line[0].isspace():
            error(path, f"unsupported frontmatter line: {line!r}")
            continue
        value = value.strip()
        if value.startswith("[") and value.endswith("]"):
            value = [item.strip().strip("\"'") for item in value[1:-1].split(",") if item.strip()]
        fields[key.strip()] = value
    return fields, match.group(2)


def check_links(path, body):
    for target in LINK.findall(body):
        if re.match(r"^[a-z]+:", target) or target.startswith("#"):
            continue
        if not (path.parent / target.split("#")[0]).exists():
            error(path, f"broken relative link {target}")


def check_marketplace():
    """Returns {plugin name: plugin directory} for the plugins the marketplace lists."""
    marketplace_file = ROOT / ".claude-plugin/marketplace.json"
    marketplace = json.loads(marketplace_file.read_text(encoding="utf-8"))
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    plugins = {}
    for entry in marketplace.get("plugins", []):
        directory = (ROOT / entry["source"]).resolve()
        manifest_file = directory / ".claude-plugin/plugin.json"
        if not manifest_file.exists():
            error(marketplace_file, f"plugin {entry['name']}: no {manifest_file.relative_to(ROOT)}")
            continue
        manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
        if manifest.get("name") != entry["name"]:
            error(manifest_file, f"name {manifest.get('name')!r} differs from the marketplace entry {entry['name']!r}")
        version = manifest.get("version", "")
        if not re.fullmatch(r"\d+\.\d+\.\d+", version):
            error(manifest_file, "version is not MAJOR.MINOR.PATCH")
        elif f"`{entry['name']}` `{version}`" not in readme:
            error(ROOT / "README.md", f"does not state the current version `{entry['name']}` `{version}`")
        plugins[entry["name"]] = directory
    for name, directory in plugins.items():
        manifest = json.loads((directory / ".claude-plugin/plugin.json").read_text(encoding="utf-8"))
        for dependency in manifest.get("dependencies", []):
            if dependency not in plugins:
                error(directory / ".claude-plugin/plugin.json", f"depends on {dependency!r}, which the marketplace does not list")
    return plugins


def check_skills(plugin):
    names = []
    skills = plugin / "skills"
    for directory in sorted(p for p in skills.iterdir() if p.is_dir()) if skills.is_dir() else []:
        skill = directory / "SKILL.md"
        if not skill.exists():
            error(directory, "no SKILL.md")
            continue
        fields, body = frontmatter(skill)
        if fields.get("name") != directory.name:
            error(skill, f"name {fields.get('name')!r} differs from its directory {directory.name!r}")
        description = fields.get("description", "")
        if not description:
            error(skill, "empty description")
        elif len(description) > 1024:
            error(skill, f"description is {len(description)} characters long (max 1024)")
        check_links(skill, body)
        for reference in directory.rglob("*.md"):
            if reference != skill:
                check_links(reference, reference.read_text(encoding="utf-8"))
        names.append(directory.name)
    return names


def check_agents(plugin):
    names = []
    for agent in sorted((plugin / "agents").glob("*.md")):
        fields, body = frontmatter(agent)
        if fields.get("name") != agent.stem:
            error(agent, f"name {fields.get('name')!r} differs from its file name {agent.stem!r}")
        if not fields.get("description"):
            error(agent, "empty description")
        if not fields.get("tools"):
            error(agent, "no tools listed (the reviewers must stay read-only)")
        check_links(agent, body)
        names.append(agent.stem)
    return names


def check_readme(plugin_name, components):
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    for name in components:
        if not re.search(rf"^\| `{re.escape(plugin_name)}` \|[^\n]*\| `{re.escape(name)}` \|", readme, re.MULTILINE):
            error(ROOT / "README.md", f"the Contents table does not list `{name}` under `{plugin_name}`")


def check_evals(plugin, dependencies, components):
    """The eval runs from the repository root, so each case loads its plugin and the plugin's dependencies itself."""
    evals = plugin / "evals"
    covered = {}
    cases = sorted(p for p in evals.iterdir() if p.is_dir() and p.name != "results") if evals.is_dir() else []
    for case in cases:
        prompt = case / "prompt.md"
        if not prompt.exists():
            error(case, "no prompt.md")
            continue
        fields, body = frontmatter(prompt)
        for key in fields.keys() - PROMPT_KEYS:
            error(prompt, f"unknown frontmatter key {key!r}")
        if not body.strip():
            error(prompt, "empty prompt")
        loaded = {(case / path).resolve() for path in fields.get("plugins", [])}
        for required in [plugin, *dependencies]:
            if required not in loaded:
                error(prompt, f"`plugins` does not load {required.relative_to(ROOT)} (relative to the case directory)")
        for tag in fields.get("tags", []):
            covered.setdefault(tag, []).append(case.name)
        graders = sorted((case / "graders").glob("*.md"))
        if not graders:
            error(case, "no graders/*.md")
        if all(frontmatter(g)[0].get("type") == "tool_used" for g in graders):
            error(case, "needs at least one outcome grader besides tool_used")
        for grader in graders:
            gfields, gbody = frontmatter(grader)
            kind = gfields.get("type")
            if kind not in GRADER_TYPES:
                error(grader, f"unknown grader type {kind!r}")
            if kind in {"regex", "llm"} and not gbody.strip():
                error(grader, "empty pattern or rubric")
            if kind == "regex":
                try:
                    re.compile(gbody.strip())
                except re.error as e:
                    error(grader, f"invalid regex: {e}")
        case_yaml = case / "case.yaml"
        if case_yaml.exists():
            text = case_yaml.read_text(encoding="utf-8")
            if not re.search(r"^schema_version:", text, re.MULTILINE):
                error(case_yaml, "missing schema_version")
            script = re.search(r"scaffold_script:\s*(\S+)", text)
            if script and not (case / script.group(1)).is_file():
                error(case_yaml, f"scaffold_script {script.group(1)} does not exist")
    for name in components:
        if name not in covered:
            error(plugin, f"no eval case under evals/ tagged {name!r}")
    if components and "negative" not in covered:
        error(plugin, "no eval case tagged 'negative' (a prompt where none of its skills may fire)")
    return covered


def main():
    plugins = check_marketplace()
    for name, plugin in plugins.items():
        manifest = json.loads((plugin / ".claude-plugin/plugin.json").read_text(encoding="utf-8"))
        dependencies = [plugins[d] for d in manifest.get("dependencies", []) if d in plugins]
        components = check_skills(plugin) + check_agents(plugin)
        check_readme(name, components)
        covered = check_evals(plugin, dependencies, components)
        print(name)
        for component in components:
            print(f"  {component}: {', '.join(covered.get(component, [])) or '-'}")
    if errors:
        print(f"\n{len(errors)} problem(s):", file=sys.stderr)
        for message in errors:
            print(f"  {message}", file=sys.stderr)
        return 1
    print("\nOK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
