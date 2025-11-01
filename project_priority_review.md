# Sword Combat Plugin - Project Priority & Size Review

**Project:** Sword Combat Evolved (Paper 1.21+) **Team:** 2 developers (@Mere-Solace owner/tester,
@iAmGiG implementation/DevOps) **Total Items:** 50 **Review Date:** 2025-10-31

---

## Executive Summary

This comprehensive review analyzes all 50 GitHub project items and provides Priority and Size
recommendations based on:

- Current project state and milestone alignment
- Team capacity (2-person team)
- Technical dependencies and blocking relationships
- Impact on core functionality vs. infrastructure improvements

**Key Findings:**

- 18 items already **Done** (36% completion rate)
- 8 items marked **Critical/P0** need immediate attention
- 15 items are **Tech Debt** that can be addressed incrementally
- 3 items are **Research** requiring investigation before implementation
- Several items lack Priority/Size assignments

---

## Priority Distribution Overview

| Priority       | Count | Percentage |
| -------------- | ----- | ---------- |
| P0 (Critical)  | 8     | 16%        |
| P1 (High)      | 12    | 24%        |
| P2 (Medium)    | 14    | 28%        |
| P3 (Low)       | 5     | 10%        |
| Research       | 5     | 10%        |
| Tech Debt      | 15    | 30%        |
| Pipe Dream     | 1     | 2%         |
| **Unassigned** | 10    | 20%        |

---

## Size Distribution Overview

| Size           | Count | Percentage |
| -------------- | ----- | ---------- |
| XS             | 4     | 8%         |
| S              | 8     | 16%        |
| M              | 12    | 24%        |
| L              | 10    | 20%        |
| XL             | 11    | 22%        |
| **Unassigned** | 15    | 30%        |

---

## Section 1: Critical/High Priority Items Needing Attention

These items should be prioritized for the upcoming milestone.

| #      | Title                                   | Current → Recommended | Size  | Justification                                           |
| ------ | --------------------------------------- | --------------------- | ----- | ------------------------------------------------------- |
| **5**  | Clean up interaction logic & improve... | Medium → **P1**       | M ✓   | Core input system, blocks milestone progress            |
| **39** | Add periodic player data auto-save      | Research → **P0**     | XL ✓  | CRITICAL: Data loss risk on crashes, affects user trust |
| **46** | Automatic Combat Capabilities           | (none) → **P1**       | **S** | Core feature for shield-based input, needed for testing |
| **2**  | Add InvUI as dependency                 | (none) → **P2**       | **M** | Enhancement, not blocking but valuable QoL              |
| **12** | File Structure and Class Structure R&D  | (none) → **Research** | **L** | Architecture decision, should precede major refactors   |
| **13** | Research displays locked to entity      | (none) → **Research** | **S** | Blocking for smooth visual effects                      |
| **14** | Display name/health/toughness above...  | (none) → **P2**       | **M** | Nice UX improvement, not blocking core                  |
| **15** | Thrown Item Marker                      | (none) → **P2**       | **S** | Visual enhancement for throwing system                  |

**Key Takeaway:** Issue #39 (data auto-save) is the highest priority - it's a critical vulnerability
that could lose player progress.

---

## Section 2: Items with Missing Priority/Size

These items need Priority and Size assignments for proper project tracking.

| #      | Title                              | Recommended Priority       | Recommended Size | Justification                                           |
| ------ | ---------------------------------- | -------------------------- | ---------------- | ------------------------------------------------------- |
| **18** | ItemDisplay Attack Animation       | → **Research**             | **L**            | Needs R&D before implementation, complex feature        |
| **24** | Address Unused Private Fields      | → **Tech Debt**            | **M**            | Code quality, requires investigation of SkillNode usage |
| **34** | Consider ItemsAdder/CraftEngine    | Tech Debt → **Pipe Dream** | M ✓              | External dependency, nice-to-have visual enhancement    |
| **37** | Remove/implement empty classes     | Tech Debt → **Tech Debt**  | XL ✓             | Correct priority, large cleanup task                    |
| **38** | Extract debug/testing code         | Tech Debt → **Tech Debt**  | L ✓              | Correct, improves production code clarity               |
| **40** | Refactor AttackAction.basicSlash() | Tech Debt → **Tech Debt**  | L ✓              | Correct, maintainability improvement                    |
| **41** | Implement/remove SwordClassType    | Research → **Research**    | XL ✓             | Needs owner decision before action                      |
| **42** | Reorganize Cache.java              | Medium → **Tech Debt**     | M ✓              | Code organization, not urgent                           |
| **43** | Architectural Review Summary       | Research → **Research**    | XL ✓             | Meta-issue tracking refactors                           |
| **48** | Add Details for Resource Pack      | Tech Debt → **P3**         | XS ✓             | Documentation, low urgency                              |

