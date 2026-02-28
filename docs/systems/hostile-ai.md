# Hostile AI System

## Overview

The Hostile AI system gives enemy (`Hostile`) entities fully autonomous, FSM-driven behaviour. Seven states cover the complete engagement lifecycle: passive patrol, player detection, approach, group coordination, attack wind-up, melee strike, post-attack withdrawal, and low-health flight. The system is designed around the same generic state machine infrastructure as the UmbralBlade FSM and integrates cleanly into the existing `SwordEntity` lifecycle without altering any shared base class.

---

## Architecture

### Entity Hierarchy Integration

```
SwordEntity  (abstract, btm.sword.system.entity.base)
 +-- Combatant  (abstract, btm.sword.system.entity.impl)
      +-- Hostile  (btm.sword.system.entity.impl)
```

`Hostile` holds a `HostileStateMachine aiStateMachine` field. The field is nullable: it is `null` until `onSpawn()` fires, and is explicitly set back to `null` in `onDeath()` so that a dead mob's `onTick()` is a no-op at the AI layer.

```java
// Hostile.onSpawn()
aiStateMachine = new HostileStateMachine(this, new IdleState());

// Hostile.onTick()
if (aiStateMachine != null) {
    aiStateMachine.tick();
}

// Hostile.onDeath()
aiStateMachine = null;
```

The tick chain is:

```
SwordEntity.onTick()
  -> Combatant.onTick()        (handles UmbralBlade tick)
    -> Hostile.onTick()        (ticks aiStateMachine)
      -> StateMachine.tick()   (calls currentState.onTick, then evaluates transitions)
```

`StateMachine.tick()` always calls `onTick` before evaluating transitions. A transition that fires within a given tick causes the machine to return immediately; the new state's `onTick` is not called until the following tick.

### Generic State Machine Infrastructure

The FSM infrastructure is in `btm.sword.utility.statemachine`:

| Class | Role |
|-------|------|
| `StateMachine<T>` | Holds the context, current state, and a `HashMap` of `Transition<T>` entries. `tick()` calls `onTick`, iterates transitions, fires the first matching one, then returns. State instantiation uses `clazz.getDeclaredConstructor().newInstance()`. |
| `State<T>` | Abstract; four lifecycle callbacks: `name()`, `onEnter(T)`, `onTick(T)`, `onExit(T)`. |
| `Transition<T>` | Record holding `from` (source state class), `to` (target state class), `condition` (`Predicate<T>`), and `onTransition` (`Consumer<T>`). Matching uses `from.isAssignableFrom(currentState.getClass())`. |

### Facade Pattern for Wildcard Transitions

`HostileAIFacade extends State<Hostile>` is an abstract supertype shared by all seven concrete AI states. Registering a transition with `HostileAIFacade.class` as the `from` type means the transition matches regardless of which concrete state the machine is currently in, because `isAssignableFrom` returns `true` for any subclass. This is how transition #1 (any state -> `FleeState`) is expressed without duplicating the condition across all source states.

The pattern is identical to `UmbralStateFacade` in the UmbralBlade FSM.

---

## Key Classes

| Class | Package | Role |
|-------|---------|------|
| `Hostile` | `system/entity/impl` | Combatant subclass; owns the `HostileStateMachine`, all AI timer fields, arc-slot state, and the `possibleAttacks` list |
| `HostileAIFacade` | `system/entity/ai` | Abstract base for all AI states; enables wildcard transitions |
| `HostileStateMachine` | `system/entity/ai` | Extends `StateMachine<Hostile>`; registers 12 transitions in `initTransitions()` |
| `IdleState` | `system/entity/ai/state` | Patrol wander; aggro scan every 10 ticks |
| `ApproachState` | `system/entity/ai/state` | Pathfind to target; ally scan every 20 ticks |
| `SurroundState` | `system/entity/ai/state` | Arc formation; delegates attack to front-slot mob |
| `PreAttackState` | `system/entity/ai/state` | Stop and face target; ominous particles/sound; wind-up countdown |
| `AttackState` | `system/entity/ai/state` | Charge; fire `randomAttack()` at 2.5-block melee reach |
| `RetreatState` | `system/entity/ai/state` | Back off 8 blocks; hold for `RETREAT_TICKS` |
| `FleeState` | `system/entity/ai/state` | Run from nearest player; recalculate direction every 20 ticks |

