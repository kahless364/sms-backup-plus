# SDLC Framework — Start Here

Welcome to your AMP-powered SDLC workspace. This file is an orientation guide.

---

## Bootstrap

If you have not yet configured this workspace, run these two skills in order:

```
/amp:setup-workspace      — scaffold the workspace, config.yaml, and CLAUDE.md
/amp:setup-engagement     — populate engagement context (code location, systems, endpoints)
```

Once both are complete, use `/amp:check-status` to see the current state of your project.

---

## Platform Capabilities

Skills are invoked as `/amp:{name}` in Claude Code. The full catalog is in
`knowledge/references/skill-catalog.md`.

### Setup & Configuration

| Skill | Description |
|-------|-------------|
| `/amp:setup-workspace` | Scaffold a v3 plugin-native workspace |
| `/amp:setup-engagement` | Populate engagement context files |
| `/amp:configure-pipeline` | Configure analysis, design, and delivery workflows |
| `/amp:upgrade-framework` | Non-destructive upgrade to the latest framework version |
| `/amp:create-agent` | Create a custom agent for engagement-specific needs |

### Navigation & Status

| Skill | Description |
|-------|-------------|
| `/amp:check-status` | Show current engagement status: assessments, backlog, and build progress |

### Analysis

| Skill | Description |
|-------|-------------|
| `/amp:create-assessment` | Create a new assessment workspace |
| `/amp:continue-assessment` | Resume an in-progress assessment |
| `/amp:promote-finding` | Promote an analysis finding to a formal REQ or DES artifact |
| `/amp:impact-analysis` | Analyze impact of new or changed requirements |

### Requirements & Design

| Skill | Description |
|-------|-------------|
| `/amp:create-requirements` | Author EPIC-* and REQ-* artifacts from stakeholder input |
| `/amp:create-design` | Create DES-* design documents from approved requirements |
| `/amp:create-contracts` | Formalize integration contracts as CNTR-* artifacts |
| `/amp:create-cr` | Create a Change Record for a defect or scope change |
| `/amp:create-bug` | Create a bug artifact |

### Build & Execution

| Skill | Description |
|-------|-------------|
| `/amp:create-stories` | Create user story artifacts from approved source artifacts |
| `/amp:create-sprint` | Schedule stories into a sprint plan |
| `/amp:execute-sprint` | Execute sprint stories with dependency-aware parallel execution |
| `/amp:execute-story` | Run the full build pipeline for a single story |
| `/amp:review-story` | Run code and security review for a completed story |
| `/amp:preflight` | Validate prerequisites before starting a plan or phase |

### Gates & Governance

| Skill | Description |
|-------|-------------|
| `/amp:gate-approve` | Approve a human gate hold on an artifact |
| `/amp:gate-reject` | Reject a human gate hold on an artifact |

### Utilities

| Skill | Description |
|-------|-------------|
| `/amp:check-status` | Show engagement, assessment, and build progress |
| `/amp:consult` | Summon any agent for ad-hoc advisory consultation |
| `/amp:ui-validation` | Browser-based UI validation for web-touching stories |
| `/amp:dashboard` | Start the artifact dashboard |

---

## Typical Workflows

**New engagement (greenfield):**
`/amp:setup-workspace` → `/amp:setup-engagement` → `/amp:create-requirements` → `/amp:create-design` → `/amp:create-contracts` → `/amp:create-stories` → `/amp:create-sprint` → `/amp:execute-sprint`

**New engagement (modernization/enhancement):**
`/amp:setup-workspace` → `/amp:setup-engagement` → `/amp:create-assessment` → `/amp:run-plan` → `/amp:promote-finding` → `/amp:create-stories` → `/amp:create-sprint` → `/amp:execute-sprint`

**Resume work:**
`/amp:check-status` → `/amp:continue-assessment` or `/amp:execute-sprint`

---

## Further Reading

- `knowledge/references/skill-catalog.md` — Full skill reference with arguments and outputs
- `knowledge/references/agent-catalog.md` — Agent roster with roles and topic signals
- `.sdlc/FRAMEWORK.md` — Framework directives and orchestration rules
