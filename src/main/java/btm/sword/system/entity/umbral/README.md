# system/entity/umbral

This package implements the UmbralBlade: the signature weapon of Sword: Combat Evolved. The blade is an `ItemDisplay` entity that moves independently through the world, driven by a 13-state finite state machine. Every external change to blade behavior must go through `UmbralBlade.request(BladeRequest)`.

## Package Contents

| Class | Description |
|-------|-------------|
| `UmbralBlade` | Core class. Extends `ThrownItem`. Owns the `UmbralStateMachine`, `InputBuffer`, combo attack array, two item representations (link + blade), per-state display transformations, idle movement, and lunge trajectory. Thin data/physics holder — state-specific behavior lives in the state classes. |
| `UmbralSkill` | Empty stub. Not part of the FSM. No functionality implemented. |

## Sub-packages

### input

| Class | Description |
|-------|-------------|
| `BladeRequest` | Enum of all valid blade commands: `SHEATH`, `STANDBY`, `TOGGLE`, `WIELD`, `ATTACK_QUICK`, `ATTACK_HEAVY`, `LUNGE`, `GRAB_IMPALE`, `FINISHER`, `RECALL`, `ACTIVATE_TO_PREVIOUS`, `DEACTIVATE`, `RESUME_FROM_REPAIR`. |
| `InputBuffer` | FIFO queue of timestamped requests. Entries expire after 70 ms. `consumeIfPresent()` scans the full queue for the first matching entry, so any request type can be consumed regardless of ordering with unrelated requests in the buffer. |

### statemachine

| Class | Description |
|-------|-------------|
| `UmbralStateMachine` | Extends `StateMachine<UmbralBlade>`. Overrides `tick()` to skip the `onAnyTransition()` / `afterAnyTransition()` hooks (calls `onTransition` then goes directly to `setState`). Tracks `previousState`. Supports `PreviousState` sentinel. Calls `setDisplayTransformation()` on every `setState()`. Owns all FSM transitions via `initTransitions(UmbralBlade)` — called once from the `UmbralBlade` constructor. |
| `UmbralStateFacade` | Abstract supertype of all concrete states. Used as the `from` type in wildcard transitions. Contains no fields or methods beyond those inherited from `State<UmbralBlade>`. |
| `UmbralState` | Legacy enum. Unused by the FSM. Retained as reference — its values no longer match the live state classes. |

**Note on `UmbralStateMachine.tick()`:** The override does not call `super.tick()`. It reimplements the tick loop inline to support the `PreviousState` sentinel. This means `onAnyTransition()` and `afterAnyTransition()` from the base class are never called for the UmbralBlade FSM (unlike the Hostile AI FSM which uses the base `tick()`).

### statemachine/state

| State Class | Summary | Tasks Started in onEnter | Cancelled in onExit |
|-------------|---------|--------------------------|---------------------|
| `InactiveState` | Blade dormant. Entered when thrower is in spectator mode or on `DEACTIVATE`. | None | None |
| `SheathedState` | Blade on hip as passenger of thrower. Soul Link in slot 0. Initial state. | None (tick calls `TimeArbiter.teleportDisplay` + `addPassenger` synchronously each tick) | `endIdleMovement()` called |
| `StandbyState` | Blade hovers behind player with glow and idle oscillation. | `DisplayUtil.itemDisplayFollowLerp` directly (stored in `followTask`), `startIdleMovement()` | `followTask.cancel()`, `endIdleMovement()` |
| `WieldState` | Blade item placed in slot 0; display hidden (viewRange = 0). | None | Teleports display to thrower, restores viewRange, puts link back in slot 0 |
| `AttackingQuickState` | Private `attack()` helper inlined from former `UmbralBlade.performSimpleAttack`. Consumes soulfire per combo step. | Attack task (scheduled inside `attack()` → `Attack.execute()`) | `setAttackCompleted(false)`, clears glow — does NOT cancel any attack task |
| `AttackingHeavyState` | Private `attack()` helper inlined from former `UmbralBlade.performWideUmbralSweepAttack`. Dynamic Bezier curve toward targeted entity. | Attack task (scheduled inside `attack()` → `Attack.execute()`) | `setAttackCompleted(false)`, clears glow — does NOT cancel any attack task |
| `LungingState` | Blade thrown on Bezier trajectory via `onRelease()`. Transitions to `LodgedState` on hit or `RecallingState` on timeout. | Motion loop from `ThrownItem.onRelease()` (a `TimeArbiter` task) | Clears `finishedLunging`, glow, and calls `cleanupBeforeNewThrow()` to stop the motion loop |
| `GrabImpaleState` | Slerps display to a position above the grabbed entity, then launches a lunge. | `DisplayUtil.displaySlerpToOffset` task (stored in `slerpTask`), then a `SwordScheduler.runBukkitTaskLater` (stored in `attackTask`) that calls `attackEnemy()` which calls `onRelease()` | `setFinishedLunging(false)`, clears glow; cancels both `slerpTask` and `attackTask` |
| `LodgedState` | Blade fixed at impact location with glow. | None | `cleanupBeforeNewThrow()`, clears glow |
| `RecallingState` | Blade slerps back to player chest. Pushes `STANDBY` on arrival. | `DisplayUtil.displaySlerpToOffset` directly (stored in `returnTask`) | `returnTask.cancel()` if not already cancelled |
| `RecoverState` | Respawns the display entity when it is null or invalid. | `TimeArbiter.runTimeIndependentBukkitTaskOnTimer` (stored in `recoverTask`). **Stores `blade` as an instance field** — the known stateless-rule violation. | `recoverTask.cancel()` |
| `WaitingState` | Registers blade as interactable, starts idle movement. The `blade -> true` transition fires immediately on the next tick, so in practice this state is never observed for longer than one tick. | `startIdleMovement()` | `endIdleMovement()`, `unregisterAsInteractableItem()` |
| `FinisherState` | Stub only. All implementation commented out. `name()` returns `""`. | None | None |
| `PreviousState` | Sentinel. Never entered directly. `UmbralStateMachine.tick()` intercepts transitions targeting this class and restores `previousState` instead of instantiating it. | N/A | N/A |

