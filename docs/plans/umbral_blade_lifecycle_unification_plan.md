# Umbral Blade Lifecycle Unification Plan

## Purpose

This plan unifies Umbral Blade lifecycle ownership so the system becomes easier to reason about, safer under join/respawn/gamemode changes, and much easier to test.

The immediate goal is to answer one question in exactly one place:

> Should this combatant currently have a live Umbral Blade instance in the world?

Right now that answer is scattered across listeners, `Combatant`, `AnimationMode`, `InactiveState`, `RecoverState`, and ad hoc display cleanup. That is the main source of the fragility.

---

## Design Decisions

### 1. `Combatant` becomes the single lifecycle owner

`Combatant` should own the entire blade lifecycle:

- whether the blade is allowed to exist
- whether the blade should currently be active
- whether the blade system is ready to be started yet
- creating a fresh blade instance
- fully shutting down and disposing the current blade instance

No listener, menu, dev mode, or state class should directly decide to remove or respawn the blade display.

Those callers should only express intent through clean `Combatant` methods.

### 2. The blade should start deactivated

On player construction and early join staging, the blade should begin in a fully inactive lifecycle state.

It should only be activated once the player is actually ready for normal gameplay:

- load-in / join routing is complete
- the player is not in a special suppressed mode
- the combatant is allowed to own a blade

This removes the current "constantly poll until maybe it should exist" behavior and replaces it with explicit lifecycle transitions.

### 3. `InactiveState` should stay, but with a much narrower job

`InactiveState` can remain as an FSM state, but it should no longer be a substitute for full lifecycle destruction.

Recommended contract:

- lifecycle layer decides whether the blade instance exists at all
- if the blade instance exists but is intentionally dormant, FSM may sit in `InactiveState`
- `InactiveState` may suppress almost all normal transition work
- `InactiveState.onTick()` may lazily check whether the combatant's active flag says it should wake back up

Important constraint:

- `InactiveState` must not call `blade.dispose()`
- entering `InactiveState` must not permanently strand the FSM

If we want "off but recoverable," that is an FSM concern.
If we want "gone and fully destroyed," that is a lifecycle concern owned by `Combatant`.

### 4. Full destruction and recoverable dormancy must be separate concepts

We should explicitly model these as different operations:

- **Deactivate/Destroy**: fully dispose the blade instance, cancel tasks, remove display, null the combatant reference
- **Activate/Start**: create a fresh blade instance and prepare it for a known ready state
- **Suspend**: optional, keep an existing instance but move FSM into `InactiveState`
- **Resume**: optional, wake a suspended instance back into its ready state

Today these ideas are mixed together and that is what makes the system hard to follow.

### 5. `UmbralBlade` should eventually stop extending `ThrownItem`

Architecturally, the blade does not naturally fit `ThrownItem`.

Reasons:

- the blade is a persistent weapon system, not a disposable projectile
- it owns a long-lived FSM and inventory identity
- it has its own standby/wield/sheathed lifecycle that is unrelated to normal thrown-item semantics
- several `ThrownItem` behaviors are overridden, bypassed, or treated as special cases already

Recommendation:

- **Phase 1**: keep the inheritance temporarily while lifecycle ownership is stabilized
- **Phase 2**: extract the useful projectile/trajectory pieces into a reusable helper or component and remove `ThrownItem` inheritance from `UmbralBlade`

This avoids trying to do both a lifecycle rewrite and a deep movement/physics refactor at the same time.

---

## Target Architecture

### `Combatant` lifecycle API

`Combatant` should expose a small, explicit API for the blade:

- `setUmbralBladeAllowed(boolean allowed)`
- `setUmbralBladeActive(boolean active)`
- `markUmbralBladeSystemReady()`
- `markUmbralBladeSystemNotReady()`
- `startUmbralBlade(ReadyState initialState)`
- `deactivateUmbralBladeCompletely()`
- `restartUmbralBlade(ReadyState initialState)`
- `hasActiveUmbralBlade()`

`ReadyState` should be an explicit enum owned by the blade lifecycle, not inferred from whatever state happened to run last:

- `SHEATHED`
- `STANDBY`

This is cleaner than "activate to previous state," because it makes activation deterministic.

### Lifecycle flags on `Combatant`

Recommended flags:

- `umbralBladeAllowed`
- `umbralBladeActiveRequested`
- `umbralBladeSystemReady`
- `umbralBladeStarting`

Interpretation:

- `allowed`: permanent capability gate
- `activeRequested`: whether gameplay/dev state currently wants the blade on
- `systemReady`: whether it is safe to spawn after join/load/menu setup
- `starting`: guards against duplicate startups

### Lifecycle rule

Each combatant tick should follow one clear rule:

