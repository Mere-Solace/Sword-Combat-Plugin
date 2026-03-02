# Umbral Blade System

## Overview

The Umbral Blade is the signature weapon of Sword: Combat Evolved. Each `Combatant` owns exactly one `UmbralBlade` instance that is lazily initialized 200 ms after the entity first ticks. The blade is a physical `ItemDisplay` entity that moves independently through the world, driven by a 13-state FSM. All external code that wants to change blade behavior must do so by calling `UmbralBlade.request(BladeRequest)` — there is no direct state assignment from outside the blade.

---

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `UmbralBlade` | `system/entity/umbral/UmbralBlade.java` | Core class. Extends `ThrownItem`. Owns the state machine, `InputBuffer`, attack definitions, item representations (link + blade items), display transformations per state, lunge trajectory, and idle movement. Defines all state transitions in `initStateMachine()`. |
| `UmbralStateMachine` | `system/entity/umbral/statemachine/UmbralStateMachine.java` | Extends generic `StateMachine<UmbralBlade>`. Tracks `previousState`. Overrides `tick()` to support `PreviousState` sentinel and wildcard transitions via `UmbralStateFacade`. Overrides `setState()` to record the previous state and trigger display transformation updates. |
| `UmbralStateFacade` | `system/entity/umbral/statemachine/UmbralStateFacade.java` | Abstract supertype for all Umbral states. Used as the `from` class in wildcard transitions (`UmbralStateFacade.class.isAssignableFrom(anyConcreteState)` is always true). |
| `BladeRequest` | `system/entity/umbral/input/BladeRequest.java` | Enum of all valid blade commands: `SHEATH`, `STANDBY`, `TOGGLE`, `WIELD`, `ATTACK_QUICK`, `ATTACK_HEAVY`, `LUNGE`, `IMPALE`, `GRAB_IMPALE`, `FINISHER`, `RECALL`, `ACTIVATE_TO_PREVIOUS`, `ACTIVATE_AS_SHEATHED`, `DEACTIVATE`, `RESUME_FROM_REPAIR`, `REPAIR_DISPLAY`. |
| `InputBuffer` | `system/entity/umbral/input/InputBuffer.java` | FIFO queue of timestamped `BladeRequest` entries. Entries older than 70 ms are silently expired. Each `consumeIfPresent()` call only matches the head of the queue; unmatched heads block further consumption until they expire. |
| `PreviousState` | `system/entity/umbral/statemachine/state/PreviousState.java` | Sentinel class. Transitions that target `PreviousState.class` cause `UmbralStateMachine` to restore `previousState` instead of instantiating a new state. |

---

## Architecture

### Lifecycle in Combatant

`Combatant.handleUmbralBladeTick()` is called every entity tick from `Combatant.onTick()`. On the first tick where `umbralBlade == null` and the blade is not already starting, `setupUmbralBlade()` is called. This schedules blade construction 200 ms in the future on the main thread, creating a `UmbralBlade` with a `Material.STONE_SWORD` display item. The 200 ms delay avoids spawning the display entity during the entity's first tick before the world is ready.

Once constructed, every subsequent call to `handleUmbralBladeTick()` delegates to `umbralBlade.onTick()`, which calls `bladeStateMachine.tick()`.

On death or zero health, `umbralBlade.dispose()` is called. `dispose()` sets `bladeStateMachine.deactivated = true` (preventing further tick processing) and calls the parent `ThrownItem.dispose()`.

`Hostile.setupUmbralBlade()` overrides the base implementation to immediately deactivate the blade after 250 ms, removing the display passenger so no visible sword appears on mobs. This preserves the `Combatant` contract that every `Combatant` has an `umbralBlade` reference, without the visual or behavioral overhead. See `docs/systems/hostile-ai.md` for details.

### The Display Entity

`UmbralBlade` extends `ThrownItem`, which owns an `ItemDisplay` entity (`display`). During construction, the display is spawned at the thrower's eye location, configured with:

- `ItemStack` set to the thrower's equipped weapon item.
- A `Transformation` placing it at the hip (`Vector3f(0.28, -1.35, -0.5)`) with a Y-then-Z rotation that positions the blade along the player's side.
- `Billboard.FIXED` so it does not auto-rotate to face players.
- Added as a passenger of the thrower entity so it rides on the player in `SheathedState`.