## Lifecycle

`Combatant.handleUmbralBladeTick()` is called every entity tick. If `umbralBlade` is null, `setupUmbralBlade()` schedules construction 200 ms later to avoid spawning the display on the first tick. Every subsequent tick delegates to `umbralBlade.onTick()`, which calls `bladeStateMachine.tick()`.

Hostile entities override `setupUmbralBlade()` to immediately deactivate the blade after 250 ms, removing the display passenger. This preserves the Combatant contract without visual overhead.

On death or `dispose()`, `bladeStateMachine.setDeactivated(true)` prevents future ticks from executing. The display entity is removed via `ThrownItem.dispose()`. Running tasks in states (idle movement, follow tasks, return tasks) are not explicitly cancelled by `dispose()` — they rely on their own termination predicates (e.g., `display.isDead()`).

## Transition Table

All transitions are defined in `UmbralStateMachine.initTransitions()`, using `LinkedHashMap` insertion order for evaluation priority.

| From | To | Condition |
|------|----|-----------|
| Any (`UmbralStateFacade`) | `InactiveState` | Thrower is in spectator mode, or `DEACTIVATE` in buffer |
| Any (`UmbralStateFacade`) | `RecoverState` | `display == null`, dead, or invalid |
| `InactiveState` | `StandbyState` | `ACTIVATE_TO_PREVIOUS` in buffer |
| `RecoverState` | `StandbyState` | Display is valid, or `RESUME_FROM_REPAIR` in buffer |
| `SheathedState` | `StandbyState` | `TOGGLE` |
| `SheathedState` | `WieldState` | `WIELD` |
| `SheathedState` | `AttackingQuickState` | `ATTACK_QUICK` |
| `SheathedState` | `AttackingHeavyState` | `ATTACK_HEAVY` |
| `SheathedState` | `LungingState` | `LUNGE` |
| `StandbyState` | `SheathedState` | `TOGGLE` |
| `StandbyState` | `WieldState` | `WIELD` |
| `StandbyState` | `AttackingQuickState` | `ATTACK_QUICK` |
| `StandbyState` | `AttackingHeavyState` | `ATTACK_HEAVY` |
| `StandbyState` | `LungingState` | `LUNGE` |
| `StandbyState` | `GrabImpaleState` | `GRAB_IMPALE` |
| `StandbyState` | `FinisherState` | `FINISHER` |
| `FinisherState` | `RecallingState` | `skillFinished == true` or `STANDBY` |
| `WieldState` | `StandbyState` | `TOGGLE` |
| `AttackingQuickState` | `RecallingState` | `attackCompleted == true` |
| `AttackingHeavyState` | `RecallingState` | `attackCompleted == true` |
| `GrabImpaleState` | `LodgedState` | `hitEntity != null` |
| `GrabImpaleState` | `RecallingState` | `finishedLunging == true` |
| `WaitingState` | `StandbyState` | `blade -> true` (always fires) |
| `RecallingState` | `SheathedState` | `SHEATH` |
| `RecallingState` | `StandbyState` | `STANDBY` |
| `RecallingState` | `WieldState` | `WIELD` |
| `RecallingState` | `LungingState` | `LUNGE` |
| `RecallingState` | `AttackingQuickState` | `ATTACK_QUICK` |
| `RecallingState` | `AttackingHeavyState` | `ATTACK_HEAVY` |
| `LodgedState` | `RecallingState` | `hitEntity == null`, invalid, or `RECALL` |
| `LodgedState` | `WieldState` | `WIELD` |
| `LodgedState` | `StandbyState` | `STANDBY` |
| `LodgedState` | `AttackingHeavyState` | `ATTACK_HEAVY` |
| `LungingState` | `LodgedState` | `hitEntity != null` |
| `LungingState` | `RecallingState` | `finishedLunging == true` |

