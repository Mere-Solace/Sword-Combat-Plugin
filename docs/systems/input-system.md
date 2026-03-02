# Input System

## Overview

The input system translates raw Minecraft events (clicks, drops, swaps, sneaks) into a structured combo-based action execution pipeline. At its core is a trie-based `InputExecutionTree` per player, where sequences of `InputKey` steps traverse nodes to reach `InputAction` leaves.

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `InputType` | `system/input/InputType.java` | Enum: `LEFT`, `RIGHT`, `RIGHT_TAP`, `RIGHT_HOLD`, `DROP`, `SWAP`, `SHIFT`, `SHIFT_TAP`, `SHIFT_HOLD`. |
| `InputKey` | `system/input/InputKey.java` | Record combining `InputType` with a list of `SwordItemType` filters and an optional `Predicate<SwordPlayer>` accessibility check. |
| `SwordItemType` | `system/item/SwordItemType.java` | Enum classifying held items (e.g., `UMBRAL_LINK`, `UMBRAL_BLADE`, `GENERIC`). |
| `InputAction` | `system/input/InputAction.java` | Encapsulates an executable action with cooldown calculation, cast-check predicate, soulfire cost, cast duration, and display flags. Uses a builder pattern. |
| `InputActionExecutor` | `system/input/InputActionExecutor.java` | Static helper that validates then schedules/performs an `InputAction` via `ActionCaster`. |
| `InputExecutionTree` | `system/input/InputExecutionTree.java` | Per-player trie. Contains `InputNode` inner class and `InputNodeBuilder` for declarative combo registration. Manages timeouts and sequence display. |
| `InputListener` | `listeners/InputListener.java` | Bukkit event listener that captures `PrePlayerAttackEntityEvent`, `PlayerInteractEvent`, `PlayerDropItemEvent`, `PlayerToggleSneakEvent`, `PlayerSwapHandItemsEvent`, `PlayerItemHeldEvent` and routes them to `SwordPlayer.act()`. |
| `ActionCaster` | `system/action/ActionCaster.java` | Schedules an action on the main thread and locks the combatant's `abilityCastTask` for the cast duration. |

## Data Flow

```
Bukkit Event (InputListener)
  -> SwordPlayer.act(InputType)
    -> InputKey.of(input, SwordItemType)
    -> InputExecutionTree.step(inputKey)
      -> Traverse trie nodes, checking accessibility predicates
      -> If leaf reached: resolve InputAction via Supplier
    -> InputActionExecutor.execute(action, player)
      -> action.handlePerformAttempt() (cooldown + canCast checks)
      -> ActionCaster.cast() (schedule + lock)
        -> action.perform() (execute the Consumer<Combatant>)
```

## Hold Detection

Right-click and sneak inputs use a hold-detection mechanism in `SwordPlayer`:

1. On initial `RIGHT` or `SHIFT`, a tracking task starts recording hold duration.
2. When the player releases (stops blocking/sneaking), the elapsed time determines whether it was a **tap** (< 162ms) or **hold** (>= 162ms).
3. The corresponding `RIGHT_TAP`/`RIGHT_HOLD` or `SHIFT_TAP`/`SHIFT_HOLD` input is then routed to `act()`.
4. Hold inputs check `getMinHoldLengthOfNext()` to gate actions behind minimum hold durations.

## Combo Registration

Combos are declaratively built in `InputExecutionTree.initializeInputTree()` using `InputNodeBuilder`:

```java
new InputNodeBuilder(root, List.of(
    InputKey.of(InputType.SWAP),
    InputKey.of(InputType.SWAP)
)).action(() -> InputAction.builder()
    .action(executor -> MovementAction.dash(executor, DashDirection.FORWARD))
    .cooldown(executor -> executor.calcCooldown(AspectType.CELERITY, 200, 1000, 10))
    .canCast(Combatant::canAirDash)
    .build())
  .timeoutTicks(7)
  .cancellable(true)
  .display(true)
  .build();
```

The builder creates intermediate nodes as needed and places the action at the final (leaf) node.

## Currently Registered Combos

| Sequence | Action |
|----------|--------|
| F, F | Forward dash |
| F, F, L (umbral) | Shadow blink |
| F, F, L | Dash attack (forward) |
| S, S | Backward dash |
| S, S, L | Dash attack (backward) |
| S, L | Grab |
| L (umbral) x1-3 | Umbral blade basic combo |
| L x1-3 | Basic attack combo |
| D, R | Throw ready |
| D, R, R_HOLD | Throw item |
| D, D | Death (debug) |
| F, L, L | Umbral Skill 1 |
| F, L, R | Umbral Skill 2 |
| F, L, F | Umbral Skill 3 |
| S, F | Toggle umbral blade |
| S, D | Wield umbral blade |
| D, L | Heavy sweep |
| D, L, R | Heavy sweep (2nd) |
| D, L, R, L | Heavy sweep (3rd) |
| D, F, R | Lunge |

(L=Left, R=Right, D=Drop, F=Swap, S=Shift)

## Timeout and Reset

Each node has a `timeoutTicks` value (default 20). When a combo step is reached with children, a timeout timer starts. If the player does not provide the next input in time, the tree resets to root. The timeout is displayed to the player via the title system.

## Sequence Display

As the player progresses through a combo, `SwordPlayer.displayInputSequence()` shows a title with the current combo path and available next inputs. Ready inputs are shown in a bright color; unavailable ones are dimmed.

## Dependencies

- **SwordItemType / KeyRegistry** -- Item classification for input filtering
- **ActionCaster** -- Cast scheduling and lock management
- **TimeArbiter / SwordScheduler** -- Timeout timers
- **EntityAspects** -- Soulfire checks before execution

## Known Limitations

- `InputAction` uses `Supplier<InputAction>` in tree nodes, meaning a new `InputAction` instance is created on every resolve. This prevents the `timeLastExecuted` cooldown tracking from working across tree resets, since each resolve creates a fresh object with `timeLastExecuted = 0`.
- The hold detection threshold (162ms) is hardcoded rather than configurable.
- `handlePerformAttempt` casts the executor to `SwordPlayer` directly, which would fail for non-player combatants.
