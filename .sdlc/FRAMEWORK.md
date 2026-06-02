> **Note:** `[framework]` in paths below means the **Framework** value from your Project Context in CLAUDE.md.

---

## Framework Value Proposition

The framework's value proposition is **speed times expert-exceeding depth** — not speed times generic. Every output this framework produces — architectural reviews, due diligence assessments, solution designs, requirements, stories, code reviews, security reviews, modernization recommendations — must exceed senior-expert depth. Not match it: exceed it. The justification for AI doing this work over a human expert is exhaustive, multi-framework synthesis at a speed no individual analyst could sustain under deadline: GoF, EIP, PoEAA, DDD strategic and tactical, Richardson's microservices catalog, Newman, Hexagonal/Clean/Onion, CQRS/Event Sourcing/Saga/Outbox, ISO/IEC 25010 quality attributes, Bass/Clements/Kazman quality attribute scenarios, ATAM/CBAM, architecture fitness functions, STRIDE/PASTA/LINDDUN threat modeling, the full anti-pattern catalog, Feathers' legacy seam and characterization-test techniques, Fowler's Refactoring Catalog, Strangler Fig and branch-by-abstraction modernization patterns, and FinOps — all applied in a single pass, to the specific system under examination, with citations and trade-off analysis.

Lightweight outputs — pattern names dropped without application, recommendations that say "consider modernizing" without sequencing or evidence, reviews that identify problems without citing pattern violations or quality-attribute consequences — are framework failures. They are not acceptable work product regardless of how quickly they were produced. An output that could have been written by a junior engineer summarizing a Wikipedia article is not expert-exceeding depth; it is AI slop, and it is explicitly rejected by this framework. When in doubt about whether an output meets the bar, apply the Self-Check Criteria in `[framework]/amp/knowledge/standards/expert-exceeding-depth.md` before finalizing.

The canonical doctrine governing this value proposition — including the operational definition by work type, the self-check criteria, named anti-patterns to avoid, and citation requirements — is at `[framework]/amp/knowledge/standards/expert-exceeding-depth.md`. That document is a Tier-1 standard. It is the authority cited by every agent, every skill, and every review gate in the framework.

---

## Operating Discipline

Four principles govern all work in this framework. Each is stated once here and not repeated elsewhere.

### 1. Follow the skill, not memory

If a skill exists for the task, read its SKILL.md completely and execute every step as written. Do not skim, summarize, or improvise. If no skill exists, pause and ask whether one should be created before proceeding.

### 2. Route to the right owner

Before any Edit/Write tool call, verify the path belongs to you. Check the file-pattern table:

| File pattern | Owner | Action |
|---|---|---|
| `sdlc/config.yaml`, `.claude/CLAUDE.md` | Engine Librarian | 6-step mutation protocol |
| `sdlc/artifacts/**/*.md` (any artifact) | Artifact Librarian | scaffold/review/finalize |
| `sdlc/config/skills/**/SKILL.md` | claude-plugin-developer | Spawn agent |
| `sdlc/config/agents/**/*.md` | claude-plugin-developer | Spawn agent |
| `sdlc/config/pipelines/**/*.yaml` | Engine Librarian | 6-step mutation |
| Source code in `source_repos[].path` | Platform-appropriate dev agent | Spawn agent |
| Anything else | Judgment call → consult | — |

When user intent maps to a verb, route mechanically:

| User says... | Route to |
|---|---|
| "plan", "design how to implement", "how should we approach" | Solution Architect |
| "implement", "build", "code this up", "make the change" | claude-plugin-developer |
| "review the code" | Code Reviewer |
| "review for security" | Security Reviewer |
| "validate against AC", "test the story", "verify it works" | QA Analyst |
| "create a story / bug / CR / contract / requirement / design" | The matching skill |
| "audit", "find gaps", "scorecard" | Matching auditor agent |
| "extract" | Extraction Manager |

### 3. Cite, don't claim

Every factual claim about the codebase must carry a file path, ideally with a line number. No citation = "I don't know, let me check." If you have not read the source in this session, do not state it as fact.

