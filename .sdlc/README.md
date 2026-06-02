# Project Overrides (sdlc/config/)

This directory contains project-specific customizations of the SDLC Framework.
Files placed here take precedence over their counterparts in the framework plugin.

## How It Works

The framework resolves files using a two-layer lookup:

1. **Project layer** -- `sdlc/config/{category}/{path}` (this directory)
2. **Framework layer** -- `${CLAUDE_PLUGIN_ROOT}/{category}/{path}`

If a file exists in the project layer, it is used **instead of** the framework version.
If no override exists, the framework version is used automatically.

### Override (Same Name = Replacement)

When a file in `sdlc/config/` has the same relative path as a framework file, the `sdlc/config/` version **completely replaces** the framework version for this project.

| Override file | Replaces |
|---------------|----------|
| `sdlc/config/agents/developer.md` | `${CLAUDE_PLUGIN_ROOT}/agents/developer.md` |
| `sdlc/config/agents/code-reviewer.md` | `${CLAUDE_PLUGIN_ROOT}/agents/code-reviewer.md` |
| `sdlc/config/assessments/prompts/discovery/01-project-overview.md` | `${CLAUDE_PLUGIN_ROOT}/assessments/prompts/discovery/01-project-overview.md` |
| `sdlc/config/plans/standard-story.md` | `${CLAUDE_PLUGIN_ROOT}/plans/standard-story.md` |
| `sdlc/config/skills/create-assessment/SKILL.md` | `${CLAUDE_PLUGIN_ROOT}/skills/create-assessment/SKILL.md` |

### Extension (Unique Name = Addition)

Files in `sdlc/config/` with names that do NOT match any framework file are **extensions**. They add to the framework's content without replacing anything. Both the framework content and the extension are available.

| Extension file | What it does |
|----------------|--------------|
| `sdlc/config/knowledge/domain/hipaa-requirements.md` | Adds domain-specific knowledge |
| `sdlc/config/knowledge/standards/our-coding-standards.md` | Adds project-specific coding standards |
| `sdlc/config/prompts/custom/01-regulatory-scan.md` | Adds a custom analysis prompt |
| `sdlc/config/agents/compliance-reviewer.md` | Adds a new agent not in the framework |

> **Note:** `knowledge/domain/` and `knowledge/specifications/` are **empty in the framework distribution**. Consumers populate them via `sdlc/config/knowledge/domain/*.md` and `sdlc/config/knowledge/specifications/*.md` overrides as shown above. See `knowledge/domain/README.md` and `knowledge/specifications/README.md` for the namespace intent.

## Directory Structure

```
sdlc/config/
  agents/          # Override or add agent definitions
  knowledge/       # Add project knowledge (extends framework knowledge)
  prompts/         # Override or add analysis/build prompts
  plans/           # Override execution plans
  skills/          # Override or add skills
  pipelines/       # Override or add pipeline definitions
```

## Override Types in Detail

### Agents

Agents define the roles Claude assumes during different phases of the SDLC. To customize an agent's behavior, responsibilities, or model assignment, ask the Engine Librarian agent to create an override:

> "Engine Librarian, create an override for the developer agent. Change the model to opus and add our project coding standards to its responsibilities."

The Engine Librarian will create `sdlc/config/agents/developer.md` with the appropriate customizations. Claude will use your version instead of the framework version.

**Example use cases:**
- Change the Developer agent from `sonnet` to `opus` for a project requiring more sophisticated reasoning
- Add project-specific coding standards to the Developer's responsibilities
- Modify the Solution Architect to enforce domain-specific architectural patterns

### Knowledge

Knowledge files provide reference material that agents consult during their work. Knowledge in `sdlc/config/knowledge/` **extends** the framework's knowledge -- Claude reads both.

To add project-specific knowledge, ask the Engine Librarian:

> "Engine Librarian, add HIPAA compliance requirements to the project knowledge base."

The Engine Librarian will create `sdlc/config/knowledge/domain/hipaa.md` with the content. Agents will find and reference this knowledge alongside the framework's built-in knowledge.

To override a specific framework knowledge file, ask the Engine Librarian to create an override using the same relative path as the framework file.

**Example use cases:**
- Add domain glossaries (healthcare terms, financial instruments)
- Add project-specific API documentation
- Add company coding standards or architecture decision records
- Override the default truth-and-accuracy standard with stricter project requirements

### Prompts

Prompts are the numbered analysis questions that drive the framework's discovery and assessment phases. Override a specific prompt to change what gets analyzed.

Ask the Engine Librarian to create a prompt override:

> "Engine Librarian, customize the project-overview discovery prompt to include regulatory compliance questions."

The Engine Librarian will create the override at `sdlc/config/assessments/prompts/discovery/01-project-overview.md`.

**Example use cases:**
- Add regulatory compliance questions to the security prompt
- Customize the architecture prompt for your specific tech stack
- Add a custom prompt for domain-specific analysis

### Plans

Plans define the execution sequence -- which agents run, in what order, with what quality gates. Override a plan to change the workflow.

Ask the Engine Librarian:

> "Engine Librarian, override the standard-story plan to add a compliance review phase after QA."

The Engine Librarian will create `sdlc/config/plans/standard-story.md`.

**Example use cases:**
- Add an additional review phase (e.g., compliance review)
- Remove phases not relevant to your project
- Change agent assignments for specific phases

### Skills

Skills are reusable automation steps that agents invoke. Override a skill to change its behavior.

Ask the Engine Librarian:

> "Engine Librarian, override the execute-story skill to include our custom deployment checklist."

The Engine Librarian will create `sdlc/config/skills/execute-story/SKILL.md`. Skills continue to be invoked via `/amp:{skill-name}` -- the plugin system routes to your override automatically.

**Example use cases:**
- Modify the story execution workflow to include additional steps
- Change the assessment creation process for your project's needs
- Add a custom skill for project-specific automation

## Platform Plugin Overrides

Platform plugins (e.g., `sdlc-react-native`, `sdlc-dotnet`) follow the same override pattern. Use the same filename to override.

| Override | Framework default |
|----------|------------------|
| `sdlc/config/agents/react-native-specialist.md` | `${CLAUDE_PLUGIN_ROOT}/agents/react-native-specialist.md` |
| `sdlc/config/knowledge/languages/react-native/best-practices.md` | `${CLAUDE_PLUGIN_ROOT}/knowledge/languages/react-native/best-practices.md` |

## Staleness Warnings

When the framework is updated and a file you have overridden has changed upstream, the update tool warns you:

```
WARNING: developer.md was updated in framework but you have a local
  override at sdlc/config/agents/developer.md -- review for changes you may
  want to incorporate.
```

This is advisory only. Your override continues to work. Review the framework changes and decide whether to incorporate them into your override.

## Fallback Behavior

- If you **delete** an override file, Claude falls back to the framework version automatically on the next read. No configuration change is needed.
- If the framework **adds** a new file, it is immediately available to all projects. No override is needed unless you want to customize it.
- If an override is **identical** to the framework version, it works but is redundant. Consider removing it.

## Contributing Back

If your override or extension would be useful to other projects:

1. Open a PR on the framework plugin repository with the file contents
2. Once merged, other projects get it automatically via plugin updates
3. Delete your `sdlc/config/` override since the framework now includes it