---

## Section 3: Tech Debt - Incremental Improvements

These items improve code quality but don't block features. Can be tackled incrementally.

| #      | Title                                      | Priority               | Size  | Status  | Notes                                   |
| ------ | ------------------------------------------ | ---------------------- | ----- | ------- | --------------------------------------- |
| **25** | Add Missing Javadocs                       | Tech Debt ✓            | XL ✓  | Ready   | Ongoing effort, 28% → 90% coverage goal |
| **28** | Implement Sound Generation (SoundType)     | (none) → **P3**        | **S** | Done    | Low priority enhancement                |
| **35** | Refactor SoundUtil to use org.bukkit.Sound | Tech Debt ✓            | XL ✓  | Backlog | Remove 1200+ lines of custom enum       |
| **36** | Refactor ShrinkType enum                   | Tech Debt ✓            | L ✓   | Backlog | Enhancement, but may be removed per #37 |
| **37** | Remove/implement empty classes             | Tech Debt ✓            | XL ✓  | Backlog | High value cleanup                      |
| **38** | Extract debug/testing code                 | Tech Debt ✓            | L ✓   | Backlog | Security + cleanliness                  |
| **40** | Refactor AttackAction.basicSlash()         | Tech Debt ✓            | L ✓   | Backlog | 200+ line method needs breaking up      |
| **42** | Reorganize Cache.java                      | Medium → **Tech Debt** | M ✓   | Ready   | 50+ static fields need organization     |
| **53** | Address Checkstyle violations (611)        | Tech Debt ✓            | XS ✓  | Backlog | Auto-fixable with spotlessApply         |

**Recommendation:** Tackle tech debt in "30-minute cleanup" sessions between features.

---

## Section 4: Research Items - Owner Input Needed

These require investigation or owner decisions before implementation.

| #      | Title                           | Priority          | Size  | Key Questions                                   |
| ------ | ------------------------------- | ----------------- | ----- | ----------------------------------------------- |
| **12** | File Structure R&D              | Research ✓        | **L** | What package reorganization makes sense?        |
| **13** | Research display entity locking | Research ✓        | **S** | Can displays be passengers? Performance impact? |
| **18** | ItemDisplay Attack Animation    | **Research**      | **L** | What attack style? Monster Hunter-inspired?     |
| **39** | Player data auto-save           | Research → **P0** | XL ✓  | UPGRADE TO CRITICAL - Data loss is unacceptable |
| **41** | Implement/remove SwordClassType | Research ✓        | XL ✓  | Is class system planned? Timeline?              |
| **43** | Architectural Review Summary    | Research ✓        | XL ✓  | Meta-issue for tracking other refactors         |
| **44** | Review License Choice           | Research ✓        | XS ✓  | Is Apache 2.0 appropriate?                      |

**Note:** Issue #39 should be **upgraded from Research to P0** immediately.

---

## Section 5: Completed Items (For Reference)

18 items are already **Done**. This shows strong project momentum (36% completion rate).

| #   | Title                               | Type  | Priority | Size   | Notes                   |
| --- | ----------------------------------- | ----- | -------- | ------ | ----------------------- |
| 1   | Replace EntityDeathEvent            | Issue | (none)   | (none) | Memory leak fix         |
| 3   | Use Adventure's event handlers      | Issue | (none)   | (none) | Performance improvement |
| 4   | Add Lombok                          | Issue | (none)   | (none) | Code quality            |
| 6   | Add Interpolation for Displays      | Issue | (none)   | (none) | Visual smoothness       |
| 7   | Add weapon on player's back/hip     | Issue | (none)   | (none) | Visual feature          |
| 8   | Fix dash-to-sword mechanics         | Issue | (none)   | (none) | Bug fix                 |
| 9   | Filter thrown items passable blocks | Issue | (none)   | (none) | Bug fix                 |
| 10  | Remove main/off hand logic          | Issue | (none)   | (none) | Simplification          |
| 11  | Unify Interactive Item Behavior     | Issue | (none)   | (none) | Consistency fix         |
| 16  | dev (PR)                            | PR    | Critical | XS     | Javadoc additions       |
| 17  | Add Javadocs for IntelliJ           | Issue | (none)   | (none) | Documentation           |
| 19  | Review Documentation Structure      | Issue | (none)   | (none) | DevOps infrastructure   |
| 20  | Add Gradle Wrapper                  | Issue | (none)   | (none) | Build infrastructure    |
| 21  | Set Up Automated Linting            | Issue | (none)   | (none) | CI/CD pipeline          |
| 22  | Remove Unused Imports               | Issue | (none)   | (none) | Code quality            |
| 23  | Fix Unused Local Variables          | Issue | (none)   | (none) | Code quality            |
| 26  | Generate Javadoc Coverage Report    | Issue | (none)   | (none) | Metrics baseline        |
| 27  | Create GitHub Actions CI            | Issue | (none)   | (none) | Automation              |
| 28  | Sound Generation for SoundType      | Issue | (none)   | (none) | Enhancement             |
| 29  | Add Checkstyle Configuration        | Issue | (none)   | (none) | Code quality            |
| 30  | Establish Testing Strategy          | Issue | (none)   | (none) | 39 tests written        |
| 31  | Fix Unused Variables (PR)           | PR    | (none)   | (none) | Code quality            |
| 32  | Add development infrastructure (PR) | PR    | (none)   | (none) | Major DevOps work       |
| 44  | Review License Choice               | Issue | Research | XS     | Documentation           |
| 47  | Fix ReadMe Controls                 | Issue | (none)   | (none) | Documentation fix       |
| 49  | Cherry Pick QA Branch               | Issue | Critical | M      | Branch maintenance      |
| 51  | Added Linux Instructions (PR)       | PR    | (none)   | (none) | Documentation           |