When `UmbralStateMachine.setState()` fires, it calls `UmbralBlade.setDisplayTransformation(stateClass)`, which schedules a 50 ms delayed call that sets the display's interpolation duration to 2 ticks and applies the new `Transformation`. The per-state transformations are defined in `getStateDisplayTransformation()`:

| State | Transformation |
|-------|---------------|
| `SheathedState` | Hip position, Y+Z rotation (blade on back/hip) |
| `StandbyState` | Zero translation, Z-flip (blade hovers upright) |
| `RecallingState` | Zero translation, -X rotation (tip-forward returning) |
| `LungingState` | Zero translation, +X rotation (tip-forward lunging) |
| `GrabImpaleState` | Zero translation, -X rotation (same as recalling, ready to strike) |
| `LodgedState` | Retains current transformation (no change on lodge) |
| `AttackingQuickState` / `AttackingHeavyState` | Z-offset, +X rotation (horizontal sweep posture) |
| All others | Zero translation, Z-flip (default upright) |

### Idle Movement

In `StandbyState` and `WaitingState`, `startIdleMovement()` starts a periodic `TimeArbiter` timer firing every 150 ms. Each tick it sets a `Transformation` where the Y-translation oscillates as `cos(step) * 0.25f`. The step advances by `π/8` per tick, producing a slow float cycle. `endIdleMovement()` cancels the task.

### Item Representations

`UmbralBlade` generates two named items via `ItemStackBuilder`:

- **Link item** (`Material.HEAVY_CORE`) — The "Soul Link" item placed in inventory slot 0 when the blade is not wielded. Tagged with `KeyRegistry.SOUL_LINK_KEY` = thrower UUID. When the player holds this item, `holdingSoulLink()` returns true, enabling UmbralBlade-specific input actions.
- **Blade item** (same material as the weapon) — The wielded sword placed in slot 0 during `WieldState`. Tagged with `KeyRegistry.UMBRAL_BLADE_KEY` = thrower UUID and `WeaponAttackStyle.SLASH`. When held, `holdingUmbralBlade()` returns true.

---

## The InputBuffer

`InputBuffer` is an `ArrayDeque<TimestampedInput>` (FIFO). `push(BladeRequest)` enqueues a new entry timestamped with `System.currentTimeMillis()`.

`consumeIfPresent(BladeRequest request)` inspects the head of the queue:
1. Expired entries (older than 70 ms) are discarded in a loop until a live entry or empty state is reached.
2. If the head matches `request`, it is consumed (polled) and the method returns `true`.
3. If the head does not match `request`, the method returns `false` **without consuming the head**. The head blocks all further checks until it expires or is matched by a different transition.

This gives the buffer FIFO semantics with a strict ordering guarantee: no request can be consumed out of order, and each request can be consumed by at most one transition. The 70 ms window corresponds to approximately 1.4 server ticks.

`UmbralBlade.request(BladeRequest)` calls `inputBuffer.push()`. `isRequested(BladeRequest)` calls `consumeIfPresent()`. `isRequestedAndActive(BladeRequest)` adds the guard that the blade is not in `InactiveState`.

---

## The PreviousState Sentinel

`UmbralStateMachine` stores `previousState` as a `UmbralStateFacade` reference. `setState(State<UmbralBlade> next)` always assigns the current state to `previousState` before replacing it.

When a transition targets `PreviousState.class`, `tick()` detects this and calls `setState(previousState)` instead of `createState(PreviousState.class)`. This allows a transition to "go back" without knowing the specific prior state — currently used to restore from `InactiveState` via `BladeRequest.ACTIVATE_TO_PREVIOUS`.

`PreviousState` is a `final` class with no-op lifecycle methods. It should never be the actual current state; it only exists as a target class in transition records.

---

## State Diagram

