# Hostile AI Package

## Overview

This package implements a finite state machine (FSM) driven AI for `Hostile` entities. It governs every phase of enemy behaviour — idle patrol, player detection, approach, group coordination, attack wind-up, attack execution, three post-attack branches, and low-health flight — through nine concrete states wired together with nineteen transitions.

The design mirrors the `UmbralBlade` FSM found in `system/entity/umbral/statemachine/`. `HostileAIFacade` plays the same role as `UmbralStateFacade`: a common abstract base that enables wildcard transitions (transitions whose `from` type matches any concrete state in the machine). The generic `StateMachine<T>` / `State<T>` / `Transition<T>` infrastructure in `utility/statemachine/` is shared between both machines without modification.

---

## FSM State Diagram

```
                         +-------+
           +------------>| IDLE  |<----------------------------+
           |             +-------+                             |
           |               |                                   |
           |   [target in aggro range]                         |
           |               v                                   |
           |   +-----------+--+                                |
           |   |  APPROACH    |                                |
           |   +-----------+--+                                |
           |       |       |                                   |
           |       |       | [ally count >= surround_min]      |
           |       |       v                                   |
           |       |   +----------+                            |
           |       |   | SURROUND |                            |
           |       |   +----+-----+                            |
           |       |        |                                  |
           |       |        | [front arc slot]                 |
           |       |        |                                  |
           |       | [dist < approach_dist]                    |
           |       |        |                                  |
           |       v        v                                  |
           |   +------------+--+                               |
           |   |  PRE_ATTACK   |                               |
           |   +---------------+  <- mob moves during wind-up  |
           |          |                                        |
           |          | [timer <= 0 && !incapacitated]         |
           |          v                                        |
           |      +--------+                                   |
           |      | ATTACK |  <- fires ability immediately      |
           |      +---+----+                                   |
           |          |                                        |
           |          | [attack landed]                        |
           |          |                                        |
           |    roll 0|    roll 1|    roll 2|                  |
           |          v          v          v                  |
           |    +--------+  +----------+  (self-loop           |
           |    |ON_GUARD|  |ATK_READY |   combo re-entry)     |
           |    +--------+  +----------+                       |
           |        |           |                              |
           |    [timer]     [timer]                            |
           |    expired     expired                            |
           |        |           |                              |
           +--------+-----------+  [target in range] -> PRE_ATTACK
                    |              [target lost]     -> IDLE (above)

  ANY STATE --[health < flee_health_fraction]--> +------+
                                                 | FLEE |
                                                 +------+
                                                    |
                                    [no players     |
                                     in aggro range]+----> IDLE

  ANY STATE --[target switches to creative/spectator]--> IDLE
```

Transitions `SURROUND → APPROACH` (ally count drops) and the `RETREAT` sub-graph (orphaned but retained)
are omitted from the ASCII art for readability; see the transition table below.

---

## Transition Table