---

## State and Transition Data Flow

### Full Transition Table

| # | From | To | Condition |
|---|------|----|-----------|
| 1 | `HostileAIFacade` (any) | `FleeState` | Not already in `FleeState` AND `health / maxHealth < FLEE_HEALTH_FRACTION` |
| 2 | `IdleState` | `ApproachState` | `nearestScannedTarget != null` |
| 3 | `ApproachState` | `SurroundState` | `nearbyAlliesCount >= SURROUND_MIN_ALLIES` |
| 4 | `ApproachState` | `PreAttackState` | `distSq(target) < APPROACH_DISTANCE_SQUARED` |
| 5 | `SurroundState` | `ApproachState` | `nearbyAlliesCount < SURROUND_MIN_ALLIES` |
| 6 | `SurroundState` | `PreAttackState` | `isFrontSlot()` is `true` |
| 7 | `PreAttackState` | `AttackState` | `preAttackTimer <= 0` |
| 8 | `AttackState` | `RetreatState` | `isAttackDone()` is `true` |
| 9 | `AttackState` | `ApproachState` | Target invalid or `distSq(target) > AGGRO_RANGE_SQUARED` |
| 10 | `RetreatState` | `ApproachState` | `retreatTimer <= 0` AND target in aggro range |
| 11 | `RetreatState` | `IdleState` | `retreatTimer <= 0` AND target out of aggro range or invalid |
| 12 | `FleeState` | `IdleState` | No `Player` within aggro radius |

### Timer and Flag Ownership

All timer and flag fields reside directly on `Hostile` (not on state instances, since state instances are discarded and recreated on each transition). States initialise their timers in `onEnter` and clean them up in `onExit`. Upward-counting timers check against their threshold and reset to zero; downward-counting timers are initialised to a constant and decrement to zero.

| Field | Type | Direction | Reset in |
|-------|------|-----------|----------|
| `aggroScanTimer` | `int` | up, resets at 10 | `IdleState.onEnter` |
| `idleWanderTimer` | `int` | up, resets at 60 | `IdleState.onEnter` |
| `allyScanTimer` | `int` | up, resets at 20 | `ApproachState.onEnter`, `SurroundState.onEnter` |
| `fleeScanTimer` | `int` | up, resets at 20 | `FleeState.onEnter` |
| `preAttackTimer` | `int` | down from `PRE_ATTACK_TICKS` | `PreAttackState.onEnter` (init), `PreAttackState.onExit` (clear) |
| `retreatTimer` | `int` | down from `RETREAT_TICKS` | `RetreatState.onEnter` (init), `RetreatState.onExit` (clear) |
| `nearestScannedTarget` | `SwordEntity` | set in scan, cleared on exit | `IdleState.onEnter` |
| `currentTarget` | `SwordEntity` | set on transition to Approach/Flee | transition `onTransition` callbacks |
| `attackDone` | `boolean` | set in `AttackState.onTick` | `AttackState.onEnter` |
| `frontSlot` | `boolean` | set in `SurroundState.evaluateArcPosition` | transition #6 `onTransition` |
| `arcSlotIndex` | `int` | set in `SurroundState.evaluateArcPosition` | — |
| `nearbyAlliesCount` | `int` | set in ally scan | `ApproachState.onEnter` |

---

## The UmbralBlade "Always Inactive" Pattern for Mobs

`Combatant.setupUmbralBlade()` is called by the base tick loop the first time a `Combatant` has no blade. `Hostile` overrides this method and schedules a 250 ms delayed task that, once the blade has been created, immediately calls `blade.request(BladeRequest.DEACTIVATE)`. It also removes the display entity from the mob's passenger list and clears its item stack. The result is that the mob holds a real `UmbralBlade` in its permanent `InactiveState` but no visual sword appears on the entity.

