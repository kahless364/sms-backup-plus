---
title: "Modernization Planning"
plan_type: assessment
version: "1.0.0"
description: "Modernization and refactoring initiative planning from assessment through roadmap"
tags: ["modernization"]
---

# Modernization Planning Plan

## Overview
- **Purpose**: Plan and guide modernization/refactoring initiatives from assessment through roadmap creation
- **Duration**: 2-4 weeks depending on system complexity
- **Audience**: Technical Leadership, Architects, Development Teams

## Prerequisites
- [ ] Assessment created with `assessment.md` populated
- [ ] Source code access confirmed (via `artifacts/engagement/code-location.md`)
- [ ] Business drivers for modernization understood
- [ ] Stakeholder buy-in for modernization initiative

## Quality Standards

Apply the Expert-Exceeding Depth doctrine to your output. Verify against the 8 Self-Check Criteria before finalizing. See `knowledge/standards/expert-exceeding-depth.md`. Every prompt this plan invokes is held to this bar.

## Phase 1: Current State Assessment

This phase establishes a comprehensive understanding of the current system. It largely mirrors the Architecture Review plan.

### Step 1.1: Project Overview
**Prompt**: `assessments/prompts/discovery/01-project-overview.md`
**Output**: `outputs/{repo-name}/discovery/project-overview.md`
**Duration**: 30-60 minutes

**Instructions**:
1. Read the prompt file completely
2. Execute the analysis with modernization context in mind
3. Document the current technology stack and architecture
4. Identify areas that may need modernization

**Completion Criteria**:
- [ ] Technology stack documented
- [ ] Architecture described
- [ ] Age and evolution history captured
- [ ] Initial modernization concerns noted

**Update Progress**:
```
Mark Step 1.1 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 1.2: File Inventory
**Prompt**: `assessments/prompts/discovery/02-file-inventory.md`
**Output**: `outputs/{repo-name}/discovery/file-inventory.md`
**Duration**: 30-60 minutes

**Instructions**:
1. Read the prompt file completely
2. Analyze all source files
3. Note complexity patterns and potential modernization targets
4. Generate file inventory

**Completion Criteria**:
- [ ] Files categorized
- [ ] Statistics computed
- [ ] Complex areas identified
- [ ] Modernization targets noted

**Update Progress**:
```
Mark Step 1.2 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 1.3: Dependency Analysis
**Prompt**: `assessments/prompts/discovery/03-dependency-analysis.md`
**Output**: `outputs/{repo-name}/discovery/dependencies.md`
**Duration**: 30-60 minutes

**Instructions**:
1. Read the prompt file completely
2. Identify all external dependencies
3. Note outdated or end-of-life components
4. Document dependency upgrade requirements

**Completion Criteria**:
- [ ] Dependencies listed
- [ ] Outdated items identified
- [ ] Upgrade paths documented
- [ ] Risk areas noted

**Update Progress**:
```
Mark Step 1.3 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 1.4: Quality Attributes Assessment
**Prompt**: `assessments/prompts/architecture/01-ilities-assessment.md`
**Output**: `outputs/{repo-name}/architecture/ilities-assessment.md`
**Duration**: 2-4 hours

**Instructions**:
1. Read the prompt file and ilities reference
2. Assess current quality attributes
3. Document where the system falls short of requirements
4. Identify improvement opportunities

**Completion Criteria**:
- [ ] All relevant ilities assessed
- [ ] Current state documented
- [ ] Gaps identified
- [ ] Improvement opportunities noted

**Update Progress**:
```
Mark Step 1.4 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 1.5: Patterns Analysis
**Prompt**: `assessments/prompts/architecture/02-patterns-analysis.md`
**Output**: `outputs/{repo-name}/architecture/patterns.md`
**Duration**: 1-2 hours

**Instructions**:
1. Read the prompt file completely
2. Identify current architectural and design patterns
3. Note anti-patterns and inconsistencies
4. Document pattern improvements needed

**Completion Criteria**:
- [ ] Patterns identified
- [ ] Anti-patterns documented
- [ ] Consistency assessed
- [ ] Target patterns considered

**Update Progress**:
```
Mark Step 1.5 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 1.6: Technical Debt Identification
**Prompt**: `assessments/prompts/tech-debt/01-debt-identification.md`
**Output**: `outputs/{repo-name}/tech-debt/debt-inventory.md`
**Duration**: 2-3 hours

**Instructions**:
1. Read the prompt file completely
2. Identify technical debt across all categories
3. Quantify debt items
4. Note items that must be addressed during modernization

**Completion Criteria**:
- [ ] Debt items identified
- [ ] Items categorized and quantified
- [ ] Modernization-blocking debt noted
- [ ] Remediation priorities established

**Update Progress**:
```
Mark Step 1.6 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

