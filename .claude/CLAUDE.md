# SMS Backup+

> **This file governs ALL AI behavior in this project. Read it completely before doing anything.**

## Project Context

| Field | Value |
|-------|-------|
| Project | SMS Backup+ |
| Client | Open Source |
| Purpose |  |
| Dashboard | http://localhost:3818 |

### Source Repositories

(To be filled in — see sdlc/artifacts/engagement/code-location.md)

### Where to Find What

Before working on application code, read the relevant engagement and design artifacts. Do not guess at tech stack, architecture, or conventions -- they are documented.

| What you need | Read this file |
|---------------|----------------|
| Tech stack, source layout, how to build/test/run | `sdlc/artifacts/engagement/code-location.md` |
| System inventory, architecture layers, dev commands | `sdlc/artifacts/engagement/systems.md` |
| Database, API, and service endpoints | `sdlc/artifacts/engagement/endpoints.md` |
| Project configuration | `sdlc/config.yaml` |

---

@.sdlc/FRAMEWORK.md

---

## Project Skills

Framework skills resolve automatically via `/amp:{name}`.
List only project-level overrides and additions below.

(No project-level overrides yet. See sdlc/config/skills/ to add overrides.)

---

## Dev-Mode Awareness

If `sdlc/.dev-mode-active.md` exists at the start of a session or any agent invocation, **dev-mode is active** for this project. Dev-mode is a lightweight execution path for exploratory and POC work — see `/amp:dev-mode` for the full skill.

While dev-mode is active:

- **Do NOT invoke** Sprint Manager, Story Build Manager, Solution Architect (planning), Security Reviewer, or QA Analyst.
- **Do NOT scaffold formal artifacts** during the build — no story files, sprint plans, plan.md, or contract artifacts. The marker file's task log is the running record.
- **Use `dev-mode-developer`** for implementation work, not the formal Developer agent.
- **Code review** runs at each phase milestone during the build (hello-world / structure / content / polish) plus a final consolidated pass at `/amp:dev-mode end`. Mandatory, not opt-in. Catches issues early so they don't compound; not run per-increment to keep token cost contained.
- **Heavyweight skills require confirmation.** If the user invokes `/amp:execute-story`, `/amp:create-sprint`, `/amp:execute-sprint`, `/amp:create-stories`, or similar pipeline-driving skills while dev-mode is active, warn the user that dev-mode is on and ask whether to proceed (which implicitly suspends dev-mode for that invocation) or cancel.
- **Solution Architect, Security Reviewer, QA Analyst** can still be reached ad-hoc via `/amp:consult` if the user explicitly wants them — that is not blocked.
- **Exit dev-mode** with `/amp:dev-mode end`. The skill runs the final consolidated Code Reviewer pass and then offers Graduate (retroactive formal artifacts) or Quick-exit (leave code, no artifacts).
