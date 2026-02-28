# system/entity/umbral

This package implements the UmbralBlade: the signature weapon of Sword: Combat Evolved. The blade is an `ItemDisplay` entity that moves independently through the world, driven by a 13-state finite state machine. Every external change to blade behavior must go through `UmbralBlade.request(BladeRequest)`.

## Package Contents

| Class | Description |
|-------|-------------|
| `UmbralBlade` | Core class. Extends `ThrownItem`. Owns the `UmbralStateMachine`, `InputBuffer`, attack function array, two item representations (link + blade), per-state display transformations, idle movement, and lunge trajectory. Defines all FSM transitions in `initStateMachine()`. |
| `UmbralSkill` | (Separate skill wrapper, not part of the FSM.) |

## Sub-packages

### input

| Class | Description |
|-------|-------------|
| `BladeRequest` | Enum of all valid blade commands: `SHEATH`, `STANDBY`, `TOGGLE`, `WIELD`, `ATTACK_QUICK`, `ATTACK_HEAVY`, `LUNGE`, `IMPALE`, `GRAB_IMPALE`, `FINISHER`, `RECALL`, `ACTIVATE_TO_PREVIOUS`, `ACTIVATE_AS_SHEATHED`, `DEACTIVATE`, `RESUME_FROM_REPAIR`, `REPAIR_DISPLAY`. |
| `InputBuffer` | FIFO queue of timestamped requests. Entries expire after 70 ms. `consumeIfPresent()` checks only the head; a non-matching head blocks all subsequent checks until it expires. |

### statemachine

| Class | Description |
|-------|-------------|
| `UmbralStateMachine` | Extends `StateMachine<UmbralBlade>`. Tracks `previousState`. Supports `PreviousState` sentinel transitions. Calls `setDisplayTransformation()` on every state change. |
| `UmbralStateFacade` | Abstract supertype of all states. Used as the `from` type in wildcard transitions. |
| `UmbralState` | Legacy enum (unused by FSM; retained for reference). |

### statemachine/state

| State Class | Summary |
|-------------|---------|
| `InactiveState` | Blade dormant. Entered on spectator mode or `DEACTIVATE`. |
| `SheathedState` | Blade on hip as passenger. Soul Link in slot 0. Initial state. |
| `StandbyState` | Blade hovering behind player with glow and idle movement. |
| `WieldState` | Blade item in slot 0; display hidden. |
| `AttackingQuickState` | Executes `performSimpleAttack()`. Consumes soulfire per combo step. |
| `AttackingHeavyState` | Executes `performWideUmbralSweepAttack()`. Dynamic curve targeting. |
| `LungingState` | Blade thrown on Bezier trajectory. Transitions to Lodged or Recalling. |
| `GrabImpaleState` | Slerps to position above grabbed entity, then lunges. |
| `LodgedState` | Blade fixed at impact position with glow. Waits for recall. |
| `RecallingState` | Blade slerps back to player chest. Pushes STANDBY when it arrives. |
| `RecoverState` | Respawns the display entity when it is null or invalid. |
| `WaitingState` | Registers blade as interactable. Always transitions to Standby immediately. |
| `FinisherState` | Stub. All implementation commented out. |
| `PreviousState` | Sentinel. Transitions to this class restore `previousState`. |

## Lifecycle

`Combatant.handleUmbralBladeTick()` is called every entity tick. If `umbralBlade` is null, `setupUmbralBlade()` schedules construction 200 ms later to avoid spawning the display on the first tick. Every subsequent tick delegates to `umbralBlade.onTick()`, which calls `bladeStateMachine.tick()`.

Hostile entities override `setupUmbralBlade()` to immediately deactivate the blade after 250 ms, removing the display passenger. This preserves the Combatant contract without visual overhead.

On death, `umbralBlade.dispose()` marks the state machine deactivated and removes the display entity.

## Request Flow

```
Player input
  -> InputExecutionTree
  -> UmbralBladeAction.someMethod()
  -> blade.request(BladeRequest.X)         // enqueues in InputBuffer
     each tick:
       UmbralStateMachine.tick()
         -> currentState.onTick()
         -> check each Transition:
              isRequestedAndActive(X) calls consumeIfPresent(X)
              if true: onTransition().accept(), setState(newState)
```

## Extension

- Add a new state: create a class extending `UmbralStateFacade`, implement `onEnter()`, `onExit()`, `onTick()`, and register transitions in `UmbralBlade.initStateMachine()`.
- Add a new request: add a value to `BladeRequest` and a corresponding `Transition` in `initStateMachine()`.
- Add a new attack variant: add a `Function<Combatant, UmbralBladeAttack>` to the `basicAttacks` array in `loadBasicAttacks()`.

For full architecture details see `docs/systems/umbral-blade.md`.