| # | From | To | Condition | Side Effect |
|---|------|----|-----------|-------------|
| 1 | ANY (`HostileAIFacade`) | `FleeState` | Not already fleeing AND `health / maxHealth < FLEE_HEALTH_FRACTION` | Clear `currentTarget` and `aggroTarget` |
| 2 | ANY (`HostileAIFacade`) | `IdleState` | Not already idle AND `currentTarget` is a player in creative/spectator | Clear `currentTarget` and `aggroTarget` |
| 3 | `IdleState` | `ApproachState` | `nearestScannedTarget != null` | Set `currentTarget` and `aggroTarget` from scanned target |
| 4 | `ApproachState` | `SurroundState` | `nearbyAlliesCount >= SURROUND_MIN_ALLIES` | None |
| 5 | `ApproachState` | `PreAttackState` | `distanceSquared(target) < APPROACH_DISTANCE_SQUARED` | None |
| 6 | `SurroundState` | `ApproachState` | `nearbyAlliesCount < SURROUND_MIN_ALLIES` | None |
| 7 | `SurroundState` | `PreAttackState` | `isFrontSlot()` | Set `frontSlot = false` |
| 8 | `PreAttackState` | `AttackState` | `preAttackTimer <= 0` **AND** `!isIncapacitated()` | None |
| 9 | `AttackState` | `OnGuardState` | `isAttackDone() && attackPostRoll == 0` AND target in aggro range | None |
| 10 | `AttackState` | `AttackReadyState` | `isAttackDone() && attackPostRoll == 1` AND target in aggro range | None |
| 11 | `AttackState` | `AttackState` (combo) | `isAttackDone() && attackPostRoll == 2` AND target in aggro range | Set `combo = true` |
| 12 | `AttackState` | `IdleState` | `isAttackDone()` AND target lost or left aggro range | Clear `currentTarget`; clear `aggroTarget` only if target is dead/invalid |
| ~~13~~ | ~~`AttackState` → `ApproachState`~~ | *(removed)* | Was: attack not done AND target escaped — obsolete because `attackDone` is always set on `AttackState` entry | |
| 14 | `OnGuardState` | `PreAttackState` | `onGuardTimer <= 0` AND target still in aggro range | None |
| 15 | `OnGuardState` | `IdleState` | `onGuardTimer <= 0` AND target lost or left aggro range | Clear `currentTarget`; clear `aggroTarget` only if target dead/invalid |
| 16 | `AttackReadyState` | `AttackState` | `attackReadyTimer <= 0` AND target in aggro range | None |
| 17 | `AttackReadyState` | `IdleState` | `attackReadyTimer <= 0` AND target lost or left aggro range | Clear `currentTarget`; clear `aggroTarget` only if target dead/invalid |
| 18 | `RetreatState` | `ApproachState` | `retreatTimer <= 0` AND target still in aggro range | None |
| 19 | `RetreatState` | `IdleState` | `retreatTimer <= 0` AND target left aggro range or invalid | Clear `currentTarget`; clear `aggroTarget` only if target dead/invalid |
| 20 | `FleeState` | `IdleState` | No `Player` entity within aggro radius | None |

---

## State Behaviour Reference

### `IdleState`

The mob is not engaged with a target. Bukkit's built-in awareness system is enabled (`setAware(true)`) so the custom `IdleWanderGoal` can run. Every 10 ticks an aggro scan runs:

1. **Remembered target check**: if `aggroTarget` is still alive and in aggro range (and not in creative/spectator), it is used as `nearestScannedTarget` immediately — no full scan needed. This lets mobs instantly re-engage the same player after a brief retreat.
2. **Fresh scan**: if no usable `aggroTarget` exists, `World.getNearbyEntities()` finds all `Player` entities in aggro range, converts them to `SwordPlayer` wrappers, and picks the nearest. Players in creative or spectator mode are excluded.