---

## Section 6: Detailed Recommendations by Issue

### Critical Priority (P0) - Do Immediately

#### Issue #39: Add periodic player data auto-save

- **Current:** Research | XL
- **Recommended:** **P0 (Critical)** | XL
- **Justification:** Data loss on crash is unacceptable. Currently only saves on shutdown, meaning
  crashes lose all session progress. This directly impacts player trust and retention.
- **Implementation:** Hybrid approach - periodic saves (10 min) + event-based (player quit, world
  save) + shutdown
- **Effort:** 1 week
- **Blocking:** None, but highest user impact

#### Issue #5: Clean up interaction logic

- **Current:** Medium | M
- **Recommended:** **P1** | M
- **Justification:** Part of "Working and Robust Input Detection System" milestone (due 2025-12-20).
  Labeled as bug + refactor, blocks robust input system.
- **Effort:** 1-2 days
- **Blocking:** Input system milestone

#### Issue #46: Automatic Combat Capabilities

- **Current:** (none) | (none)
- **Recommended:** **P1** | **S**
- **Justification:** Core feature for auto-equipping shields, essential for right-click detection.
  Simple implementation but high value for UX.
- **Effort:** 3-4 hours
- **Blocking:** None, but enables smoother onboarding

---

### High Priority (P1) - Next Sprint

#### Issue #2: Add InvUI as dependency

- **Current:** (none) | (none)
- **Recommended:** **P2** | **M**
- **Justification:** Refactor + enhancement. Improves inventory management but not blocking core
  features. Status "Ready" suggests it's been evaluated.
- **Effort:** 1 day
- **Blocking:** None

#### Issue #14: Display name/health/toughness

- **Current:** (none) | (none)
- **Recommended:** **P2** | **M**
- **Justification:** Enhancement for SwordEntities. Good UX but not blocking gameplay. Depends on
  Issue #13 (research display locking).
- **Effort:** 1 day
- **Blocking:** Issue #13 (soft dependency)

#### Issue #15: Thrown Item Marker

- **Current:** (none) | (none)
- **Recommended:** **P2** | **S**
- **Justification:** Visual enhancement showing where thrown items will land. Nice-to-have for
  throwing system clarity.
- **Effort:** 2-4 hours
- **Blocking:** None

---

### Research Priority - Investigation Required

#### Issue #12: File Structure and Class Structure R&D

- **Current:** (none) | (none)
- **Recommended:** **Research** | **L**
- **Justification:** Architecture decision that should precede major refactors. Issue #43 already
  provides comprehensive architectural review. Need to decide on package reorganization strategy.
- **Effort:** 3-5 days (investigation + planning)
- **Blocking:** Should be done before #37, #38, #40

#### Issue #13: Research displays locked to entity

- **Current:** (none) | (none)
- **Recommended:** **Research** | **S**
- **Justification:** Technical research for smooth item displays. Blocks Issue #14 (display
  name/health). Labeled "help wanted" + "research".
- **Effort:** 4-8 hours (research + prototype)
- **Blocking:** Issue #14

#### Issue #18: ItemDisplay Attack Animation

- **Current:** (none) | (none)
- **Recommended:** **Research** | **L**
- **Justification:** Complex feature needing R&D. Monster Hunter-style attacks mentioned. Labeled
  "research" + "help wanted". Not blocking core functionality.
- **Effort:** 3-5 days (research + implementation)
- **Blocking:** None