```
                ┌───────────────────────────────┐
                │ FROM ANY STATE (wildcard)      │
                │ DEACTIVATE / spectator → INACTIVE
                │ display invalid → RECOVER      │
                └───────────────────────────────┘

 INACTIVE ──ACTIVATE_TO_PREVIOUS──► StandbyState (restores previous)
 RECOVER  ──display valid / RESUME_FROM_REPAIR──► StandbyState

                     ┌────────────────────────────────┐
                     │ SHEATHED                        │
                     └───┬──────┬────┬────┬────────────┘
              TOGGLE ◄───┘      │    │    │
                              WIELD │    │
                      ATTACK_QUICK  │    │
                      ATTACK_HEAVY  │    │
                      LUNGE ────────┘    │
                                         │
                     ┌───────────────────▼──────────────┐
                     │ STANDBY                           │
                     └──┬──┬──┬──┬──┬──┬────────────────┘
              TOGGLE ◄──┘  │  │  │  │  │
                         WIELD │  │  │  │
                 ATTACK_QUICK  │  │  │  │
                 ATTACK_HEAVY  │  │  │  │
                 LUNGE ────────┘  │  │  │
                 GRAB_IMPALE ─────┘  │  │
                 FINISHER ───────────┘  │
                                        │  (TOGGLE)
                     ┌──────────────────▼──────┐
                     │ WIELD                   │
                     └─────────────────────────┘
                                        │ (attack completes)
┌───────────────────┐  ┌────────────────┴──────────────────┐
│ ATTACKING_QUICK   │  │ ATTACKING_HEAVY                    │
│ onEnter: performSimpleAttack()     onEnter: performWideUmbralSweepAttack()
│ attackCompleted=true → RECALLING   attackCompleted=true → RECALLING
└───────────────────┘  └──────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ LUNGING                                                 │
│ onEnter: sets ctrlPointsForLunge, calls onRelease()    │
│  hitEntity != null → LODGED                            │
│  finishedLunging   → RECALLING                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ GRAB_IMPALE                                             │
│ onEnter: slerp blade to position above grabbed target, │
│          then calls onRelease() (lunge at grabbed entity)│
│  hitEntity != null → LODGED                            │
│  finishedLunging   → RECALLING                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ LODGED                                                  │
│  entity invalid / RECALL → RECALLING                   │
│  WIELD   → WIELD                                       │
│  STANDBY → STANDBY                                     │
│  ATTACK_HEAVY → ATTACKING_HEAVY                        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ RECALLING                                               │
│ onEnter: returnToWielderAndRequestState(STANDBY)       │
│  SHEATH        → SHEATHED                              │
│  STANDBY       → STANDBY                              │
│  LUNGE         → LUNGING                              │
│  ATTACK_QUICK  → ATTACKING_QUICK                       │
│  ATTACK_HEAVY  → ATTACKING_HEAVY                       │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ WAITING                                                 │
│ condition = true (immediate) → STANDBY                 │
│ isTooFarOrIdleTooLong()      → RECALLING               │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ RECOVER                                                 │
│ onEnter: periodic task to remove and respawn display   │
│ display valid / RESUME_FROM_REPAIR → STANDBY           │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ FINISHER (stub — body commented out)                   │
│ skillFinished / STANDBY → RECALLING                    │
└─────────────────────────────────────────────────────────┘
```

---

## BladeRequest Reference

| Request | Typical Source | Effect |
|---------|---------------|--------|
| `SHEATH` | `InputExecutionTree` (TOGGLE) | From Recalling: go to Sheathed |
| `STANDBY` | Multiple states | Return to Standby hover |
| `TOGGLE` | Drop + Swap input | Toggle between Sheathed and Standby |
| `WIELD` | Swap + Left Click | Place blade item in hand |
| `ATTACK_QUICK` | Left Click (Standby/Sheathed), DashAttackAction | Begin quick Bezier sweep |
| `ATTACK_HEAVY` | Swap + Left Click (Standby/Sheathed/Lodged) | Begin wide sweep |
| `LUNGE` | Drop + Left Click | Throw blade on Bezier lunge trajectory |
| `IMPALE` | `ImpalingUmbralBladeAttack` on hit | Stop iteration, lock blade to target |
| `GRAB_IMPALE` | `Combatant.onGrabHit()` | Slerp to grabbed entity and lunge |
| `FINISHER` | Skill system | Begin finisher skill sequence |
| `RECALL` | `onGrounded()`, `Combatant`, `LodgedState` exit | Begin return journey |
| `ACTIVATE_TO_PREVIOUS` | Activation after Inactive | Restore previous state |
| `ACTIVATE_AS_SHEATHED` | Activation | Activate in Sheathed state |
| `DEACTIVATE` | Spectator mode | Enter Inactive |
| `RESUME_FROM_REPAIR` | After display reset | Exit Recover |
| `REPAIR_DISPLAY` | Internal (not wired to transition) | Trigger display repair |

