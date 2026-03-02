# Input Execution System

Package: `btm.sword.system.input`

## Overview

The input execution system translates raw Minecraft player events (left-click, right-click, drop, swap, sneak) into typed `InputType` signals, routes those signals through a **trie-based finite state machine** called `InputExecutionTree`, and executes the `InputAction` associated with the matching sequence. It is the single source of truth for every player action in the plugin — melee attacks, dashes, grabs, throws, UmbralBlade commands, and skill casts all flow through this system.

---

## Package Structure

| Class | Role |
|---|---|
| `InputType` | Enum of nine logical input signals |
| `InputKey` | Record pairing an `InputType` with a set of allowed `SwordItemType`s and an optional accessibility predicate |
| `InputExecutionTree` | The trie. Owns the root `InputNode`, traversal state, timeout timer, and HUD rendering |
| `InputExecutionTree.InputNode` | A trie node. Holds children, a `Supplier<InputAction>`, node metadata, and a visibility predicate |
| `InputExecutionTree.InputNodeBuilder` | Builder for registering sequences into the trie during `initializeInputTree()` |
| `InputAction` | Encapsulates the executable unit: action consumer, cooldown, cast gating predicate, `ActionConstraint` list, soulfire cost, and cast duration |
| `InputActionExecutor` | Static helper that runs `canCast` checks and delegates to `ActionCaster.cast()` |
| `ActivationContext` | Enum of three player contexts (`NORMAL`, `STUNNED`, `CHANNELING`) used by node `visibleIf` predicates |

Supporting classes outside this package:

| Class | Package | Role |
|---|---|---|
| `ActionConstraint` | `system.action.constraint` | Composable `Predicate<Combatant>` evaluated before execution |
| `CommonConstraints` | `system.action.constraint` | Named, reusable `ActionConstraint` constants |
| `ActionCaster` | `system.action` | Schedules the action on the main thread and sets the cast-blocking `BukkitTask` |
| `SwordAction` | `system.action` | Empty shell. Casting logic was extracted to `ActionCaster` |
| `SkillSlotActionFactory` | `system.action.skill.container` | Resolves a `SkillSlot` to an `InputAction` at call time |
| `InputListener` | `listeners` | Bukkit event handler that converts raw events to `InputType` and calls `SwordPlayer.act()` |

---

## InputType Enum

Nine logical signals cover every physically distinct input a player can produce:

```
LEFT         — left-click (attack)
RIGHT        — right mouse button pressed down (begins hold timer)
RIGHT_TAP    — right mouse button released after < 162 ms
RIGHT_HOLD   — right mouse button held >= 162 ms
DROP         — Q key (drop item)
SWAP         — F key (swap hands)
SHIFT        — sneak key pressed down (begins hold timer)
SHIFT_TAP    — sneak key released after < 162 ms
SHIFT_HOLD   — sneak key held >= 162 ms
```

`RIGHT` and `SHIFT` are never sent directly to the trie; they trigger hold timers inside `SwordPlayer`. When the timer ends, either `*_TAP` or `*_HOLD` is dispatched instead.

---

## InputKey

`InputKey` is a record: `(InputType input, List<SwordItemType> allowedItemTypes, Predicate<SwordPlayer> accessibilityPredicate)`.

`checkAccessibility(SwordPlayer)` returns `true` when both the predicate passes and the player's currently held item matches one of the `allowedItemTypes`. `SwordItemType.GENERIC` acts as a wildcard — it matches any item. Every `InputKey.of(InputType)` factory call produces a `GENERIC` key.

Keys are stored as the map keys in `InputNode.children`. Path resolution in `retrieveAvailableNode()` checks `InputType` equality, then calls `checkAccessibility()`. Separate item-specific and item-generic keys for the same `InputType` can coexist in the same node's children because `noMatchingChild()` blocks insertion only when the new key's allowed types overlap with an existing key's allowed types.

---

## The Trie — InputExecutionTree

### Structure