#### Issue #41: Implement/remove SwordClassType

- **Current:** Research | XL
- **Recommended:** **Research** | XL ✓
- **Justification:** Requires owner decision on whether class system (Warrior/Archer/Mage/Rogue) is
  planned. Currently unused field in CombatProfile. Need design specification before implementation.
- **Effort:** If implemented: 1-2 weeks. If removed: 1-2 hours.
- **Blocking:** None

#### Issue #43: Architectural Review Summary

- **Current:** Research | XL
- **Recommended:** **Research** | XL ✓
- **Justification:** Meta-issue tracking architectural improvements. Already provides comprehensive
  analysis. Acts as parent issue for #37-#42. Keep as Research for tracking purposes.
- **Effort:** N/A (tracking issue)
- **Blocking:** None (tracks other issues)

---

### Tech Debt - Incremental Improvements

#### Issue #24: Address Unused Private Fields

- **Current:** (none) | (none)
- **Recommended:** **Tech Debt** | **M**
- **Justification:** 13 unused fields in SkillNode, 2 in SkillSequence, others scattered. Requires
  investigation - may indicate incomplete features (Issue #41 class system) or dead code (Issue #37
  empty classes).
- **Effort:** 1 day (investigation + cleanup)
- **Blocking:** None, but related to #37 and #41

#### Issue #25: Add Missing Javadocs

- **Current:** Tech Debt | XL
- **Recommended:** **Tech Debt** | XL ✓
- **Justification:** Correctly prioritized. Currently 28% coverage, goal is 90%. Incremental
  improvement. Phase 1 (core classes) → Phase 2 (utilities) → Phase 3 (complete).
- **Effort:** Ongoing, 2-3 hours per week
- **Blocking:** None

#### Issue #28: Implement Sound Generation

- **Current:** (none) | (none)
- **Recommended:** **P3** | **S**
- **Justification:** TODO comment for completing SoundType enum. However, Issue #35 recommends
  removing this enum entirely in favor of org.bukkit.Sound. Marking P3 until #35 is decided.
- **Effort:** 1-2 hours (but may be obsolete)
- **Blocking:** Blocked by Issue #35 decision

#### Issue #34: Consider ItemsAdder/CraftEngine

- **Current:** Tech Debt | M
- **Recommended:** **Pipe Dream** | M ✓
- **Justification:** External dependency for enhanced visuals. Nice-to-have but adds complexity,
  requires resource packs. Low priority suggestion from code review. Not essential for core
  functionality.
- **Effort:** 2-3 days (integration + testing)
- **Blocking:** None

#### Issue #35: Refactor SoundUtil

- **Current:** Tech Debt | XL
- **Recommended:** **Tech Debt** | XL ✓
- **Justification:** Correctly prioritized. Removes 1200+ lines of manually-maintained SoundType
  enum in favor of org.bukkit.Sound. Large refactor but high value. Closes Issue #28.
- **Effort:** 1 week (find/replace ~20-30 files)
- **Blocking:** None, but should be done before #28

#### Issue #36: Refactor ShrinkType enum