1. If the system is not ready, do not spawn the blade.
2. If the blade is not allowed, ensure it is fully destroyed.
3. If active is not requested, ensure it is fully destroyed or suspended, depending on the caller's intent.
4. If active is requested and no blade exists, start one in a known ready state.
5. If a blade exists, tick it.

This replaces the current loose "if null maybe schedule setup" behavior.

---

## Concrete Behavior Changes

### A. Remove implicit delayed setup as the primary startup model

Current behavior:

- `handleUmbralBladeTick()` notices null
- schedules delayed creation
- external systems independently mutate blade requests and display state

Target behavior:

- creation only happens through `startUmbralBlade(...)`
- if a small delayed spawn is still needed for visuals, that delay lives inside `startUmbralBlade(...)`
- the lifecycle layer owns that delay and its guard conditions

The system should no longer "constantly check whether the blade is alive or not, based on some boolean value changed somewhere."

### B. Remove `ACTIVATE_TO_PREVIOUS`

This request is a symptom of unclear lifecycle ownership.

A reactivation path should always say exactly what it wants:

- re-create to `SHEATHED`
- re-create to `STANDBY`

This is more readable, more deterministic, and safer after disposal.

### C. Stop listener-driven display mutation

`PlayerListener.gameChangeEvent()` should not:

- remove the blade display directly
- push ad hoc activation requests

Instead it should call explicit lifecycle intent methods on the player:

- spectator enter -> `setUmbralBladeActive(false)`
- spectator exit -> `setUmbralBladeActive(true)`

If the architecture team wants hard destruction instead of suspension for spectator, that should still flow through the `Combatant` lifecycle API rather than direct display manipulation.

### D. Join/load should explicitly arm the blade

The player should spawn with the blade system not ready.

Once join staging and routing are complete:

- mark the blade system ready
- request activation
- start in `SHEATHED` or `STANDBY` depending on desired UX

That gives a clean, testable boot sequence.

### E. Animation/dev systems should talk to lifecycle, not raw FSM requests

For example:

- entering animation mode: `setUmbralBladeActive(false)` or suspend explicitly
- leaving animation mode: `startUmbralBlade(STANDBY)` or `setUmbralBladeActive(true)`

No dev tool should need to know about `DEACTIVATE` or `ACTIVATE_TO_PREVIOUS`.

---

## Proposed `InactiveState` Contract

Keep `InactiveState`, but redefine it carefully.

### Responsibilities

- represent a dormant but recoverable FSM mode
- avoid normal combat transitions
- avoid motion/follow/attack tasks
- optionally hide or null out nonessential visual behavior
- lazily check whether the owning combatant now wants the blade active again

### Non-responsibilities

- should not destroy the blade instance
- should not null the combatant reference
- should not be the primary join/spectator cleanup path if we want true destruction

### Wake-up rule

`InactiveState.onTick()` may read a simple combatant-owned flag:

- if `thrower.isUmbralBladeActiveRequested()` and the instance is valid, transition to a known ready state

That would support a suspend/resume flow cleanly.

### Open question for review

There are two valid variants:

1. **Hard-destroy model**
   Spectator/dev suppression fully destroys the blade instance. `InactiveState` is reserved for narrow internal uses only.

2. **Suspend/resume model**
   Spectator/dev suppression moves the blade into `InactiveState`, keeping the instance alive for fast wake-up.

Recommendation:

- use **hard-destroy** for join, leave, death, and any reliability-critical cleanup
- allow **suspend/resume** only if there is a concrete UX or dev-tool benefit and its semantics remain simple

This gives us the safest core while still leaving room for a recoverable inactive mode.

---

## Should `UmbralBlade` Still Extend `ThrownItem`?

### Short answer

Not long-term.

### Why it currently feels wrong

`ThrownItem` assumes a mostly projectile-shaped lifecycle:

- setup
- ready
- release
- impact/catch/end
- dispose

`UmbralBlade` is instead:

- a persistent equipment system
- a combat-state machine
- a display identity tied to inventory state
- sometimes projectile-like during only a few states

That mismatch causes a lot of override-heavy behavior and makes disposal/recovery semantics muddy.

### Migration strategy

Do not remove inheritance in the same pass as lifecycle cleanup.

Instead:

1. stabilize lifecycle ownership first
2. identify the reusable projectile functions the blade actually needs
3. extract those into a shared helper/component
4. migrate `UmbralBlade` to composition

Likely extraction candidates:

- trajectory generation
- `initFlight()` / `stepFlight()`
- hit filtering helpers
- grounded/hit/catch termination plumbing
- display projectile teleport helpers

---

## Dev Testing Tool

To support fast verification, add a dedicated dev item:

- item: `BREEZE_ROD`
- source: obtainable from `DevMenu`
- purpose: direct Umbral Blade lifecycle testing

### Desired behavior

When held by a `DevSwordPlayer`:

- `LEFT` -> completely deactivate and destroy the blade
- `DROP` -> completely reactivate the blade into ready state