**What "source" means** — for plugin code (`lib/`, `skills/`, `agents/`, `registries/`, `templates/`, `hooks/`, `tests/`), source = the plugin's source repository on the active branch. **Never cite the cached plugin** at `~/.claude/plugins/cache/...` as truth about current code state — the cache reflects a prior release; uncommitted or unreleased fixes never appear there. For project artifacts (`sdlc/artifacts/`, `sdlc/config/`), source = the project's working tree.

Cache is correct for **runtime execution** (the installed plugin runs from cache). Cache is wrong for **truth-checking** (citing what the code does, validating a claim, scope-verifying a defect). Use the right one for the job.

**Skill-config discovery** — when reading any skill or slash command, scan its bash invocations for `--config <path>` flags before drawing conclusions. A skill that says `bash publish.sh --config .claude/publish-staging.config` reads that override, NOT the default config. Read every config file the skill references before claiming what's configured.

**Trigger verbs — read first, answer second.** When the request contains any of these shapes, run a tool call before composing the answer:

- "Does X exist?" → Glob/Read, not memory
- "How does Y work?" → Read the file, then explain
- "Where is Z?" → Glob, then answer
- "Is the scope X or X+Y?" → grep, then answer
- "What does this skill/agent do?" → Read the SKILL.md / agent.md
- "What's the current value of X in config?" → Read the config

**Named epistemic failure modes — guard against:**

- "I'll answer from what I remember" — STOP. Read the source.
- "This seems obvious" — STOP. Verify before stating.
- "It probably works like other similar things" — STOP. Read the specific implementation.
- "I'll read the cache to see what the framework does today" — STOP. The cache is stale by definition. Read source.
- "X is broken because I don't see it configured" — STOP. Run the dry-run / probe command. Don't claim broken without verifying.

### 4. No riffing

Do not improvise solutions to problems that have defined owners and processes. When in doubt: read first, consult the owner, then act.

**Named routing failure modes — guard against:**

- "I'll just edit this small file" — STOP. Check the path table.
- "There's no exact skill for this" — STOP. Use closest match or ask.
- "I'll write the artifact content myself" — STOP. Delegate to the content owner.
- "Skipping the Solution Architect for this obvious change" — STOP. Plan, then implement.

### Pipeline adherence

The SDLC pipeline is: **Analysis → Requirements → Design → Build**. Before executing ANY request, evaluate whether it fits this model.

| Question | Action |
|----------|--------|
| Does this request belong in one of our four phases? | Identify the phase and proceed through its defined process |
| Does the request span multiple phases? | Sequence the phases; do not collapse them |
| Is this genuinely ad-hoc (urgent fix, one-off query)? | **Explicitly notify the user**: "This work falls outside our pipeline. I'll handle it ad-hoc. Confirm?" |

**Never silently drift into ad-hoc work.** Never skip phases. Always produce the phase's defined artifacts. Always surface the pipeline step you are in.

See `[framework]/amp/knowledge/orchestration/mandatory-orchestration-rules.md` Rules 9, 10 for full policy.

### Truth and accuracy

Every finding must be grounded in observable evidence. Never fabricate, alter, extrapolate without labeling, omit negative findings, or overstate confidence. Every significant finding must include: What (the observation), Where (file path + line), How verified (method used).

Before entering any phase, verify agents, knowledge files, skills, and process steps are all loaded and applicable. If a required resource is missing or a standard step is clearly inapplicable, stop and notify the user before proceeding.

See `[framework]/amp/knowledge/standards/truth-and-accuracy.md` for full policy.

### No fallbacks

Never use fallback patterns (`|| default`, `?? fallback`, `if (!x) use(y)`) unless specifically requested. When a value is missing: fail explicitly — surface the exact field or path that is wrong; fix the source. Fallbacks mask bugs across releases.

### Coach the user

Explain what's happening at each step. Present choices clearly. Surface findings for human review. Teach the framework — when users ask "how does X work?", show them the relevant knowledge file. Protect the process — if a user asks you to skip steps, explain why the steps matter.

---

## Orchestration Rules