```
root (InputNode, action = null)
 ├── SWAP
 │    └── SWAP                    → dash forward
 │         └── LEFT               → dash-forward attack
 ├── SHIFT
 │    ├── SHIFT                   → dash backward
 │    │    └── LEFT               → dash-backward attack
 │    ├── LEFT                    → grab
 │    ├── SWAP                    → UmbralBlade toggle
 │    └── DROP                    → UmbralBlade wield
 ├── LEFT [GENERIC]
 │    ├── (leaf)                  → basic attack 1
 │    └── LEFT [GENERIC]
 │         ├── (leaf)             → basic attack 2
 │         └── LEFT [GENERIC]
 │              └── (leaf)        → basic attack 3
 ├── LEFT [UMBRAL_LINK]
 │    └── LEFT [UMBRAL_LINK]
 │         └── LEFT [UMBRAL_LINK] → umbral link attack 3
 ├── DROP
 │    ├── RIGHT                   → throw ready
 │    │    └── RIGHT_HOLD         → throw item
 │    ├── LEFT                    → heavy sweep 1
 │    │    └── RIGHT              → heavy sweep 2
 │    │         └── LEFT          → heavy sweep 3
 │    └── DROP                    → debug kill
 └── SWAP
      └── LEFT
           ├── LEFT               → skill UMBRAL_1
           ├── RIGHT              → skill UMBRAL_2
           └── SWAP               → skill UMBRAL_3
```

(Abbreviated — see `initializeInputTree()` for the complete definition.)

### Traversal

`InputExecutionTree` owns:

- `root` — the permanent start node
- `currentNode` — the active position in the trie (starts at `root`)
- `timeoutTimer` — a scheduled `SwordScheduler` task that calls `reset()` after inactivity

`step(InputKey inputKey)` is the core method:

1. Cancels any running timeout timer.
2. If the current node has no children (it is a leaf), resets to root before stepping.
3. Calls `currentNode.retrieveAvailableNode(inputKey, owner)` to find the matching child.
4. If no match is found at root, returns null (no-op). If no match is found mid-sequence, resets to root; if the node was `cancellable`, retries the step from root (allowing a new sequence to begin immediately without losing the input).
5. If the next node has an `InputAction` and `canCast` fails, increments `currentAttempts`. At three consecutive failures the tree force-resets.
6. Appends the input's display character to `baseSequenceToDisplay`.
7. Sets `currentNode = next`.
8. If the new node has children, builds `potentialInputSelectionText` showing each reachable follow-up with ready/not-ready coloring, then starts the timeout timer.

`SwordPlayer.act()` calls `step()` and then, on a non-null result, calls `InputActionExecutor.execute()` on the resolved action.

### Timeout

Each `InputNode` has a `timeoutTicks` field (default 20 ticks = 1 second). When a node with children is reached, a one-shot timer fires `reset()` after `timeoutTicks * 50 ms`. Starting a new input restarts the timer. The timeout enforces that players must complete a combo within the allotted window or the sequence resets.

---

## InputNode

Each node in the trie stores:

| Field | Type | Purpose |
|---|---|---|
| `children` | `LinkedHashMap<InputKey, InputNode>` | Ordered child map; insertion order matters for HUD display |
| `action` | `Supplier<InputAction>` | Factory for the associated action; null for intermediate nodes |
| `cachedAction` | `InputAction` | Cached result for non-dynamic nodes; preserves `timeLastExecuted` across calls |
| `dynamic` | `boolean` | When true, supplier is invoked fresh every `resolveAction()` call (used for skill-slot nodes) |
| `timeoutTicks` | `int` | Timeout after reaching this node while children remain |
| `minHoldTime` | `int` | Minimum milliseconds a hold input must be held (applies to `RIGHT_HOLD`/`SHIFT_HOLD` nodes) |
| `sameItemRequired` | `boolean` | If true, switching items mid-sequence resets the tree |
| `cancellable` | `boolean` | If true, a failed step here allows the input to re-attempt from root |
| `display` | `boolean` | Whether the sequence so far is shown as a HUD title |
| `visibleIf` | `Predicate<SwordPlayer>` | Controls whether this node is exposed in the HUD and is navigable |