---

## All 13 States

### InactiveState
The blade is fully dormant. No tick processing runs (`UmbralStateMachine.deactivated` is independent; `InactiveState.onTick()` does nothing). Entered when the thrower enters spectator mode or `DEACTIVATE` is requested. Exited to `StandbyState` on `ACTIVATE_TO_PREVIOUS`.

### SheathedState
The blade rides on the player's hip as a display entity passenger. Each tick, `updateSheathedPosition()` re-teleports and re-adds the display as a passenger to counteract any displacement. The Soul Link item is placed in slot 0. On exit, the blade registers in `InteractiveItemArbiter` for pick-up detection. Initial FSM state.

### StandbyState
The blade hovers behind and above the player's right shoulder (offset `0.7, 0.7, -0.5` via `hoverBehindWielder()`). Glow is enabled. Idle movement starts. A "Ready." subtitle is displayed to the player. The follow task is cancelled on exit; glow and idle movement are disabled.

### WieldState
The display's view range is set to 0 (invisible to all) and the blade item is placed in slot 0. On exit, the display is teleported back to the player and view range is restored.

### AttackingQuickState
On entry: consumes `currentComboStep * 2.5f` soulfire. If `comboStep == 3`, the current display rotation is modified by a Y-axis `π/2` rotation before the attack. Calls `blade.performSimpleAttack(5.0)`. Glow enabled. Transitions to `RecallingState` when `attackCompleted` becomes true (set by `attackEndCallback`). `attackCompleted` is reset to `false` on exit.

### AttackingHeavyState
On entry: calls `blade.performWideUmbralSweepAttack(8)`. Glow is set to `FEROCIOUS_SWEEP` color. Same `attackCompleted` -> Recalling transition as AttackingQuick. Attack details are below under "Heavy Sweep Construction".

### LungingState
On entry: clears `hitEntity` and `finishedLunging`, sets `timeCutoff` and `timeScalingFactor` from Config, sets `ctrlPointsForLunge` to `AttackType.LUNGE1.controlVectors()`, then calls `onRelease(LUNGE_ON_RELEASE_VELOCITY)`. This triggers `ThrownItem`'s throw logic, which eventually calls `calcBezierTrajectory()` to orient the lunge toward the targeted entity or look direction.

Transitions:
- `hitEntity != null` (populated by `ImpalingUmbralBladeAttack`) → `LodgedState`
- `finishedLunging` (set in `onEnd()` after trajectory completes) → `RecallingState`

On exit: `finishedLunging` is reset to false and glow is disabled.

### GrabImpaleState
On entry: slerps the display to a position above the grabbed entity (offset `-1, eyeHeight*6, -1`). After slerp completes (or after 200 ms delay), calls `attackEnemy()`, which sets lunge parameters identically to `LungingState` and calls `onRelease()`. The lunge targets the grabbed entity. Transitions to `LodgedState` on hit, `RecallingState` on miss.

### LodgedState
On entry: glow enabled. The blade remains at its current position with its current transformation (no change applied). The transition to `RecallingState` fires when `hitEntity == null` (target died/invalidated) or `BladeRequest.RECALL` is received. On transition, a `teleportDisplay` sends the blade backward 6 blocks along its current facing direction, and the hit entity receives a recoil velocity of `-0.75` along that direction. `cleanupBeforeNewThrow()` is called on exit.