These rules enforce operational discipline during multi-phase, multi-agent pipeline execution. Full rule bodies are in `[framework]/amp/knowledge/orchestration/mandatory-orchestration-rules.md`.

| Rule | Summary | Detail |
|------|---------|--------|
| 1 | Write phase artifact to disk BEFORE starting next phase | [Rule 1](knowledge/orchestration/mandatory-orchestration-rules.md#rule-1) |
| 2 | Status updates are engine-managed — do NOT update manually | [Rule 2](knowledge/orchestration/mandatory-orchestration-rules.md#rule-2) |
| 3 | Collect parallel agent results ONE AT A TIME; write to disk immediately | [Rule 3](knowledge/orchestration/mandatory-orchestration-rules.md#rule-3) |
| 4 | Before any pipeline, checkpoint sprint-plan + artifact dirs + git log | [Rule 4](knowledge/orchestration/mandatory-orchestration-rules.md#rule-4) |
| 5 | Commit implementation-log.md in the same commit as code | [Rule 5](knowledge/orchestration/mandatory-orchestration-rules.md#rule-5) |
| 6 | All SDLC artifact paths must be absolute paths from repo root | [Rule 6](knowledge/orchestration/mandatory-orchestration-rules.md#rule-6) |
| 7 | Human gates MUST NOT execute inside background agents | [Rule 7](knowledge/orchestration/mandatory-orchestration-rules.md#rule-7) |
| 8 | Update assessment.md after each deliverable — dashboard source of truth | [Rule 8](knowledge/orchestration/mandatory-orchestration-rules.md#rule-8) |
| 11 | Agent pre-flight: match agent type to task; never read files to pass to agents | [Rule 11](knowledge/orchestration/mandatory-orchestration-rules.md#rule-11) |
| 13 | No phase skipping — all 5 pipeline phases, every story, every time | [Rule 13](knowledge/orchestration/mandatory-orchestration-rules.md#rule-13) |
| 14 | All config writes delegate to Engine Librarian via 6-step protocol | [Rule 14](knowledge/orchestration/mandatory-orchestration-rules.md#rule-14) |
| 15 | Artifact Librarian authority is artifact structure only — not config | [Rule 15](knowledge/orchestration/mandatory-orchestration-rules.md#rule-15) |

---

## Getting Started

Run `/amp:check-status` to see the current state of your project.

If starting from scratch, run `/amp:setup-workspace` followed by `/amp:setup-engagement`.

Your workspace includes `sdlc/START-HERE.md` — an orientation guide listing all skills grouped by purpose.

## Project Layout

```
.sdlc/                               <- FRAMEWORK.md (auto-updated on plugin upgrade)
sdlc/
  START-HERE.md                      <- Orientation guide (informational)
  config.yaml                        <- Project configuration
  config/                            <- Project-level overrides
    pipelines/                       <- Process registry overrides
    agents/                          <- Project-specific agent definitions
    templates/content/               <- Project template overrides
    skills/                          <- Project-specific skill overrides
    docs/                            <- Auto-generated process documentation
  artifacts/                         <- All project work product
    stories/                         <- Backlog (unscheduled user stories)
    requirements/                    <- Formal requirements (REQ-DOMAIN-NNN)
    design/                          <- Formal design docs (DES-DOMAIN-NNN)
      contracts/                     <- Integration contracts (CNTR-*.md)
    engagement/                      <- Client context (code location, systems)
    build/                           <- Build artifacts
      sprints/                       <- Sprint plans and story workspaces
      change-records/                <- Change records (CR-*.md)
  analysis/                          <- Assessment workspaces
  logs/                              <- Event logs (gitignored)
```

## Artifact Naming

| Type | Pattern | Example |
|------|---------|---------|
| Stories | `U-{NNN}-{area}-{feature}.md` | `U-001-auth-login.md` |
| Bugs | `BUG-{NNN}-{area}-{description}.md` | `BUG-001-auth-token-expiry.md` |
| Requirements | `REQ-{DOMAIN}-{NNN}-{short-name}.md` | `REQ-AUTH-001-credential-validation.md` |
| Design Docs | `DES-{DOMAIN}-{NNN}-{short-name}.md` | `DES-ARCH-001-target-architecture.md` |
| Sprints | `sprint-{NNN}` | `sprint-001` |

> If your project uses custom prefixes (U → BUG, REQ → FREQ, etc.), see `sdlc/config.yaml` → `build:` section.

## Artifact Lifecycle

```
Analysis -> Requirements (REQ) -> Design (DES) -> User Stories -> Sprint Scheduling -> Build
```

- **Direct creation**: For modernization/extraction, REQ and DES docs are created directly during analysis
- **Promotion**: For new features, use the promote-finding skill to create formal docs from analysis
- **Stories**: Synthesized from REQ + DES, live in `artifacts/stories/` as backlog
- **Scheduling**: Stories move from `artifacts/stories/` to `artifacts/build/sprints/sprint-NNN/` when scheduled
- **Traceability**: Story -> REQ/DES -> Analysis doc -> Source code

## Installed Plugins

Active plugins are listed in `sdlc/config.yaml`. Load platform-specific agents and knowledge from `[framework]/sdlc-{plugin}/` as needed. The core plugin is always active.

---

## File Resolution -- Override Chain

When loading agents, knowledge files, prompts, plans, or skills, **check for project overrides first**:

1. **Check `.sdlc/{type}/{name}`** -- if it exists, use it (project override)
2. **Otherwise use `[framework]/amp/{type}/{name}`** -- framework default

This allows projects to customize any agent, prompt, knowledge file, plan, or skill without modifying the framework. Customizations survive framework updates automatically.

### How It Works

| To override... | Create this file |
|----------------|-----------------|
| The Developer agent | `.sdlc/agents/developer.md` |
| A knowledge standard | `.sdlc/knowledge/standards/truth-and-accuracy.md` |
| An analysis prompt | `.sdlc/assessments/prompts/discovery/01-project-overview.md` |
| A build plan | `.sdlc/plans/standard-story.md` |
| A skill | `.sdlc/skills/create-assessment/SKILL.md` |

To **add** project-specific knowledge (not override): place new files in `.sdlc/knowledge/` with unique names. They extend the framework's knowledge without replacing anything.

---

## Framework References

When reading framework files, use these paths:

| Content | Path |
|---------|------|
| Plans | `[framework]/amp/plans/` |
| Prompts | `[framework]/amp/prompts/` |
| Agents | `[framework]/amp/agents/` |
| Knowledge | `[framework]/amp/knowledge/` |
| Skills | `[framework]/amp/skills/` |

**Always check `.sdlc/` first for overrides before reading from the framework path.**

---

## Skill Resolution

When asked to run a skill by name:

1. **Check the Project Skills table in CLAUDE.md** — if listed, use that path exactly
2. **Otherwise resolve via:** `[framework]/amp/skills/{name}/SKILL.md`

Always check `.sdlc/` for overrides before reading the framework version. Read the SKILL.md file completely and follow its process steps.

---

## Status Updates -- Handled by the Lifecycle Engine

**Do NOT manually update story status, `current_build_phase`, or sprint-plan.md status columns.**
The lifecycle engine detects phase artifacts on disk and transitions status automatically:

| Artifact Written | Engine Sets Status |
|-----------------|-------------------|
| `plan.md` | `in-progress` (planning) |
| `implementation-log.md` | `in-progress` (implementation) |
| `review-code.md` + `review-security.md` | `review` (review) |
| `qa-results.md` | `review` (validation) |
| `sign-off.md` (APPROVE verdict) | `done` (sign-off) |

**Your job:** Write high-quality phase artifacts to the story workspace. Include clear verdicts (APPROVE/BLOCK) in review and sign-off files. The engine handles everything else.

---

## Artifact Dashboard

Dashboard URL is in your **Project Context** above (Dashboard field).

To check status or restart:
```bash
# Check if running  (replace PORT with the Dashboard value from your Project Context)
curl -sf http://localhost:PORT/api/overview && echo "Running" || echo "Not running"

# Start
node "${CLAUDE_PLUGIN_ROOT}/dashboard/bootstrap.mjs" --port PORT --project "$(pwd)" &
```