## Phase 2: Future State Definition

### Step 2.1: Modernization Assessment
**Prompt**: `assessments/prompts/modernization/01-modernization-assessment.md`
**Output**: `outputs/{repo-name}/modernization/assessment.md`
**Output Format:** findings
**Duration**: 2-3 hours

**Instructions**:
1. Read the prompt file completely
2. Evaluate modernization drivers:
   - Business drivers
   - Technical drivers
   - Constraints
3. Assess modernization readiness
4. Identify high-level approach options

**Completion Criteria**:
- [ ] Drivers documented
- [ ] Constraints identified
- [ ] Readiness assessed
- [ ] Approach options outlined

**Update Progress**:
```
Mark Step 2.1 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 2.2: Target Architecture Definition
**Prompt**: `assessments/prompts/modernization/02-target-architecture.md`
**Output**: `outputs/{repo-name}/modernization/target-state.md`
**Duration**: 3-5 hours

**Instructions**:
1. Read the prompt file completely
2. Define the target architecture:
   - System structure
   - Technology stack
   - Quality attribute targets
   - Integration architecture
3. Document architectural decisions
4. Create architecture diagrams (ASCII/descriptions)

**Completion Criteria**:
- [ ] Target architecture defined
- [ ] Technology choices documented
- [ ] Quality targets established
- [ ] Architecture decisions recorded

**Update Progress**:
```
Mark Step 2.2 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 2.3: Gap Analysis
**Prompt**: `assessments/prompts/modernization/03-gap-analysis.md`
**Output**: `outputs/{repo-name}/modernization/gaps.md`
**Output Format:** findings
**Duration**: 2-4 hours

**Instructions**:
1. Read the prompt file completely
2. Compare current state to target state
3. Identify and categorize all gaps:
   - Structural gaps
   - Technology gaps
   - Capability gaps
   - Process gaps
4. Prioritize gaps

**Completion Criteria**:
- [ ] All gaps identified
- [ ] Gaps categorized
- [ ] Dependencies mapped
- [ ] Gaps prioritized

**Update Progress**:
```
Mark Step 2.3 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

## Phase 2.5: Requirements Extraction (For Full Rebuild Scenarios)

> **When to include this phase:** Include when the modernization involves rebuilding significant
> portions of the system rather than refactoring in place. Particularly valuable when:
> - Moving to a new technology stack
> - Rewriting UI components
> - Needing detailed specifications to guide new implementation
>
> Skip this phase if the modernization is primarily refactoring existing code.

This phase extracts detailed functional requirements from the current codebase to guide the new implementation.

### Step 2.5.1: Code Classification
**Prompt**: `assessments/prompts/migration/requirements/01-code-classification.md`
**Output**: `outputs/{repo-name}/modernization/requirements/code-classification.md`
**Duration**: 1-2 hours

**Instructions**:
1. Read the prompt file completely
2. Classify source files by their migration/modernization approach
3. Identify which code needs requirements extraction vs direct refactoring
4. Focus on components identified for rewrite in prior analysis

**Completion Criteria**:
- [ ] Rewrite-target files classified
- [ ] Extraction approach assigned per component
- [ ] Scope for detailed requirements defined

**Update Progress**:
```
Mark Step 2.5.1 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 2.5.2: UI Requirements Extraction
**Prompt**: `assessments/prompts/migration/requirements/02-ui-requirements-extraction.md`
**Output**: `outputs/{repo-name}/modernization/requirements/ui-requirements/`
**Duration**: 4-8 hours per major screen

> **Note**: This step can run in parallel with Steps 2.5.3 and 2.5.4

**Instructions**:
1. Focus on UI components targeted for rewrite
2. Extract exhaustive field-level specifications
3. Document all state controllers and event handlers
4. Attach source code for complex logic

**Completion Criteria**:
- [ ] Target UI screens have specifications
- [ ] Field-level requirements captured
- [ ] Event handlers documented
- [ ] Source code attached for complexity

**Update Progress**:
```
Mark Step 2.5.2 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 2.5.3: Non-Visual Interface Specification
**Prompt**: `assessments/prompts/migration/requirements/03-non-visual-interface-spec.md`
**Output**: `outputs/{repo-name}/modernization/requirements/non-visual-specs/`
**Duration**: 2-4 hours

> **Note**: This step can run in parallel with Steps 2.5.2 and 2.5.4

**Instructions**:
1. Document interfaces for business logic being rewritten
2. Capture behavioral notes and edge cases
3. Attach complete source as specification

**Completion Criteria**:
- [ ] Business logic interfaces documented
- [ ] Source code attached
- [ ] Dependencies mapped

**Update Progress**:
```
Mark Step 2.5.3 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 2.5.4: Dead Code Identification
**Prompt**: `assessments/prompts/migration/requirements/04-dead-code-identification.md`
**Output**: `outputs/{repo-name}/modernization/requirements/dead-code-candidates.md`
**Duration**: 1-2 hours