### Static vs. Dynamic Nodes

Non-dynamic (static) nodes call `action.get()` once and cache the result. This is essential: `InputAction.timeLastExecuted` is an instance field, and cooldown tracking depends on it surviving between calls. If a new `InputAction` were constructed on every step, the cooldown clock would always read zero.

Dynamic nodes (currently only skill-slot nodes) re-invoke the supplier each time so that they always reflect the currently equipped skill. Dynamic nodes do not persist cooldown state on the `InputAction` itself — cooldown tracking for skills must live on the `Skill` or `ActiveSkill` object.

---

## InputAction

`InputAction` is the executable unit. It is built via the static `InputAction.Builder`:

```java
InputAction.builder()
    .action(executor -> AttackAction.basicAttack(executor, 1))
    .cooldown(Combatant::getDurationOfLastAttack)
    .canCast(Combatant::canPerformAction)
    .constraints(CommonConstraints.CAN_ACT, CommonConstraints.HOLDING_WEAPON)
    .requiredSoulfire(() -> 5f)
    .castDuration(() -> 300)
    .displayCooldown(true)
    .displayDisabled(true)
    .resetIfCannotPerform(false)
    .build();
```

### Fields

| Field | Type | Purpose |
|---|---|---|
| `action` | `Consumer<Combatant>` | The actual work to perform |
| `cooldownCalculation` | `Function<Combatant, Integer>` | Returns cooldown in ms for this executor |
| `canCastAbility` | `Predicate<Combatant>` | Legacy single-predicate gate (evaluated after constraints) |
| `constraints` | `List<ActionConstraint>` | Composable precondition list; all must pass |
| `requiredSoulfire` | `Function<Combatant, Float>` | Soulfire cost |
| `castDuration` | `Function<Combatant, Integer>` | Duration in ms to block other actions after firing |
| `displayCooldown` | `boolean` | Show remaining cooldown as HUD title on failure |
| `displayDisabled` | `boolean` | Show disabled effect on failure |
| `resetIfCannotPerform` | `boolean` | Reset tree on failure |
| `timeLastExecuted` | `long` | Timestamp of last successful execution (ms) |

### Execution Gate — `handlePerformAttempt`

Called by `InputActionExecutor.execute()` before the action runs:

1. Check cooldown: `System.currentTimeMillis() - timeLastExecuted <= cooldown`. Fail → optionally display remaining cooldown, call `handleExecutionFailure` (which calls `swordPlayer.resetTree()`).
2. Evaluate all `ActionConstraint`s in order. First failure → optionally display disabled effect and/or reset.
3. Evaluate `canCastAbility` predicate. Failure → optionally display and/or reset.
4. Consume soulfire.
5. Return true — caller proceeds to `perform()`.

`perform(executor)` runs `action.accept(executor)` and sets `timeLastExecuted`.

---

## ActionConstraint System

`ActionConstraint` is a `@FunctionalInterface` extending `Predicate<Combatant>` with a `default String failureReason()` method.

`CommonConstraints` provides the standard named constants:

| Constant | Condition |
|---|---|
| `NOT_CASTING` | `abilityCastTask == null` |
| `NOT_GRABBING` | `!isGrabbing()` |
| `NOT_GRABBED` | `!isGrabbed()` |
| `CAN_ACT` | Not casting, not grabbing, not grabbed |
| `HOLDING_ITEM` | Main hand is not empty |
| `HOLDING_WEAPON` | Main hand is not empty, bow, or crossbow |
| `UMBRAL_LINK_ATTACK_READY` | `CAN_ACT` + blade exists and is in standby/recalling/sheathed/lodged |
| `UMBRAL_ACTION_READY` | Same as above |
| `AIR_DASH_AVAILABLE` | `canAirDash()` |
| `BLADE_IS_LODGED` | `CAN_ACT` + blade is lodged |

Constraints are checked in `InputActionExecutor.canCast()` (for HUD readiness display) and in `InputAction.handlePerformAttempt()` (for execution gating). Both paths must agree.