Dead transitions previously present (`LodgedState -> WaitingState` with `blade -> false` and `WaitingState -> RecallingState` with `isTooFarOrIdleTooLong`) have been removed.

## Request Flow

```
Player input
  -> InputExecutionTree
  -> UmbralBladeAction.someMethod()
  -> blade.request(BladeRequest.X)         // enqueues in InputBuffer
     each tick:
       UmbralStateMachine.tick()
         -> currentState.onTick()
         -> check each Transition (LinkedHashMap order):
              isRequestedAndActive(X) calls consumeIfPresent(X)
              if true: onTransition().accept(), setState(newState)
                  -> currentState.onExit()
                  -> currentState = newState (instantiated fresh)
                  -> currentState.onEnter()
                  -> setDisplayTransformation(newState.class) [50 ms delay via SwordScheduler]
              return immediately after first firing transition
```

## UmbralBlade / ThrownItem Inheritance

`UmbralBlade` extends `ThrownItem`, which provides:
- `ItemDisplay` setup and lifecycle (`display`, `displaySetupInstructions`, `setup()`)
- Bezier trajectory physics: `positionFunction`, `velocityFunction`, `origin/cur/prev/to/velocity`, `generateFunctions()`, `onRelease()`, `applyFunctions()`, `timeStep`, `timeScalingFactor`, `timeCutoff`
- Collision detection: `evaluate()`, `hitCheck()`, `groundedCheck()`, `hitEntity`, `grounded`, `hit`, `caught`
- Impalement: `impale()`, `thisImpalement`, `exitImpalementStatePredicate`
- Base `dispose()`, `onEnd()`, `onGrounded()`, `onHit()`, `onCatch()`
- `InteractiveItem` contract (via `onGrab()`, `disposeWithNewInteractiveItem()`)

`UmbralBlade` overrides: `groundedCheck()` (uses `prev` as ray origin, not `cur`), `generateFunctions()` (detours to `calcBezierTrajectory()` when `ctrlPointsForLunge` is set), `onGrounded()` (issues `RECALL` instead of embedding), `onEnd()` (sets `finishedLunging = true`), `teleport()` (uses `TimeArbiter.teleportDisplay` instead of the base method), `onCatch()` (no-op), `shouldShowLandingMarker()` (returns false), `handleItemDamageAndCheckIfBroken()` (no-op), `disposeWithNewInteractiveItem()` (issues `RECALL`), `dispose()` (also deactivates state machine).

Most of `ThrownItem`'s lifecycle (`onReady()`, `handleOnReleaseActions()`, item damage, landing marker) is either bypassed or no-oped by these overrides. `onRelease()` is the only `ThrownItem` path that `UmbralBlade` actively uses (called from `LungingState.onEnter()` and `GrabImpaleState.attackEnemy()`).

## Known Issues

### Attack task not cancelled on exit from `AttackingQuickState` / `AttackingHeavyState`
Attacks are launched by calling the private `attack()` helper inside `onEnter()`. These schedule time-bound `TimeArbiter` tasks internally. If the FSM exits these states before `attackCompleted` is set (e.g., via a wildcard `DEACTIVATE` or display-invalid transition), the attack task continues running independently. The `attackEndCallback` will eventually fire and set `attackCompleted = true` on the blade, which may then trigger an unexpected transition in whatever state the blade is in at that point.

### `RecoverState` violates the stateless rule
`RecoverState` stores `blade` as an instance field initialized in `onEnter()`. State instances are created fresh via reflection on every `setState()` call, so this field is never stale across instances. However, it is the only state class with instance data, and it relies on `onEnter()` being called before the `Runnable` lambda executes. The current design is safe but fragile.

## Extension

- **Add a new state:** create a class extending `UmbralStateFacade`, implement `onEnter()`, `onExit()`, `onTick()`. If the state starts any tasks, store their handle as instance fields and cancel them in `onExit()`. Register transitions in `UmbralStateMachine.initTransitions()`. Keep all state-specific behavior (following, attacking, returning) inside the state class itself — not in `UmbralBlade`.
- **Add a new request:** add a value to `BladeRequest` and a corresponding `Transition` in `UmbralStateMachine.initTransitions()`. Verify no existing transition for the same source state consumes or blocks it in `InputBuffer` ordering (now a full-queue scan, so ordering with other request types is no longer an issue).
- **Add a new attack variant:** add a `Function<Combatant, UmbralBladeAttack>` to the `basicAttacks` array in `loadBasicAttacks()`. The index is selected by `currentComboStep - 1`, clamped to array bounds.

For full architecture details see `docs/systems/umbral-blade.md`.