> **Note**: This step can run in parallel with Steps 2.5.2 and 2.5.3

**Instructions**:
1. Identify unused code in modernization scope
2. Recommend exclusion from modernization effort
3. Estimate scope reduction

**Completion Criteria**:
- [ ] Dead code candidates identified
- [ ] Scope reduction quantified

**Update Progress**:
```
Mark Step 2.5.4 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 2.5.5: Tribal Knowledge Gap Analysis
**Prompt**: `assessments/prompts/migration/requirements/05-tribal-knowledge-gaps.md`
**Output**: `outputs/{repo-name}/modernization/requirements/tribal-knowledge-gaps.md`
**Duration**: 1-2 hours

**Instructions**:
1. Identify undocumented complexity in rewrite scope
2. Generate SME interview questions
3. Prioritize gaps by risk

**Completion Criteria**:
- [ ] Knowledge gaps identified
- [ ] SME questions generated
- [ ] Risk assessment complete

**Update Progress**:
```
Mark Step 2.5.5 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 2.5.6: Migration Unit Definition
**Prompt**: `assessments/prompts/migration/requirements/06-migration-unit-definition.md`
**Output**: `outputs/{repo-name}/modernization/requirements/migration-units.md`
**Duration**: 2-3 hours

**Instructions**:
1. Group components into modernization units
2. Define unit boundaries for incremental delivery
3. Map dependencies between units

**Completion Criteria**:
- [ ] Modernization units defined
- [ ] Dependencies mapped
- [ ] Delivery increments clear

**Update Progress**:
```
Mark Step 2.5.6 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 2.5.7: Requirements Prioritization
**Prompt**: `assessments/prompts/migration/requirements/07-requirements-prioritization.md`
**Output**: `outputs/{repo-name}/modernization/requirements/prioritized-backlog.md`
**Duration**: 1-2 hours

**Instructions**:
1. Score modernization units by value and risk
2. Create dependency-aware sequence
3. Establish milestones

**Completion Criteria**:
- [ ] Prioritized backlog created
- [ ] Sequence respects dependencies
- [ ] Milestones defined

**Update Progress**:
```
Mark Step 2.5.7 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

## Phase 3: Strategy Development

### Step 3.1: Strangler Candidates Analysis
**Prompt**: `assessments/prompts/modernization/04-strangler-candidates.md`
**Output**: `outputs/{repo-name}/modernization/candidates.md`
**Output Format:** findings
**Duration**: 2-3 hours

**Instructions**:
1. Read the prompt file completely
2. Identify components suitable for strangler pattern:
   - Clear boundaries
   - Well-defined interfaces
   - Data isolation potential
3. Evaluate suitability of each candidate
4. Recommend strangling sequence

**Completion Criteria**:
- [ ] Candidates identified
- [ ] Suitability assessed
- [ ] Sequence recommended
- [ ] Migration strategies outlined

**Update Progress**:
```
Mark Step 3.1 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 3.2: Rewrite vs Refactor Analysis
**Prompt**: `assessments/prompts/modernization/05-rewrite-vs-refactor.md`
**Output**: `outputs/{repo-name}/modernization/approach.md`
**Duration**: 2-3 hours

**Instructions**:
1. Read the prompt file completely
2. For each major component, analyze:
   - Refactor approach (effort, risk, outcome)
   - Rewrite approach (effort, risk, outcome)
   - Replace approach (if applicable)
3. Make recommendations with rationale
4. Consider hybrid approaches

**Completion Criteria**:
- [ ] All options analyzed
- [ ] Each component assessed
- [ ] Recommendations documented
- [ ] Rationale provided

**Update Progress**:
```
Mark Step 3.2 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

## Phase 4: Roadmap Creation

### Step 4.1: Modernization Roadmap
**Prompt**: `assessments/prompts/modernization/06-modernization-roadmap.md`
**Output**: `outputs/{repo-name}/modernization/roadmap.md`
**Duration**: 3-4 hours

**Instructions**:
1. Read the prompt file completely
2. Synthesize all prior analysis
3. Create phased roadmap:
   - Phase 1: Foundation
   - Phase 2: Quick Wins
   - Phase 3: Core Transformation
   - Phase 4: Optimization
4. Define milestones and success criteria
5. Identify resource requirements
6. Document governance approach

**Completion Criteria**:
- [ ] Phases defined
- [ ] Work items detailed
- [ ] Dependencies mapped
- [ ] Timeline established
- [ ] Resources estimated
- [ ] Governance defined