### RecallingState
On entry: calls `returnToWielderAndRequestState(BladeRequest.STANDBY)`, which starts a slerp interpolation back to the player's chest offset. When the blade arrives (or when it has been stationary for 4 consecutive iterations of `< EPSILON_SQUARED` movement), `BladeRequest.STANDBY` is pushed to the buffer, triggering the `Recalling -> Standby` transition. The slerp task is cancelled on exit if still running.

### RecoverState
Entered when the display entity is null or invalid. On entry, starts a timer that attempts to remove the old display and call `resetWeaponDisplay()` (which spawns a fresh `ItemDisplay` at the thrower's eye). Exits when the display is valid or `RESUME_FROM_REPAIR` is received.

### WaitingState
Registers the blade in `InteractiveItemArbiter` and starts idle movement. The `WaitingState -> StandbyState` transition condition is hardcoded to `true`, meaning the state is transitioned through immediately on the next tick. The `WaitingState -> RecallingState` condition (`isTooFarOrIdleTooLong()`) is evaluated but effectively never reached before the `true` transition fires. The state is architecturally present for future use.

### FinisherState
Declared but all behavioral logic is commented out. On entry and exit, nothing happens. Transitions to `RecallingState` when `skillFinished == true` or `BladeRequest.STANDBY` is received.

### PreviousState
Sentinel only. Never the active state. See "PreviousState Sentinel" section above.

---

## Lunge Mechanics

When entering `LungingState` or `GrabImpaleState`, the blade is "released" via `ThrownItem.onRelease(velocity)`. `UmbralBlade.generateFunctions()` detects `ctrlPointsForLunge != null` and calls `calcBezierTrajectory()` instead of the default ballistic calculation.

`calcBezierTrajectory()`:
1. Determines the target: the grabbed entity (GrabImpale) or the closest targeted entity within 20 blocks (Lunge). If no target, uses the point 20 blocks along the player's eye direction.
2. Computes `dir` as the vector from the display's current location to the target's chest location.
3. Builds a `Basis` from the display's location oriented toward `dir`.
4. Calls `ctrlPointsForLunge.adjustToBasis(basis, 1)` to rotate the `LUNGE1` curve to face the target.
5. Sets `positionFunction = BezierUtil.cubicBezier3D(adjusted)` and `velocityFunction = t -> dir * 0.5`.

`LUNGE1` is defined as:
```
start:  (0.37, 0,   2)
c1:     (0,    0,   20)
c2:     (1.1,  0,   3.1)
end:    (0,    0,   2.46)
```
The large `c1.z = 20` creates a strong forward bias, producing a nearly straight stab trajectory with a slight curve. The `timeScalingFactor` (from Config) accelerates the blade's parametric time, and `timeCutoff` terminates the trajectory early if the target is close.

`ThrownItem` detects hits via `throwingHitCheck()` (called from `ImpalingUmbralBladeAttack` during the attack iteration). When a hit is recorded, `blade.hitEntity` is set and the FSM transitions to `LodgedState`.

---

## Attack Dispatch from State

Blade attack states initiate attacks differently from the normal `AttackAction` path:

**Quick attack** (`AttackingQuickState.onEnter()`):
- Calls `blade.performSimpleAttack(5.0)`.
- If `blade.isDashing()`, routes to `umbralDashAttack(dashDirection)` which creates a `UmbralBladeAttack` with an `F_DASH_ATTACK`, `B_DASH_ATTACK`, or `WIDE_UMBRAL_SLASH3` profile.
- Otherwise, reads `currentComboStep` (1, 2, or 3) and selects from the `basicAttacks` array (three elements, each a `Function<Combatant, UmbralBladeAttack>`). The step is clamped to `[0, basicAttacks.length - 1]`.
- Each `basicAttack` entry is a two-phase attack: a windup pass (display-only, `displayOnly = true`) followed by the actual strike pass (via `setNextAttack`).

**Heavy attack** (`AttackingHeavyState.onEnter()`):
- Calls `blade.performWideUmbralSweepAttack(8)`.
- Finds the targeted entity within 8 blocks. If none, targets 8 blocks in the facing direction.
- Dynamically builds a `GeneratedAttackProfile` with `ControlVectors` computed from the distance and a directional offset:
  - `start` = slightly behind and to the side of the attack origin, scaled by distance.
  - `c1`, `c2` = forward-offset positions relative to origin and target, scaled by distance.
  - `end` = target location plus a forward offset to the opposite side, producing a sweeping arc.
- Duration scales as `20 * log(max(1, dist²))` ms.
- A two-phase `UmbralBladeAttack` chain: a display-only phase with no particles moves the blade to position; the strike phase performs hit detection.

---

## Impalement System

`SwordEntity` tracks active impalements in `Set<Impalement> impalements`. `addImpalement(Impalement)` and `removeImpalement(Impalement)` manage the set. `isImpaled()` returns `!impalements.isEmpty()`.

`Hostile.onImpaledByBlade(UmbralBlade blade)` is a stub hook (TODO #189) called when an Umbral Blade lodges into a hostile entity. Currently a no-op, but is the intended integration point for future mob reactions to being lodged.

---

## Interactions with Other Systems

- **Combatant** — Owns the lifecycle: `setupUmbralBlade()`, `handleUmbralBladeTick()`, `endUmbralBlade()`, `requestUmbralBladeState()`. Exposes `holdingSoulLink()`, `holdingUmbralBlade()`, and `canPerformUmbralAction()` (returns true only from `Standby`, `Recalling`, `Lodged`, or `Sheathed`).
- **InputExecutionTree** — Dispatches `BladeRequest` values via `UmbralBladeAction` static methods (e.g., `TOGGLE`, `WIELD`, `ATTACK_QUICK`, `ATTACK_HEAVY`, `LUNGE`, `GRAB_IMPALE`, `FINISHER`). Also calls `DashAttackAction.dashAttack()` which can issue `ATTACK_QUICK` when the Soul Link is held.
- **Attack system** — `UmbralBladeAttack` and `ImpalingUmbralBladeAttack` execute the actual sweep geometry. The `attackEndCallback` (`() -> attackCompleted = true`) is passed into each attack chain so the FSM knows when to transition away from attacking states.
- **ThrownItem** — Provides trajectory computation, `onRelease()`, ground detection, and display entity management. `UmbralBlade` overrides `generateFunctions()` to inject the Bezier lunge trajectory, `groundedCheck()` for custom throw detection, `onGrounded()` to issue `RECALL`, and `onEnd()` to set `finishedLunging = true`.
- **InteractiveItemArbiter** — The blade registers itself during `SheathedState.onExit()` and `WaitingState.onEnter()` and deregisters during `SheathedState.onEnter()` and `WaitingState.onExit()`. When the player physically interacts with the registered display entity, `onGrab()` is called, which issues `WIELD` or `STANDBY` depending on what item is in hand.
- **TimeArbiter / SwordScheduler** — Used for idle movement, display transformation delays, slerp return animation, and attack callbacks. Never calls `Bukkit.getScheduler()` directly.
- **Config** — `Config.UmbralBlade.LUNGE_TIME_CUTOFF`, `LUNGE_TIME_SCALING_FACTOR`, `LUNGE_ON_RELEASE_VELOCITY` control lunge behavior. Visual constants (`SwordColor.UMBRAL_GLOW`, etc.) control glow overrides.

---

## Known Limitations

- `StateMachine` uses `HashMap` with no ordering guarantee. If multiple transitions from the same source state have true conditions simultaneously, the one iterated first wins non-deterministically. This is most relevant in `LodgedState`, which has four outgoing transitions.
- `WaitingState -> StandbyState` has `blade -> true` as its condition, so the state is exited immediately on the next tick. The `WaitingState -> RecallingState` distance/idle check is evaluated but cannot fire before the always-true transition.
- `FinisherState` has all logic commented out. The state is a placeholder with no active behavior.
- State instantiation uses reflection (`clazz.getDeclaredConstructor().newInstance()`), preventing state classes from having constructor arguments. All per-activation state must be stored on the `UmbralBlade` context.
- `RecoverState` stores a `blade` field on the instance, which technically violates the documented constraint that states must be stateless. This is a known deviation specific to the recovery mechanism.
- `isTooFarOrIdleTooLong()` in `WaitingState` is never reached due to the `true` transition condition above it.