- **Current:** Tech Debt | L
- **Recommended:** **Tech Debt** | L ✓ (or remove if #37 removes DisplayWrapper)
- **Justification:** Enhancement for functional enum pattern. However, Issue #37 recommends removing
  DisplayWrapper entirely as unused. Hold until #37 is decided.
- **Effort:** 2-3 hours (if needed)
- **Blocking:** Blocked by Issue #37 decision

#### Issue #37: Remove/implement empty classes

- **Current:** Tech Debt | XL
- **Recommended:** **Tech Debt** | XL ✓
- **Justification:** Correctly prioritized. CommandManager, StatusAction, SkillAction, Skill
  package, DisplayWrapper all empty or incomplete. High-value cleanup. Should be done early.
- **Effort:** 3-5 days (investigation + removal + verification)
- **Blocking:** None, but affects #36

#### Issue #38: Extract debug/testing code

- **Current:** Tech Debt | L
- **Recommended:** **Tech Debt** | L ✓
- **Justification:** Correctly prioritized. Production classes contain debug code (chat commands,
  test methods, hardcoded test values). Extract to btm.sword.debug package. Improves security
  (permission system) and cleanliness.
- **Effort:** 2-3 days
- **Blocking:** None

#### Issue #40: Refactor AttackAction.basicSlash()

- **Current:** Tech Debt | L
- **Recommended:** **Tech Debt** | L ✓
- **Justification:** Correctly prioritized. 200+ line method with multiple responsibilities. Extract
  methods, create AttackConfig. Improves maintainability and testability. Not urgent but valuable.
- **Effort:** 2-3 days
- **Blocking:** None

#### Issue #42: Reorganize Cache.java

- **Current:** Medium | M
- **Recommended:** **Tech Debt** | M ✓
- **Justification:** Should be Tech Debt, not Medium priority. 50+ static fields in flat structure
  with generic names (v1, v2, v3). Reorganize into nested static classes (Particles, AttackShapes,
  Directions). Improves discoverability.
- **Effort:** 1 day (refactor + find/replace)
- **Blocking:** None

#### Issue #45: DevOps Overhaul

- **Current:** Critical | XL
- **Recommended:** **(Completed)** Keep as-is for historical reference
- **Justification:** This was the major DevOps push (testing, linting, documentation). Marked as
  "Ready" status but describes completed work. Should be "Done". Keep priority/size for tracking
  purposes.
- **Effort:** Already completed
- **Blocking:** N/A

#### Issue #48: Add Resource Pack Details

- **Current:** Tech Dept | XS
- **Recommended:** **P3** | XS ✓
- **Justification:** Documentation for server.properties resource pack setup. Low priority, simple
  documentation task. Correctly sized as XS.
- **Effort:** 15-30 minutes
- **Blocking:** None

#### Issue #53: Address Checkstyle violations

- **Current:** Tech Dept | XS
- **Recommended:** **Tech Debt** | XS ✓
- **Justification:** 611 violations across 47 files. Most auto-fixable with
  `./gradlew spotlessApply`. Good first issue. Low effort, high visual impact on code quality.
- **Effort:** 1-2 hours (mostly automated)
- **Blocking:** None

---

## Section 7: Recommended Sprint Planning

### Sprint 1: Critical Data & Input (2 weeks)

**Goal:** Fix data loss vulnerability and stabilize input system

1. **Issue #39** (P0) - Player data auto-save [5 days]
2. **Issue #5** (P1) - Clean up interaction logic [2 days]
3. **Issue #46** (P1) - Automatic combat capabilities [0.5 day]
4. **Issue #53** (Tech Debt) - Address Checkstyle violations [0.25 day]

**Deliverables:**

- No more data loss on crashes
- Robust input detection for milestone
- Auto-shield equipping
- Clean code formatting

---

### Sprint 2: Research & Architecture (2 weeks)

**Goal:** Make key architectural decisions and research blocking issues

1. **Issue #12** (Research) - File structure R&D [4 days]
2. **Issue #13** (Research) - Display entity locking [1 day]
3. **Issue #41** (Research) - Class system decision [1 day]
4. **Issue #37** (Tech Debt) - Remove empty classes [4 days]

**Deliverables:**

- Architecture decision on package reorganization
- Display locking solution for smooth visuals
- Decision on class system (implement vs. remove)
- Cleaned up codebase (removed unused classes)

---

### Sprint 3: Tech Debt & Enhancements (2 weeks)

**Goal:** Code quality improvements and incremental features

1. **Issue #38** (Tech Debt) - Extract debug code [3 days]
2. **Issue #42** (Tech Debt) - Reorganize Cache.java [1 day]
3. **Issue #14** (P2) - Display name/health/toughness [1 day]
4. **Issue #15** (P2) - Thrown item marker [0.5 day]
5. **Issue #2** (P2) - Add InvUI dependency [1 day]
6. **Issue #48** (P3) - Resource pack docs [0.25 day]

**Deliverables:**

- Clean separation of production and debug code
- Better organized cache system
- Enhanced visual feedback for entities
- Improved inventory management

---

### Sprint 4: Large Refactors (2-3 weeks)

**Goal:** Major code improvements for long-term maintainability

1. **Issue #35** (Tech Debt) - Refactor SoundUtil [5 days]
2. **Issue #40** (Tech Debt) - Refactor AttackAction [3 days]
3. **Issue #25** (Tech Debt) - Add Javadocs (Phase 1: Core) [2 days]
4. **Issue #24** (Tech Debt) - Address unused fields [1 day]

**Deliverables:**

- Removed 1200+ line SoundType enum
- Maintainable attack action code
- 50%+ Javadoc coverage
- Clean field usage

---

### Backlog: Future Enhancements

**Goal:** Advanced features when core stabilized

1. **Issue #18** (Research) - ItemDisplay attack animations
2. **Issue #34** (Pipe Dream) - ItemsAdder/CraftEngine integration
3. **Issue #28** (P3) - Sound generation (if #35 doesn't obsolete)
4. **Issue #36** (Tech Debt) - ShrinkType refactor (if DisplayWrapper kept)

---

## Section 8: Team Workload Distribution

Given a 2-person team:

### @Mere-Solace (Owner/Tester)

**Focus Areas:**

- Issue #39 (Data save) - Owner decision on backup strategy
- Issue #41 (Class system) - Design decision required
- Issue #12 (File structure) - Architecture review
- Issue #5 (Interaction logic) - Testing and validation
- Issue #13 (Display research) - Technical research
- Issue #18 (Attack animations) - Design direction

**Workload:** 50% implementation, 50% testing/decisions

---

### @iAmGiG (Implementation/DevOps)

**Focus Areas:**

- Issue #37 (Remove empty classes) - Code cleanup
- Issue #38 (Extract debug) - Code organization
- Issue #40 (Refactor AttackAction) - Code quality
- Issue #42 (Reorganize Cache) - Code organization
- Issue #35 (Refactor SoundUtil) - Large refactor
- Issue #53 (Checkstyle) - Quick wins

**Workload:** 80% implementation, 20% DevOps maintenance

---

## Section 9: Risk Assessment

### High Risk Items

1. **Issue #39 (Data save)** - Complex concurrency, file I/O errors, must be bulletproof
2. **Issue #35 (SoundUtil refactor)** - Touches ~20-30 files, high regression risk
3. **Issue #37 (Remove classes)** - May break assumptions, need thorough testing

### Medium Risk Items

1. **Issue #12 (File structure)** - Large reorganization, merge conflicts likely
2. **Issue #40 (AttackAction refactor)** - Core combat logic, thorough testing needed
3. **Issue #5 (Interaction logic)** - Edge cases in input system

### Low Risk Items

1. **Issue #53 (Checkstyle)** - Mostly automated
2. **Issue #42 (Cache reorganize)** - Find/replace, compile-time checks
3. **Issue #48 (Resource pack docs)** - Documentation only

---

## Section 10: Dependencies & Blocking Relationships

```
Issue #39 (Data save) → BLOCKS nothing, HIGH PRIORITY standalone
Issue #5 (Interaction) → BLOCKS Input milestone completion
Issue #13 (Display research) → BLOCKS #14 (Display name/health)
Issue #12 (File structure) → SHOULD PRECEDE #37, #38, #40 (refactors)
Issue #37 (Remove empty) → BLOCKS #36 (ShrinkType - may be removed)
Issue #35 (Sound refactor) → BLOCKS #28 (Sound generation - may obsolete)
Issue #41 (Class system) → RELATED TO #24 (unused fields - may explain them)
```

**Recommended Order:**

1. #39 (Critical, no dependencies)
2. #5 (Milestone blocker)
3. #13 (Blocks #14)
4. #12 (Informs other refactors)
5. #37 (Cleanup before refactors)
6. #38, #40, #42 (Refactors)
7. #35 (Large refactor, should be after cleanup)

---

## Section 11: Missing Information

These items need clarification or additional context:

1. **Issue #16 (PR: dev)** - Marked Critical/XS but already Done. Why was this Critical? Historical
   context only.
2. **Issue #31 (PR: Fix unused variables)** - Labeled "bug" but it's code cleanup. Should be
   "improvement".
3. **Issue #32 (PR: Infrastructure)** - Massive PR, marked Done. Verify all components working.
4. **Issue #45 (DevOps Overhaul)** - Marked Ready but describes completed work. Should be Done.
5. **Issue #51 (PR: Linux instructions)** - Done, good contribution tracking.

---

## Section 12: Label Consistency Review

Some labeling inconsistencies to address:

- **"needs-review"** vs **"review-needed"** - Use one consistently
- **"upgrade or change"** - Non-standard label, use "enhancement" or "refactor"
- **"devops"** vs **"infrastructure"** vs **"automation"** - Overlapping meanings
- **"question"** - Often paired with Research priority, good usage
- **"help wanted"** - Good for community contributions

---

## Section 13: Milestone Alignment

**Current Milestone:** "Working and Robust Input Detection System" (Due: 2025-12-20)

**Items in Milestone:**

- Issue #5 (Clean up interaction logic)

**Recommended Additions:**

- Issue #46 (Automatic combat capabilities)
- Issue #13 (Display research - for visual feedback)
- Issue #39 (Data save - critical before launch)

**Timeline Check:**

- 7 weeks until milestone deadline
- Sprint 1-2 can complete critical path
- Sprint 3-4 are polish/enhancement
- **Milestone is achievable**

---

## Section 14: Quick Wins (High Value, Low Effort)

These items deliver significant value for minimal time investment:

1. **Issue #53** - Checkstyle violations (XS, automated) → Clean codebase
2. **Issue #46** - Auto-equip shields (S, 3-4 hours) → Better UX
3. **Issue #48** - Resource pack docs (XS, 30 min) → Easier setup
4. **Issue #15** - Thrown item marker (S, 2-4 hours) → Visual clarity

**Recommendation:** Knock these out in "cleanup sessions" between larger features.

---

## Section 15: Summary Table - All 50 Items

| #   | Title (Truncated)                   | Current Priority | Recommended | Current Size | Recommended | Status  | Justification                     |
| --- | ----------------------------------- | ---------------- | ----------- | ------------ | ----------- | ------- | --------------------------------- |
| 1   | Replace EntityDeathEvent            | (none)           | -           | (none)       | -           | Done    | Completed memory leak fix         |
| 2   | Add InvUI dependency                | (none)           | P2          | (none)       | M           | Ready   | Enhancement, not blocking         |
| 3   | Use Adventure's event handlers      | (none)           | -           | (none)       | -           | Done    | Performance improvement           |
| 4   | Add Lombok                          | (none)           | -           | (none)       | -           | Done    | Code quality                      |
| 5   | Clean up interaction logic          | Medium           | P1          | M            | M           | Backlog | Milestone blocker                 |
| 6   | Add Interpolation for Displays      | (none)           | -           | (none)       | -           | Done    | Visual smoothness                 |
| 7   | Add weapon on back/hip              | (none)           | -           | (none)       | -           | Done    | Visual feature                    |
| 8   | Fix dash-to-sword mechanisms        | (none)           | -           | (none)       | -           | Done    | Bug fix                           |
| 9   | Filter thrown items passable blocks | (none)           | -           | (none)       | -           | Done    | Bug fix                           |
| 10  | Remove main/off hand logic          | (none)           | -           | (none)       | -           | Done    | Simplification                    |
| 11  | Unify Interactive Item Behavior     | (none)           | -           | (none)       | -           | Done    | Consistency                       |
| 12  | File Structure R&D                  | (none)           | Research    | (none)       | L           | Ready   | Architecture decision             |
| 13  | Research display entity locking     | (none)           | Research    | (none)       | S           | Ready   | Blocks #14                        |
| 14  | Display name/health/toughness       | (none)           | P2          | (none)       | M           | Ready   | Visual enhancement                |
| 15  | Thrown Item Marker                  | (none)           | P2          | (none)       | S           | Ready   | Visual enhancement                |
| 16  | dev (PR)                            | Critical         | -           | XS           | -           | Done    | Javadoc additions                 |
| 17  | Add Javadocs for IntelliJ           | (none)           | -           | (none)       | -           | Done    | Documentation                     |
| 18  | ItemDisplay Attack Animation        | (none)           | Research    | (none)       | L           | Ready   | Complex R&D feature               |
| 19  | Review Documentation Structure      | (none)           | -           | (none)       | -           | Done    | DevOps infra                      |
| 20  | Add Gradle Wrapper                  | (none)           | -           | (none)       | -           | Done    | Build infra                       |
| 21  | Set Up Automated Linting            | (none)           | -           | (none)       | -           | Done    | CI/CD                             |
| 22  | Remove Unused Imports               | (none)           | -           | (none)       | -           | Done    | Code quality                      |
| 23  | Fix Unused Local Variables          | (none)           | -           | (none)       | -           | Done    | Code quality                      |
| 24  | Address Unused Private Fields       | (none)           | Tech Debt   | (none)       | M           | Ready   | Code investigation                |
| 25  | Add Missing Javadocs                | Tech Debt        | Tech Debt   | XL           | XL          | Ready   | Ongoing effort                    |
| 26  | Generate Javadoc Coverage Report    | (none)           | -           | (none)       | -           | Done    | Metrics baseline                  |
| 27  | Create GitHub Actions CI            | (none)           | -           | (none)       | -           | Done    | Automation                        |
| 28  | Sound Generation for SoundType      | (none)           | P3          | (none)       | S           | Done    | Low priority (may be obsolete)    |
| 29  | Add Checkstyle Configuration        | (none)           | -           | (none)       | -           | Done    | Code quality                      |
| 30  | Establish Testing Strategy          | (none)           | -           | (none)       | -           | Done    | 39 tests written                  |
| 31  | Fix Unused Variables (PR)           | (none)           | -           | (none)       | -           | Done    | Code quality                      |
| 32  | Add dev infrastructure (PR)         | (none)           | -           | (none)       | -           | Done    | Major DevOps                      |
| 34  | Consider ItemsAdder/CraftEngine     | Tech Debt        | Pipe Dream  | M            | M           | Backlog | External dependency, nice-to-have |
| 35  | Refactor SoundUtil                  | Tech Debt        | Tech Debt   | XL           | XL          | Backlog | Remove 1200+ enum lines           |
| 36  | Refactor ShrinkType enum            | Tech Debt        | Tech Debt   | L            | L           | Backlog | May be removed per #37            |
| 37  | Remove/implement empty classes      | Tech Debt        | Tech Debt   | XL           | XL          | Backlog | High-value cleanup                |
| 38  | Extract debug/testing code          | Tech Debt        | Tech Debt   | L            | L           | Backlog | Security + cleanliness            |
| 39  | Player data auto-save               | Research         | **P0**      | XL           | XL          | Backlog | **CRITICAL data loss risk**       |
| 40  | Refactor AttackAction.basicSlash()  | Tech Debt        | Tech Debt   | L            | L           | Backlog | 200+ line method                  |
| 41  | Implement/remove SwordClassType     | Research         | Research    | XL           | XL          | Backlog | Owner decision needed             |
| 42  | Reorganize Cache.java               | Medium           | Tech Debt   | M            | M           | Ready   | Code organization                 |
| 43  | Architectural Review Summary        | Research         | Research    | XL           | XL          | Backlog | Meta-issue tracking               |
| 44  | Review License Choice               | Research         | -           | XS           | -           | Done    | Documentation                     |
| 45  | DevOps Overhaul                     | Critical         | -           | XL           | -           | Ready   | Should be Done                    |
| 46  | Automatic Combat Capabilities       | (none)           | P1          | (none)       | S           | Backlog | Core feature, simple              |
| 47  | Fix ReadMe Controls                 | (none)           | -           | (none)       | -           | Done    | Documentation fix                 |
| 48  | Add Resource Pack Details           | Tech Dept        | P3          | XS           | XS          | Backlog | Simple docs                       |
| 49  | Cherry Pick QA Branch               | Critical         | -           | M            | -           | Done    | Branch maintenance                |
| 51  | Added Linux Instructions (PR)       | (none)           | -           | (none)       | -           | Done    | Documentation                     |
| 53  | Address Checkstyle violations       | Tech Dept        | Tech Debt   | XS           | XS          | Backlog | Auto-fixable                      |

---

## Section 16: Final Recommendations

### Immediate Actions (This Week)

1. **Upgrade Issue #39 to P0** - Data loss is unacceptable
2. **Assign Issue #46 as P1/S** - Core feature, quick win
3. **Review Issue #45** - Mark as Done if completed
4. **Sprint Planning** - Use Sprint 1 recommendations above

### Short-term (Next Month)

1. **Complete Sprint 1** - Fix data save + input system
2. **Make architectural decisions** - File structure, class system
3. **Clean up codebase** - Remove empty classes, extract debug code
4. **Track tech debt progress** - 1-2 tech debt items per sprint

### Long-term (Next Quarter)

1. **Major refactors** - SoundUtil, AttackAction, Cache reorganization
2. **Visual enhancements** - Attack animations, display improvements
3. **Documentation** - Javadoc coverage to 90%
4. **Testing** - Maintain test coverage as features added

### Project Health Metrics to Track

- **Javadoc Coverage:** 28% → 50% → 75% → 90%
- **Checkstyle Violations:** 611 → <100 → <50
- **Test Coverage:** 39 tests → expand as features grow
- **Open Issues:** 50 → Focus on high-priority items
- **Milestone Progress:** Input System (due 2025-12-20) - on track

---

## Appendix A: Priority Guidelines Reference

- **P0 (Critical/Red):** Blocks development, critical bugs, security issues, data loss
- **P1 (Orange):** Important features, significant bugs, milestone blockers
- **P2 (Yellow):** Standard work, enhancements, nice-to-have features
- **P3 (Yellow):** Low priority, nice-to-haves, polish
- **Research (Green):** Investigation/spike work, requires design decisions
- **Tech Dept (Pink):** Code quality, refactoring, linting, documentation
- **Pipe Dream (Gray):** Future ideas, not current focus, low priority

## Appendix B: Size Guidelines Reference

- **XS:** < 1 hour (simple changes, config updates, doc fixes)
- **S:** 1-4 hours (small features, bug fixes, quick enhancements)
- **M:** 1-2 days (medium features, moderate refactors)
- **L:** 3-5 days (large features, significant refactors, investigations)
- **XL:** 1+ week (epics, major architectural work, multi-file refactors)

---

**End of Review**

_This comprehensive analysis provides a roadmap for the next 3-6 months of development. Focus on
critical items first (data save), then stabilize the input system for the milestone, followed by
incremental tech debt improvements. The project has strong momentum (36% completion rate) and solid
DevOps infrastructure thanks to recent work._

**Questions?** Review this document with your team and adjust priorities based on your specific
goals and timeline constraints.