Players in creative or spectator mode are also excluded from being set as `aggroTarget` (transition #2 clears it if the mode changes mid-combat).

### `ApproachState`

The mob has acquired a target. Bukkit awareness is re-enabled and `Mob.setTarget()` is set to the target living entity. On entry, `ApproachGoal` computes the angle between the mob's current facing direction and the direction to the target. If the angle exceeds 15°, the goal delays pathfinding for `(angleDeg / 15)` ticks while `LookAtTargetGoal` rotates the mob — producing a visible swivel before movement begins. After the swivel, the mob pathfinds at 110% speed, stopping within `APPROACH_DISTANCE_SQUARED + 4.0` (hysteresis band). Every 20 ticks an ally scan counts `Hostile` entities targeting the same player; if the count reaches `SURROUND_MIN_ALLIES` the group enters `SurroundState`.

### `SurroundState`

The mob is part of a coordinated group attack. Every 20 ticks each mob in the group re-evaluates its arc slot by building a list of all allies (including itself) targeting the same entity, sorting them by `UUID` for a deterministic ordering, then computing a position on a circle of radius `approach_distance` around the target using its index in that list. Arc slot 0 is the "front slot"; the mob at slot 0 sets `frontSlot = true` and immediately transitions to `PreAttackState` via transition #7. All other mobs pathfind to their arc positions and wait. If the ally count falls below `SURROUND_MIN_ALLIES` the group reverts to `ApproachState` (transition #6).

### `PreAttackState`

The mob has committed to an attack. On entry:

1. `preAttackTimer` is set to `PRE_ATTACK_TICKS`.
2. `selectAbility()` is called to pick a `MobAbility` from the available (non-cooldown) pool and store it in `pendingAbility`.
3. `TRIAL_SPAWNER_DETECTION_OMINOUS` particles are spawned at the mob's eye location (count 25, spread 0.4), and `PRE_ATTACK` sound is broadcast to all players within 10 blocks.
4. Depending on the selected ability's category, a movement goal is registered:
   - **`MELEE`**: `ApproachGoal` (MOVE, priority 1) + `LookAtTargetGoal` (LOOK, priority 2) — mob closes in on the target during the wind-up.
   - **`RANGED`**: `PreAttackRetreatGoal` (MOVE, priority 1) + `LookAtTargetGoal` (LOOK, priority 2) — mob retreats from the target during the wind-up.
   - **No ability available**: pathfinding stops; mob holds position.

The `preAttackTimer` counts down by 1 each tick. When it reaches zero (and the mob is not incapacitated), transition #8 fires. On exit, all MOVE and LOOK goals are removed.

This design makes attack timing predictable from the telegraph: the mob **moves** during the wind-up and fires the ability **immediately** when the timer expires — the player's cue is the wind-up start, not a proximity threshold.

### `AttackState`

On entry, if the `combo` flag is set, a `Particle.CRIT` burst (8 particles) is spawned at the mob's eye location to signal the combo continuation, and `combo` is immediately reset to `false`.

**Incapacitation guard**: if `isIncapacitated()` is `true` (e.g., the mob was grabbed during pre-attack), the ability is not executed and `attackDone` is **not** set. The transition out of `AttackState` requires `attackDone = true`, so the state holds until the mob is released and the post-attack roll fires on the next re-entry attempt.

Otherwise, `pendingAbility.execute(h)` is called immediately — no charge, no distance gate — then `setAbilityCooldown()` sets the per-ability cooldown, `attackDone` is set to `true`, and `attackPostRoll` is set to a random integer 0–2. Three post-attack branches are selected by the roll:

- **Roll 0** → `OnGuardState` (transition #9): back off and strafe laterally.
- **Roll 1** → `AttackReadyState` (transition #10): pause and telegraph a follow-up.
- **Roll 2** → `AttackState` (combo, transition #11): immediate re-entry with a crit visual.

If the attack lands but the target has left aggro range, transition #12 returns to `IdleState`.

### `OnGuardState`

One of three post-attack branches (roll 0). On entry, `onGuardTimer` is set to `ON_GUARD_TICKS`, a `Particle.SMOKE` burst (12 particles) is spawned at the mob's eye location, and two goals are registered: `OnGuardBackoffGoal` (MOVE, priority 2) and `LookAtTargetGoal` (LOOK, priority 3). `OnGuardBackoffGoal` backs the mob off to `ON_GUARD_SAFE_DISTANCE` blocks from the target and then strafes laterally by incrementally advancing an orbit angle every 10 ticks, creating a weaving defensive circle. `LookAtTargetGoal` keeps the mob facing its target throughout.

When `onGuardTimer` reaches zero: transition #14 fires if the target is still in aggro range (→ `PreAttackState`); transition #15 fires if the target is gone (→ `IdleState`).

### `AttackReadyState`

One of three post-attack branches (roll 1). On entry, `attackReadyTimer` is set to `ATTACK_READY_TICKS`, pathfinding is stopped, and a `Particle.CRIT` burst (10 particles) is spawned to telegraph the follow-up. The mob holds position and faces the target each tick via `Mob.lookAt()` at maximum turn speed. This creates a visible "loading" pause before the next attack.

When `attackReadyTimer` reaches zero: transition #16 fires if the target is still in aggro range (→ `AttackState`); transition #17 fires if the target is gone (→ `IdleState`). Unlike `PreAttackState`, there is no wind-up particle or sound — the crit burst on entry serves as the sole telegraph.

### `RetreatState`

After landing an attack the mob backs off before re-engaging. On entry the mob's position relative to the target is used to compute a retreat direction vector; it pathfinds 8 blocks directly away from the target at 110% speed. The `retreatTimer` is initialised to `RETREAT_TICKS` and counts down by 1 per tick. Once the timer reaches zero, transition #18 re-enters `ApproachState` if the target is still within aggro range, or transition #19 returns to `IdleState` if the target has left.

> **Note:** `RetreatState` is no longer reachable from `AttackState` — all post-attack branches now lead to `OnGuardState`, `AttackReadyState`, or `AttackState` (combo). `RetreatState` and its transitions are retained for potential future use.

### `FleeState`

The mob's health has dropped below `FLEE_HEALTH_FRACTION` of its maximum. `currentTarget` is cleared on transition entry. `Mob.setAware(true)` is called so the mob can move freely. Every 20 ticks the nearest `Player` within aggro range is located and a flee direction is computed as the vector pointing away from that player; the mob pathfinds 16 blocks along that vector at 150% speed. The direction is recalculated on a cadence rather than every tick to reduce the cost of `World.getNearbyPlayers()`. Transition #20 checks for the complete absence of players in aggro range and returns to `IdleState`.

---

## MobAbility System

`MobAbility` (interface, `ability/`) is the abstraction for what a mob does at the end of its wind-up. Each ability declares:

| Method | Purpose |
|--------|---------|
| `name()` | Unique string key; used as the cooldown map key in `Hostile.abilityCooldowns` |
| `category()` | `AbilityCategory.MELEE` or `AbilityCategory.RANGED` — controls movement during pre-attack |
| `canUse(Hostile)` | Returns `true` if the cooldown has elapsed and preconditions are met |
| `execute(Hostile)` | Performs the ability — called once on `AttackState` entry |
| `cooldownTicks()` | How many ticks must pass before the ability can be selected again |

**Built-in abilities:**

| Class | Category | Description |
|-------|----------|-------------|
| `MobSlashAbility` | `MELEE` | Randomly selects `SLASH1`/`SLASH2`/`SLASH3` and runs a `MobSweepAttack` with `defaultMobHit` |
| `MobThrowAbility` | `RANGED` | Launches the mob's off-hand item as a `DroppedItem` with a parabolic arc toward the target |

**Selection flow in `PreAttackState.onEnter`:**
1. `selectAbility()` filters `possibleAbilities` by `canUse()` and picks one at random; stores it in `pendingAbility`.
2. If no ability is available, `pendingAbility = null` and pathfinding stops.
3. If an ability is selected, its `category()` determines which movement goal is registered.

**Execution flow in `AttackState.onEnter`:**
1. Guard: `if (isIncapacitated()) return;`
2. `pendingAbility.execute(h)` fires the ability.
3. `setAbilityCooldown(pendingAbility.name(), pendingAbility.cooldownTicks())` starts the cooldown.
4. `attackDone = true`, `attackPostRoll` is rolled.

**Cooldown tracking:** `Hostile.abilityCooldowns` is a `HashMap<String, Integer>` decremented every tick in `tickAbilityCooldowns()`. Entries are removed (not zeroed) when they reach 0, so `canUse()` checks `cooldowns.get(name()) == null || cooldown <= 0`.

---

## Grab Incapacitation

When a `Hostile` is grabbed by a `Combatant`, `SwordEntity.onGrabbed()` is called (wired in `Combatant.onGrab()`), which triggers `Hostile.onGrabbed()`:

- Sets `incapacitated = true`
- Calls `mob.setAware(false)` — stops all pathfinding
- Calls `MobGoalArbiter.GOALS.removeAllGoals(mob, GoalType.MOVE)` — clears any active movement goals

When released (`onGrabLetGo` or `onGrabThrow`), `Hostile.onReleased()`:
- Sets `incapacitated = false`
- Calls `mob.setAware(true)` — re-enables pathfinding

The FSM continues to tick during incapacitation. Transition #8 (`PreAttackState → AttackState`) has an `!isIncapacitated()` guard so the attack cannot fire while the mob is held. The `AttackState.onEnter` also has a guard: if incapacitated on entry (rare edge case), the ability is not executed and `attackDone` is not set, keeping the state stable until the mob is released and the next re-entry attempt.

---

## Cadence Table

| Timer field | Owner state(s) | Cadence | Action |
|-------------|---------------|---------|--------|
| `idleWanderTimer` | `IdleState` | 60 ticks (3 s) | Pick random wander target near origin |
| `aggroScanTimer` | `IdleState` | 10 ticks (0.5 s) | Scan for nearby players; set `nearestScannedTarget` |
| `allyScanTimer` | `ApproachState`, `SurroundState` | 20 ticks (1 s) | Count allies targeting the same player; recompute arc in `SurroundState` |
| `fleeScanTimer` | `FleeState` | 20 ticks (1 s) | Recalculate flee direction toward nearest player |
| `preAttackTimer` | `PreAttackState` | counts down from `PRE_ATTACK_TICKS` | Fires ability on reaching 0 (if not incapacitated) |
| `retreatTimer` | `RetreatState` | counts down from `RETREAT_TICKS` | Allows re-engage on reaching 0 |
| `onGuardTimer` | `OnGuardState` | counts down from `ON_GUARD_TICKS` | Allows re-engage on reaching 0 |
| `attackReadyTimer` | `AttackReadyState` | counts down from `ATTACK_READY_TICKS` | Triggers follow-up attack on reaching 0 |

Timers that count upward reset to zero at their cadence threshold and are stored as fields on `Hostile` so their values persist across state boundaries. Timers that count downward are initialised in `onEnter` and reset to zero in `onExit`.

---

## How to Add a New State

1. Create a class in `btm.sword.system.entity.ai.state` that extends `HostileAIFacade`.
2. Implement `name()`, `onEnter(Hostile)`, `onTick(Hostile)`, and `onExit(Hostile)`.
3. If the state requires its own timer or flag, add the field (and Lombok annotations) to `Hostile.java`.
4. Add any config values needed to `Config.Hostile` (see the section below).
5. All concrete state classes must have a public no-argument constructor because `StateMachine.createState()` instantiates them via `clazz.getDeclaredConstructor().newInstance()`.

---

## How to Add a New MobAbility

1. Create a class in `btm.sword.system.entity.ai.ability` implementing `MobAbility`.
2. Choose `AbilityCategory.MELEE` (mob approaches) or `AbilityCategory.RANGED` (mob retreats) for `category()`.
3. Add config entries in `Config.Hostile` for cooldown and any tunable parameters.
4. Register an instance in `Hostile`'s constructor via `possibleAbilities.add(new YourAbility())`.

---

## How to Add a New Transition

Open `HostileStateMachine.initTransitions()` and call `addTransition(new Transition<>(...))` with:

- `from` — the source state class (use `HostileAIFacade.class` for a wildcard that applies from any state).
- `to` — the destination state class.
- `condition` — a `Predicate<Hostile>` that is evaluated every tick while the mob is in the source state.
- `onTransition` — a `Consumer<Hostile>` for any side effects that must happen exactly when the transition fires.

Be aware that `StateMachine` stores transitions in a `HashMap`, which provides no iteration order guarantee. Ensure conditions for transitions out of the same source state are mutually exclusive, or accept that the order in which competing conditions are evaluated is not deterministic.

---

## Config Wiring: `Config.Hostile`

All tunable constants for this system live in the `Config.Hostile` static inner class in `btm.sword.config.Config`. Each field is a `public static` value initialised to its default. A `static { register(...) }` block below each field wires it into the `ConfigManager` self-registration mechanism so that it is automatically updated when `/sword reload` is run.

Distance-based constants (`aggro_range`, `approach_distance`) are stored as their squared values (`AGGRO_RANGE_SQUARED`, `APPROACH_DISTANCE_SQUARED`) by squaring the raw YAML value on assignment. This avoids calling `Math.sqrt()` on every tick when comparing against `Location.distanceSquared()`. Any new distance config entry should follow this same pattern.

To add a new config entry:

1. Add a `public static` field in `Config.Hostile` with the default value.
2. Add a `static { register(...) }` block referencing the YAML path, default, type, assignment consumer, and YAML reader function.
3. Add the YAML key and its default value in `src/main/resources/config.yaml` under the `hostile:` block.

Example (from the existing code):

```java
/** Wind-up ticks before an attack is executed (~1.2 s at 20 TPS). */
public static int PRE_ATTACK_TICKS = 24;
static { register(
    "hostile.pre_attack_ticks",
    24, Integer.class,
    v -> PRE_ATTACK_TICKS = v,
    (s, p, d) -> s.getInt(p, d)
); }
```

---

## Package Contents

| File | Role |
|------|------|
| `HostileAIFacade.java` | Abstract base for all Hostile AI states; enables wildcard transitions |
| `HostileStateMachine.java` | Extends `StateMachine<Hostile>`; registers all 19 active transitions |
| `ability/MobAbility.java` | Interface for discrete mob combat abilities |
| `ability/AbilityCategory.java` | Enum: `MELEE` (approach) / `RANGED` (retreat) during pre-attack |
| `ability/MobSlashAbility.java` | Melee slash using `SLASH1`/`SLASH2`/`SLASH3` Bezier curves |
| `ability/MobThrowAbility.java` | Ranged throw: launches off-hand item as `DroppedItem` with arc |
| `state/IdleState.java` | Patrol, aggro scan; prioritises `aggroTarget` for fast re-engagement |
| `state/ApproachState.java` | Pathfind toward target; ally scan; look goal wired |
| `state/SurroundState.java` | Arc formation; front-slot attack delegation; look goal wired |
| `state/PreAttackState.java` | Wind-up with mobile movement; selects ability; wires movement goal by category |
| `state/AttackState.java` | Fires `pendingAbility` immediately on entry; rolls post-attack branch |
| `state/OnGuardState.java` | Post-attack branch (roll 0): orbit target at safe distance; face target continuously |
| `state/AttackReadyState.java` | Post-attack branch (roll 1): hold position and telegraph follow-up attack |
| `state/RetreatState.java` | Back off 8 blocks; hold for `RETREAT_TICKS`; look goal wired (currently unreachable) |
| `state/FleeState.java` | Flee from nearest player at low HP; look goal wired |
| `goal/IdleWanderGoal.java` | Three-phase idle patrol: IDLE → LOOK → WALK |
| `goal/ApproachGoal.java` | Pathfind toward target at 1.1x; swivel delay on entry |
| `goal/SurroundHoldGoal.java` | Pathfind to arc position around target |
| `goal/AttackChargeGoal.java` | Charge toward target at 1.6x (legacy; no longer used in `AttackState`) |
| `goal/PreAttackRetreatGoal.java` | RANGED pre-attack: retreat from target each tick while `preAttackTimer > 0` |
| `goal/OnGuardBackoffGoal.java` | Orbit target at safe distance; strafe via advancing orbit angle |
| `goal/RetreatBackoffGoal.java` | One-shot backoff 8 blocks away from target |
| `goal/FleeGoal.java` | Recalculate flee direction every 20 ticks; pathfind away from nearest player |
| `goal/LookAtTargetGoal.java` | Look goal that faces `currentTarget` each tick |
| `goal/LookWhereGoingGoal.java` | Look goal that faces the mob's velocity direction (Retreat, Flee) |
| `goal/ObserveGoal.java` | Look goal that pans to random nearby points; available for future states |

---

## Adventure Goal System Integration

Each FSM state owns one custom `Goal` subclass in the `goal/` subpackage. The goal handles all pathfinding commands for that state; the FSM state itself handles timer management, flag updates, and transition condition evaluation. Neither layer has access to the other's internal state.

### Lifecycle Mapping

| FSM callback | Goal / `MobGoals` API call |
|--------------|---------------------------|
| `State.onEnter(Hostile)` | `MobGoalArbiter.GOALS.addGoal(mob, priority, new XxxGoal(mob, hostile))` |
| `State.onExit(Hostile)` | `MobGoalArbiter.GOALS.removeGoal(mob, XxxGoal.KEY)` |
| `Goal.start()` | Register any Bukkit event listeners needed for the active period |
| `Goal.stop()` | Unregister listeners, stop active pathfinding |
| `Goal.tick()` | Issue or refresh pathfinding commands toward the current target |
| `Goal.shouldActivate()` | Return `true` immediately — the FSM has already decided to enter the state |
| `Goal.shouldStayActive()` | Mirror the FSM's own transition exit conditions as a secondary safety net |

`shouldActivate()` returning `true` on every tick is correct for state-owned goals because they are only registered while the FSM is in the owning state. The FSM transition (which calls `removeGoal` in `onExit`) is the primary deactivation mechanism; `shouldStayActive()` returning `false` is a secondary path that lets Paper stop the goal without waiting for the next FSM tick.

### Awareness Flag Pattern

| FSM state | `setAware` value | Reason |
|-----------|-----------------|--------|
| `IdleState` | `false` | Prevents vanilla AI from interfering with custom idle wander |
| `ApproachState` | `true` | Target pursuit and pathfinding require awareness |
| `SurroundState` | `true` | Arc positioning requires active pathfinding |
| `PreAttackState` | `true` | Mob moves toward/away from target during wind-up |
| `AttackState` | `true` | Goals may still be active on entry (cleared in `onExit`) |
| `OnGuardState` | `true` | Orbit pathfinding requires awareness |
| `AttackReadyState` | `true` | Mob is stationary but aware, facing target |
| `RetreatState` | `true` | Retreat direction pathfinding requires awareness |
| `FleeState` | `true` | Flee pathfinding requires awareness |
| **Grabbed (incapacitated)** | `false` | `Hostile.onGrabbed()` disables awareness; re-enabled in `onReleased()` |

`Mob.setAi(false)` is not used in the current system. It would be appropriate for a future stunned or frozen state where all AI processing should halt.

### Vanilla Goal Removal

All vanilla goals are cleared immediately after mob spawn via `MobGoalArbiter.GOALS.removeAllGoals(mob)`. This prevents vanilla attacks, pathfinding targets, and look behaviours from interfering with the custom FSM-driven goals. Custom goals are then registered selectively by FSM states as the mob transitions between them.

### Goal Class Reference

| FSM State | Move Goal | Look Goal | Notes |
|-----------|-----------|-----------|-------|
| `IdleState` | `IdleWanderGoal` (`MOVE`) | (none — IdleWanderGoal handles look internally) | Patrol near spawn origin in a three-phase IDLE → LOOK → WALK cycle |
| `ApproachState` | `ApproachGoal` (`MOVE`) | `LookAtTargetGoal` (`LOOK`) | Pathfind at 1.1x; swivel phase delays pathfinding on entry until mob has turned to face target |
| `SurroundState` | `SurroundHoldGoal` (`MOVE`) | `LookAtTargetGoal` (`LOOK`) | Pathfind to arc slot position; face target while holding |
| `PreAttackState (MELEE)` | `ApproachGoal` (`MOVE`) | `LookAtTargetGoal` (`LOOK`) | Mob closes in at 1.1x during the wind-up |
| `PreAttackState (RANGED)` | `PreAttackRetreatGoal` (`MOVE`) | `LookAtTargetGoal` (`LOOK`) | Mob retreats 5 blocks; direction recomputed each tick |
| `PreAttackState (none)` | (none — pathfinding stopped) | (none) | No ability available; mob holds position |
| `AttackState` | (none) | (none) | Ability fires immediately on entry; no charge goal |
| `OnGuardState` | `OnGuardBackoffGoal` (`MOVE`) | `LookAtTargetGoal` (`LOOK`) | Orbit target at safe distance (priority 2 MOVE, priority 3 LOOK) |
| `AttackReadyState` | (none — pathfinding stopped) | (none — manual `lookAt` at 100f/100f) | Hold position; face target via direct `lookAt` calls each tick |
| `RetreatState` | `RetreatBackoffGoal` (`MOVE`) | `LookWhereGoingGoal` (`LOOK`) | Back off 8 blocks; face direction of travel |
| `FleeState` | `FleeGoal` (`MOVE`) | `LookWhereGoingGoal` (`LOOK`) | Pathfind away from nearest player; face direction of travel |

`ObserveGoal` (`LOOK`) is implemented and available but not yet wired to a state. It is intended for future hold or observe states where the mob should casually look around without a specific target.

All combat states that use goals remove them via `removeAllGoals(mob, GoalType.MOVE)` and/or `removeAllGoals(mob, GoalType.LOOK)` in `onExit`.