This approach preserves the inheritance contract (every `Combatant` has a blade) while keeping the mob's visual appearance clean and the blade's tick cost near zero. The `onImpaledByBlade(UmbralBlade)` hook on `Hostile` is stubbed (tracked under TODO #189) as the extension point for future mob-vs-blade interactions: reactions when the player's thrown blade lodges in a mob.

---

## Configuration

All tunable values live in the `Config.Hostile` static inner class in `btm.sword.config.Config`. Each field is a `public static` value updated at startup and on `/sword reload`. The self-registration pattern stores a `ConfigEntry` in `Config.ENTRIES` via a `static { register(...) }` block alongside each field.

| Config key | Java field | Default | Notes |
|------------|-----------|---------|-------|
| `hostile.aggro_range` | `AGGRO_RANGE_SQUARED` | 16 blocks (stored as 256.0) | Raw radius is squared on assignment |
| `hostile.approach_distance` | `APPROACH_DISTANCE_SQUARED` | 6 blocks (stored as 36.0) | Raw radius is squared on assignment |
| `hostile.surround_min_allies` | `SURROUND_MIN_ALLIES` | 2 | Minimum allied count to trigger surround |
| `hostile.pre_attack_ticks` | `PRE_ATTACK_TICKS` | 24 (~1.2 s at 20 TPS) | Wind-up duration |
| `hostile.retreat_ticks` | `RETREAT_TICKS` | 40 (~2 s at 20 TPS) | Post-attack hold duration |
| `hostile.flee_health_fraction` | `FLEE_HEALTH_FRACTION` | 0.20 | Fraction of max HP below which flee triggers |

Distance thresholds are stored squared to avoid calling `Math.sqrt()` during per-tick comparisons against `Location.distanceSquared()`. The only place `Math.sqrt()` is computed at runtime is when passing a radius to `World.getNearbyEntities()` or `World.getNearbyPlayers()`, which requires a linear radius, not a squared one.

---

## Performance Notes

### Cadence-Based Scanning

The two most expensive per-tick operations are world entity queries (`World.getNearbyEntities`, `World.getNearbyLivingEntities`, `World.getNearbyPlayers`). None of these is called every tick. Aggro scans run every 10 ticks (0.5 s); ally scans and flee-direction recalculations run every 20 ticks (1 s); wander updates run every 60 ticks (3 s). The attack-phase operations (melee reach check in `AttackState`, timer decrement in `PreAttackState` and `RetreatState`) are O(1) per tick.

### Squared Distance Comparisons

All threshold comparisons in conditions and state `onTick` methods use `Location.distanceSquared()` against pre-squared config constants. The only square roots extracted from config values are those passed to radius-based API calls. `ApproachState` adds a 4.0-unit squared hysteresis margin (`HYSTERESIS_SQ`) to the approach threshold to prevent pathfinding start/stop oscillation at the boundary.

### Ally Scan Filtering

Ally scans in `ApproachState` and `SurroundState` query `SwordEntityArbiter` via `SwordEntityArbiter.get(entity)` to convert Bukkit `LivingEntity` handles to `Hostile` wrappers, then filter on matching `currentTarget` UUID. Only entities already registered in the arbiter are considered, so the ally scan never instantiates new wrappers.

---

## Transition Ordering Caveat

`StateMachine` stores all transitions in a `java.util.HashMap`. HashMap provides no iteration order guarantee; the order in which transitions are evaluated on each tick is non-deterministic across JVM runs and can change as the map grows. In the current implementation this is not a correctness issue because:

- Transitions out of the same source state have mutually exclusive conditions (e.g., transitions #3 and #4 from `ApproachState` can both be true simultaneously only if ally count and attack range conditions overlap, which is architecturally possible).
- The flee wildcard (transition #1) guards against its own state (`!(state instanceof FleeState)`) to prevent re-entry.

If future transitions are added where two conditions from the same source state could both be true at once, the `HashMap` storage means the "winner" is not predictable. Consider switching to a `LinkedHashMap` or adding explicit priority ordering if deterministic evaluation order becomes necessary.

---

## Extension Points

### Adding a New State

1. Create a class in `btm.sword.system.entity.ai.state` extending `HostileAIFacade`.
2. Implement `name()`, `onEnter(Hostile)`, `onTick(Hostile)`, `onExit(Hostile)`.
3. State classes must have a public no-argument constructor; `StateMachine.createState()` instantiates them via reflection.
4. Add any required timer or flag fields to `Hostile` with Lombok `@Getter` / `@Setter`.
5. Register transitions to and from the new state in `HostileStateMachine.initTransitions()`.

### Adding a New Attack Type

`Hostile.possibleAttacks` is a `List<Consumer<Combatant>>` populated in the constructor. Add new entries to this list to expand the attack pool. `randomAttack()` selects uniformly from the list. Each consumer receives the `Hostile` as a `Combatant`; cast to `Hostile` to access target and AI state. Future work could replace the random selection with a weighted or context-sensitive selection strategy without changing any FSM code.

### Mob Blade Interactions (`onImpaledByBlade`)

`Hostile.onImpaledByBlade(UmbralBlade blade)` is a stub (TODO #189) called when the player's thrown blade lodges into this mob. This is the correct hook for implementing reactions: stagger states, counter-attacks, blade-grab mechanics, or applying a special debuff. Implementing this hook should not require changes to the FSM; the reaction can be modelled as a new state entered via a new transition triggered by a flag set in `onImpaledByBlade`.

### Group Tactics

The current `SurroundState` selects the front-slot attacker deterministically by UUID order. A more sophisticated group tactic system could:

- Maintain a shared coordination object (injected into `Hostile` at spawn or via a group manager) to avoid redundant `getNearbyLivingEntities` calls across all mobs in a group.
- Implement role-based slots (aggressor, flanker, bait) instead of a single front attacker.
- Introduce a `CoordinateState` where mobs share information before committing to surround positions.

None of these changes would require modifying the generic `StateMachine` infrastructure; they would be expressed as new state classes, new fields on `Hostile`, and new transitions in `HostileStateMachine`.

### Custom AI Profiles Per Mob Type

The current system applies the same FSM to all `Hostile` subtypes. A natural extension would be to introduce a `HostileAIProfile` (analogous to `CombatProfile`) that parameterises aggro range, attack pool, and state-machine variant per entity type, allowing different mobs to have different behaviour without subclassing `HostileStateMachine`.

---

## Interactions with Other Systems

| System | Interaction |
|--------|-------------|
| `SwordEntityArbiter` | Used in `IdleState`, `ApproachState`, and `SurroundState` to convert Bukkit entity handles to `SwordEntity` / `Hostile` wrappers for target tracking and ally counting |
| `Config` / `ConfigManager` | All timing and distance constants read from `Config.Hostile`; hot-reloaded via `/sword reload` without restarting the server |
| `UmbralBlade` / `BladeRequest` | `Hostile.setupUmbralBlade()` overrides the base method to immediately deactivate and suppress the blade visual; `onImpaledByBlade` provides a hook for future blade-vs-mob interactions |
| `SwordScheduler` / `TimeArbiter` | `setupUmbralBlade` uses `SwordScheduler.runBukkitTaskLater` for the 250 ms deactivation delay; the mob's `onTick` is driven by the `TimeArbiter`-scheduled tick inherited from `SwordEntity` |
| `Prefab.Attacks` | `defaultMobHit` is the `HitValuePacket` used by the default melee attack in `possibleAttacks` |
| `GrabAction` | `Hostile.grab()` is a stub forwarding to `GrabAction.grab(this)`; not currently called by any FSM state |
| `PlayerDataManager` | No direct interaction; `Hostile` entities are not persisted |

---

## Known Limitations

- The `SurroundState` ally scan scans from the target's location, not the mob's, which is correct for detecting the group but means mobs beyond aggro range of the mob itself but near the target may be counted.
- Transitions #10 and #11 from `RetreatState` have logically complementary conditions but are written as two independent predicates. If both are true simultaneously (e.g., timer is 0 and the target validity check produces different results between the two predicate evaluations), the outcome depends on HashMap iteration order.
- The `FleeState -> IdleState` transition (transition #12) performs a fresh `World.getNearbyEntities` call every tick that the condition is evaluated, which is every tick the mob is fleeing. This is the one cadence-free world query in the system.
- `Hostile` stub methods (`surround`, `approach`, `charge`, `retreat`, `flee`, `jump`) are empty and exist as extension points or legacy placeholders. The FSM is the authoritative behaviour controller; these methods are not called by the state machine.