**Current state:** Constraints exist and are wired into the `Builder` and `handlePerformAttempt`. However, most entries in `initializeInputTree()` still use the legacy `canCast(Predicate)` path rather than `.constraints(...)`. The constraint system is functional but adoption is partial.

---

## ActionCaster

`ActionCaster.cast(Combatant executor, int castDurationMillis, Runnable action)`:

1. Schedules the action on the main server thread via `Bukkit.getScheduler().runTask()`.
2. If `castDurationMillis > 0`, sets `executor.setCastTask(castTask)` to block subsequent ability casts.
3. Schedules a follow-up task via `SwordScheduler` to clear the cast task after `castDurationMillis / globalTimeScale` ms.

The cast task reference is what `canPerformAction()` and `CommonConstraints.NOT_CASTING` check — a non-null task means the player is locked out.

---

## ActivationContext

`ActivationContext` is an enum currently declared with three values:

| Value | Intent |
|---|---|
| `NORMAL` | Default — all accessible paths are visible |
| `STUNNED` | Player is grabbed or incapacitated; restricted action set |
| `CHANNELING` | Player is mid-cast; only interrupts or continuations visible |

`SwordPlayer` holds a `@Getter @Setter ActivationContext activationContext`, initialized to `NORMAL`. The `InputNode.visibleIf` predicate receives the `SwordPlayer` and can call `player.getActivationContext()` to gate visibility.

**Current state:** The enum is defined and the field exists on `SwordPlayer`. No node in `initializeInputTree()` currently uses a `visibleIf` predicate that reads `activationContext`. The infrastructure is ready but the context-switching logic is not yet wired.

---

## Input Detection Pipeline

The complete path from physical key press to executed action:

```
Bukkit Event
    │
    ▼
InputListener (onNormalAttack / onPlayerInteract / onPlayerDropEvent /
               onSneakEvent / onSwapEvent / onChangeItemEvent)
    │
    │  handleItemInteraction() — intercept special items (e.g., main menu button)
    │  cancel event if intercepted
    │
    ▼
SwordPlayer.act(InputType input)
    │
    ├── Early exits:
    │     isAttemptingThrow() + wrong input → ThrowAction.throwCancel(), resetTree()
    │     isGrabbing() + SWAP              → release grab
    │     isGrabbing() + LEFT              → grab hit
    │     abilityCastTask != null          → return (blocked)
    │
    ├── RIGHT input → startHoldingRight()
    │     TimeArbiter task polls isHandRaised()
    │     on release: timeRightHeld < 162 ms → act(RIGHT_TAP)
    │                 timeRightHeld ≥ 162 ms → act(RIGHT_HOLD)
    │
    ├── SHIFT input → startSneaking()
    │     Same timing logic → SHIFT_TAP or SHIFT_HOLD
    │
    ├── RIGHT_TAP / SHIFT_TAP → only proceed if nextExists(inputKey)
    │
    ├── RIGHT_HOLD / SHIFT_HOLD → check minHoldTime of next node
    │
    ▼
InputExecutionTree.step(InputKey)
    │
    ├── retrieve next node
    ├── canCast pre-check (attempt gate)
    ├── update HUD display components
    ├── start/restart timeout timer
    │
    ▼
SwordPlayer.act() resumes
    │
    ├── check soulfire (pre-deduction display check)
    │
    ▼
InputActionExecutor.execute(InputAction, Combatant)
    │
    ├── InputAction.handlePerformAttempt()
    │     cooldown check → fail if on cooldown
    │     ActionConstraint list → fail if any fail
    │     canCastAbility predicate → fail if fails
    │     consume soulfire
    │
    ▼
ActionCaster.cast(executor, castDurationMillis, () -> action.perform(executor))
    │
    ├── Bukkit.getScheduler().runTask() — schedules on main thread
    └── if castDuration > 0: setCastTask(), schedule clear after duration
```

### RIGHT and SHIFT Hold Detection