**Update Progress**:
```
Mark Step 4.1 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

### Step 4.2: Final Report Generation
**Prompt**: N/A - Use output templates
**Output**: `outputs/{repo-name}/modernization/modernization-report.md`
**Duration**: 2-3 hours

**Instructions**:
1. Read all generated outputs from previous steps
2. Compile comprehensive modernization plan
3. Include executive summary
4. Present clear recommendations

**Report Structure**:
```markdown
# Modernization Plan

## Executive Summary

### Vision
[Future state description]

### Key Findings
- Current state: [Summary]
- Target state: [Summary]
- Gap magnitude: [Assessment]

### Recommended Approach
[High-level approach and rationale]

### Investment Summary
| Category | Estimate |
|----------|----------|
| Effort | [person-months] |
| Infrastructure | [$] |
| Training | [$] |
| Total | [$] |

### Timeline
[High-level timeline]

## Current State Summary

### Architecture
[Key findings from Phase 1]

### Technical Debt
[Key debt items]

### Quality Assessment
[Key quality gaps]

## Target State

### Target Architecture
[Summary with diagrams]

### Technology Decisions
| Decision | Choice | Rationale |
|----------|--------|-----------|

### Quality Targets
| Attribute | Current | Target |
|-----------|---------|--------|

## Gap Analysis Summary

### Critical Gaps
| Gap | Category | Effort |
|-----|----------|--------|

### Dependencies
[Key dependency chains]

## Strategy

### Approach
[Strangle/Refactor/Rewrite decisions]

### Strangler Candidates
| Component | Suitability | Sequence |
|-----------|-------------|----------|

### Risk Assessment
| Risk | Severity | Mitigation |
|------|----------|------------|

## Roadmap

### Phase Overview
```
[Timeline visualization]
```

### Phase 1: Foundation
[Summary]

### Phase 2: Quick Wins
[Summary]

### Phase 3: Core Transformation
[Summary]

### Phase 4: Optimization
[Summary]

### Key Milestones
| Milestone | Date | Criteria |
|-----------|------|----------|

## Resource Requirements

### Team
[Team structure and size]

### Skills
[Required skills and gaps]

### Infrastructure
[Infrastructure needs]

## Governance

### Decision Gates
| Gate | Timing | Decision |
|------|--------|----------|

### Success Metrics
| Metric | Target | Measurement |
|--------|--------|-------------|

## Recommendations

1. [Primary recommendation]
2. [Secondary recommendation]
3. [Additional recommendation]

## Next Steps

1. [Immediate action]
2. [Short-term action]
3. [Medium-term action]

## Appendix

### A: Detailed Work Breakdown
[Link to detailed WBS]

### B: Architecture Decision Records
[Key ADRs]

### C: Risk Register
[Full risk register]
```

**Completion Criteria**:
- [ ] Executive summary written
- [ ] All sections compiled
- [ ] Recommendations clear
- [ ] Next steps defined

**Update Progress**:
```
Mark Step 4.2 complete in assessment-progress.md
Record: timestamp, duration, output location
```

---

## Completion

### Final Steps
1. Update progress file status to "Completed"
2. Generate summary of all outputs
3. Update `assessment.md`:
   - Set `**Status**` to `completed`
   - Set all phase statuses to `completed`
   - Set all deliverable statuses to `completed`
4. Announce completion

### Outputs Summary
| Output | Location | Description |
|--------|----------|-------------|
| Project Overview | `outputs/{repo-name}/discovery/project-overview.md` | Current system overview |
| File Inventory | `outputs/{repo-name}/discovery/file-inventory.md` | Codebase analysis |
| Dependencies | `outputs/{repo-name}/discovery/dependencies.md` | Dependency analysis |
| Ilities Assessment | `outputs/{repo-name}/architecture/ilities-assessment.md` | Quality attributes |
| Patterns | `outputs/{repo-name}/architecture/patterns.md` | Pattern analysis |
| Tech Debt | `outputs/{repo-name}/tech-debt/debt-inventory.md` | Debt inventory |
| Assessment | `outputs/{repo-name}/modernization/assessment.md` | Modernization readiness |
| Target State | `outputs/{repo-name}/modernization/target-state.md` | Target architecture |
| Gaps | `outputs/{repo-name}/modernization/gaps.md` | Gap analysis |
| Candidates | `outputs/{repo-name}/modernization/candidates.md` | Strangler candidates |
| Approach | `outputs/{repo-name}/modernization/approach.md` | Rewrite/refactor decisions |
| Roadmap | `outputs/{repo-name}/modernization/roadmap.md` | Implementation roadmap |
| Final Report | `outputs/{repo-name}/modernization/modernization-report.md` | Comprehensive plan |

### Recommended Next Steps
- Present plan to stakeholders
- Secure funding and resources
- Establish program governance
- Begin Phase 1 (Foundation)
- Set up progress tracking and reporting