Recommended ready state on reactivation:

- `STANDBY`

That makes the result visually obvious and removes ambiguity about whether the blade came back correctly.

### Integration point

Add a new item in `DevMenu` that gives the player the testing rod.

Suggested name:

- `Umbral Blade Tester`

Suggested lore:

- `Left-click: fully destroy blade`
- `Drop: fully recreate blade in standby`

### Input routing

The tester should use the existing dev-only input path, not generic gameplay input:

- detect the rod by a dedicated item key
- route it through `DevSwordPlayer.act(...)` or a small dedicated dev action helper

This keeps the tool isolated from normal combat inputs.

### Why this tool matters

It gives the team a deterministic way to test:

- full shutdown
- clean recreation
- whether tasks leak after destruction
- whether the ready-state startup path is stable

---

## Implementation Plan

### Phase 1. Establish clean lifecycle API

Files:

- `Combatant`
- `SwordPlayer`
- `DevSwordPlayer`
- join/load flow classes

Changes:

- add explicit lifecycle flags to `Combatant`
- add clean start/stop/restart methods
- move all blade creation into `startUmbralBlade(...)`
- ensure full destruction always nulls the reference
- make startup target state explicit (`SHEATHED` or `STANDBY`)

### Phase 2. Rewire external callers to lifecycle API

Files:

- `PlayerListener`
- `AnimationMode`
- any dev/menu/special-mode classes touching blade requests directly

Changes:

- replace direct `BladeRequest.DEACTIVATE` / `ACTIVATE_TO_PREVIOUS` usage
- remove manual display removal
- express intent only through `Combatant` lifecycle methods

### Phase 3. Redefine `InactiveState`

Files:

- `InactiveState`
- `UmbralStateMachine`
- `BladeRequest`

Changes:

- remove recursive self-disposal behavior from `InactiveState`
- decide whether `InactiveState` is suspend/resume only or nearly unused
- remove `ACTIVATE_TO_PREVIOUS`
- if needed, add explicit wake-to-ready transition logic

### Phase 4. Simplify startup and recovery

Files:

- `Combatant`
- `UmbralBlade`
- `RecoverState`

Changes:

- replace passive null polling with explicit start requests
- make recovery one-shot and deterministic
- ensure startup and repair both land in known ready states

### Phase 5. Add the dev testing rod

Files:

- `DevMenu`
- dev input routing classes
- possibly `KeyRegistry`
- a new small dev action helper

Changes:

- add `BREEZE_ROD` dev item
- left click destroys blade
- drop recreates blade in standby
- keep behavior dev-only

### Phase 6. Remove `ThrownItem` inheritance in a follow-up refactor

Files:

- `UmbralBlade`
- `ThrownItem`
- new shared projectile helper/component

Changes:

- extract reusable projectile logic
- move blade to composition
- preserve behavior while reducing inheritance mismatch

---

## Verification Matrix

After implementation, verify all of the following manually:

1. Join during load-in -> no blade before system-ready moment.
2. Join complete -> blade starts exactly once in the chosen ready state.
3. Breeze rod left click -> blade fully disappears, no jitter, no ghost.
4. Breeze rod drop -> blade recreates exactly once in standby.
5. Repeat destroy/reactivate rapidly -> no duplicate displays, no stale tasks.
6. `/kill` or normal death -> blade fully destroyed and recreates cleanly after respawn readiness.
7. Spectator enter -> blade follows the selected suppression policy cleanly.
8. Spectator exit -> blade returns through the lifecycle API, not stale FSM state.
9. Animation mode enter/exit -> no direct display manipulation, no orphaned blade.
10. Leave and rejoin -> never see old blade jittering beside new blade.
11. Kill display entities externally -> repair path produces one clean blade only.

---

## Recommended Review Decisions

The architecture review should explicitly approve or reject these points:

1. `Combatant` is the sole Umbral Blade lifecycle owner.
2. Blade startup must be explicit and readiness-gated.
3. `ACTIVATE_TO_PREVIOUS` should be removed.
4. `InactiveState` remains only as a recoverable dormant state, not a destroy path.
5. Spectator/dev suppression should default to full destruction unless there is a strong reason to keep suspend/resume.
6. `UmbralBlade` should be migrated away from `ThrownItem` in a second refactor phase, not the first.
7. A dev-only `BREEZE_ROD` tester should be added immediately to validate the new lifecycle.

---

## Summary

The key simplification is this:

- the FSM decides what the blade is doing
- the lifecycle layer decides whether the blade exists at all

Once that separation is enforced, the rest of the cleanup becomes much easier:

- startup becomes explicit
- deactivation becomes explicit
- reactivation becomes deterministic
- dev testing becomes trivial
- `InactiveState` becomes understandable
- and the long-term removal of `ThrownItem` inheritance becomes much safer