`startHoldingRight()` replaces the main-hand item with gunpowder (to prevent Minecraft's vanilla use-item behavior from firing) and starts a `TimeArbiter` repeating task. The task polls `player.isHandRaised()` — when the player is no longer raising the shield/item (i.e., they released right-click), `endHoldingRight()` is called, elapsed time is recorded, and `act(RIGHT_TAP)` or `act(RIGHT_HOLD)` is fired depending on hold duration. Sneaking uses the same pattern but without item swapping.

---

## Skill Slot Integration

Skill slots `UMBRAL_1`, `UMBRAL_2`, and `UMBRAL_3` are registered as **dynamic** nodes. When a node is marked dynamic, `resolveAction()` re-invokes the supplier on every call:

```java
new InputNodeBuilder(root, List.of(
    InputKey.of(InputType.SWAP),
    InputKey.of(InputType.LEFT),
    InputKey.of(InputType.LEFT)
)).action(() -> SkillSlotActionFactory.create(owner, SkillSlot.UMBRAL_1))
  .dynamic(true)
  .build();
```

`SkillSlotActionFactory.create()`:
1. Reads the `SkillId` from `player.getCombatProfile().getPlayerSkillContainer().getEquipped(slot)`.
2. Looks up the `Skill` in `SkillRegistry`.
3. If it is an `ActiveSkill`, builds an `InputAction` wiring `ActiveSkill.execute()`, `calculateCooldown()`, and `canPerform()` into the respective builder slots.
4. Returns null if no skill is equipped or the slot is locked — `step()` treats a null-action node as a valid traversal step with no execution.

Because these nodes are dynamic, `timeLastExecuted` does not persist on the returned `InputAction` — each call produces a fresh instance. Skill-level cooldown must be tracked on the `ActiveSkill` itself or a future cooldown registry.

---

## HUD Display

After each `step()` call, if the new node has children, the tree builds two `Component` objects:

- `baseSequenceToDisplay` — the sequence entered so far as symbols (`L`, `R`, `•`, `▁▁`, `D`, `F`, `S`).
- `potentialInputSelectionText` — the next available inputs, colored based on `InputActionExecutor.canCast()`.

`SwordPlayer.displayInputSequence()` renders both as a subtitle using `Title.times` matching the current node's timeout window. Nodes with `display = false` suppress this call.

---

## SwordAction (Legacy Shell)

`SwordAction` is now a near-empty abstract class holding static references to `Bukkit.getScheduler()` and `Sword.getInstance()`. All cast scheduling logic has been extracted to `ActionCaster`. Concrete action classes (`AttackAction`, `UmbralBladeAction`, `ThrowAction`, etc.) extend `SwordAction` to inherit these static fields, but the abstract class does not impose any meaningful contract.

---

## Issue #167 — What It Asks and Current Gaps

Issue #167 ("InputExecutionTree Upgrade") targets four areas. Current status of each:

### 1. Runtime-Resolvable Node Configurations

**Asked for:** Dynamic resolution of input nodes and actions at call time rather than compile time.

**Current state:** Partially implemented. The `dynamic` flag on `InputNode` and the `Supplier<InputAction>` pattern already support runtime resolution. Skill slot nodes use this today. The `addSequence(List<InputKey>, Consumer<InputNodeBuilder>)` method on `InputExecutionTree` allows runtime registration. However, all sequences are still registered statically in `initializeInputTree()` at construction time. There is no mechanism to add, remove, or swap sequences after the tree is built (beyond `addSequence` appending to the same root).

### 2. Context-Based Path Visibility

**Asked for:** Parallel trees or per-context path filtering so that different game states expose different action sets.

**Current state:** Infrastructure exists. `ActivationContext` enum has three values, `SwordPlayer.activationContext` is a settable field, and `InputNode.visibleIf` accepts a `Predicate<SwordPlayer>`. What is missing is: (a) no `initializeInputTree()` node currently attaches a `visibleIf` predicate keyed on `activationContext`, and (b) no game code currently changes `activationContext` in response to player state changes (stun, channeling, etc.). The issue comment mentions a preference for restricting predicates to leaf nodes only, as applying predicates to shared intermediate nodes breaks overlapping sequences.

### 3. Casting Logic Separation (Resolved)

**Asked for:** Extract casting from `SwordAction` into a separate class.

**Current state:** Complete. `ActionCaster` handles all cast scheduling. `SwordAction` is now an empty shell with a comment documenting the migration.

### 4. Builder Pattern for Node Registration (Resolved)

**Asked for:** Replace overloaded constructors with a builder pattern.

**Current state:** Complete. `InputNodeBuilder` is the standard registration mechanism used throughout `initializeInputTree()`. Direct `InputNode` construction is only used internally in `addChild()`.

### Remaining Gaps Summary

| Gap | Description |
|---|---|
| `ActivationContext` not wired | No nodes use `visibleIf` on context; no state transitions write `activationContext` |
| Dynamic constraint adoption | Most nodes still use `canCast(Predicate)` instead of `.constraints(...)` |
| Dynamic cooldown for skills | Skill `InputAction`s are recreated each call; `timeLastExecuted` is lost |
| `ACTIVE_1` / `ACTIVE_2` slots | No input sequences defined for active skill slots |
| Soulfire double-check | `act()` checks soulfire before `handlePerformAttempt`, but `handlePerformAttempt` also consumes it — the display path and consumption path are slightly misaligned |
| `SwordAction` extension | Concrete action classes still extend `SwordAction` for the scheduler reference; this coupling could be removed by making the references package-private constants |

---

## Extension Points

### Adding a New Combo

Register in `initializeInputTree()` using `InputNodeBuilder`:

```java
new InputNodeBuilder(root, List.of(
    InputKey.of(InputType.SWAP),
    InputKey.of(InputType.RIGHT)
)).action(() -> InputAction.builder()
        .action(executor -> MyAction.execute(executor))
        .cooldown(executor -> 1000)
        .canCast(Combatant::canPerformAction)
        .displayCooldown(true)
        .displayDisabled(true)
        .resetIfCannotPerform(true)
        .build())
    .timeoutTicks(10)
    .cancellable(true)
    .display(true)
    .build();
```

### Adding a Context-Gated Path

Use `.visibleIf()` on the leaf node only (per the issue #167 comment — applying predicates to intermediate nodes breaks shared prefixes):

```java
new InputNodeBuilder(root, List.of(...))
    .action(() -> InputAction.builder()...build())
    .visibleIf(p -> p.getActivationContext() == ActivationContext.NORMAL)
    .build();
```

### Adding a New ActionConstraint

Add a constant to `CommonConstraints` and reference it in the builder:

```java
public static final ActionConstraint MY_CONDITION =
    c -> /* test combatant state */;
```

```java
InputAction.builder()
    .action(...)
    .constraints(CommonConstraints.CAN_ACT, CommonConstraints.MY_CONDITION)
    .build()
```

### Adding a New Skill to the Input Tree

Register in `SkillIds` and `SkillRegistry`, implement `ActiveSkill`, then the existing dynamic skill-slot nodes will pick it up automatically when the player equips it — no changes to `initializeInputTree()` are needed for slots `UMBRAL_1` through `UMBRAL_3`.

---

## Known Limitations

- `InputNode.children` is a `LinkedHashMap`, so insertion order determines HUD display order and tie-breaking in `retrieveAvailableNode()`. There is no explicit priority mechanism — the first matching child wins.
- The `dynamic` flag disables cooldown persistence. Skills with per-player cooldowns need an external tracker.
- `SwordPlayer.act()` contains a pre-deduction soulfire check that is separate from the consumption inside `handlePerformAttempt`. If `handlePerformAttempt` fails after soulfire was already displayed as insufficient, no soulfire is actually consumed (correct behavior), but the logic is split across two call sites which makes it easy to diverge.
- `SHIFT` and `RIGHT` hold detection depends on `player.isHandRaised()` polling. This works as long as the player has a shield in offhand. A future context-aware solution is tracked in issue #5.
