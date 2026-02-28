# Hostile AI Package

## Overview

This package implements a finite state machine (FSM) driven AI for `Hostile` entities. It governs every phase of enemy behaviour — idle patrol, player detection, approach, group coordination, attack wind-up, attack execution, post-attack retreat, and low-health flight — through seven concrete states wired together with twelve transitions.

The design mirrors the `UmbralBlade` FSM found in `system/entity/umbral/statemachine/`. `HostileAIFacade` plays the same role as `UmbralStateFacade`: a common abstract base that enables wildcard transitions (transitions whose `from` type matches any concrete state in the machine). The generic `StateMachine<T>` / `State<T>` / `Transition<T>` infrastructure in `utility/statemachine/` is shared between both machines without modification.

---

## FSM State Diagram

```
                         +-------+
           +------------>| IDLE  |<------------------------------+
           |             +-------+                               |
           |               |                                     |
           |   [target in aggro range]                           |
           |               v                                     |
           |   +-----------+--+                                  |
           |   |  APPROACH    |                                  |
           |   +-----------+--+                                  |
           |       |       |                                     |
           |       |       | [ally count >= surround_min_allies] |
           |       |       v                                     |
           |       |   +----------+                              |
           |       |   | SURROUND |                              |
           |       |   +----+-----+                              |
           |       |        |                                    |
           |       |        | [front arc slot]                   |
           |       |        |                                    |
           |       | [dist < approach_dist]                      |
           |       |        |                                    |
           |       v        v                                     |
           |   +------------+--+                                  |
           |   |  PRE_ATTACK   |                                  |
           |   +---------------+                                  |
           |          |                                           |
           |          | [pre_attack_timer <= 0]                   |
           |          v                                           |
           |      +--------+                                      |
           |      | ATTACK |                                      |
           |      +---+----+                                      |
           |          |                                           |
           |          | [attack landed]                           |
           |          v                                           |
           |      +--------+     [cooldown elapsed +             |
           +------+ RETREAT|----> target in aggro range] --> APPROACH
                  +--------+
                       |
                       | [cooldown elapsed +
                       |  target left aggro range]
                       +-----> IDLE (above)

  ANY STATE --[health < flee_health_fraction]--> +------+
                                                 | FLEE |
                                                 +------+
                                                    |
                                    [no players     |
                                     in aggro range]+----> IDLE
```

Transition #9 (`ATTACK -> APPROACH`, target escapes aggro range) and the `SURROUND -> APPROACH` back-edge (ally count drops below threshold) are omitted from the ASCII art for readability; see the transition table below.

---

## Transition Table

| # | From | To | Condition | Side Effect |
|---|------|----|-----------|-------------|
| 1 | ANY (`HostileAIFacade`) | `FleeState` | Not already fleeing AND `health / maxHealth < FLEE_HEALTH_FRACTION` | Clear `currentTarget` |
| 2 | `IdleState` | `ApproachState` | `nearestScannedTarget != null` | Set `currentTarget` from scanned target |
| 3 | `ApproachState` | `SurroundState` | `nearbyAlliesCount >= SURROUND_MIN_ALLIES` | None |
| 4 | `ApproachState` | `PreAttackState` | `distanceSquared(target) < APPROACH_DISTANCE_SQUARED` | None |
| 5 | `SurroundState` | `ApproachState` | `nearbyAlliesCount < SURROUND_MIN_ALLIES` | None |
| 6 | `SurroundState` | `PreAttackState` | `isFrontSlot()` | Set `frontSlot = false` |
| 7 | `PreAttackState` | `AttackState` | `preAttackTimer <= 0` | None |
| 8 | `AttackState` | `RetreatState` | `isAttackDone()` | None |
| 9 | `AttackState` | `ApproachState` | Target invalid or `distanceSquared > AGGRO_RANGE_SQUARED` | None |
| 10 | `RetreatState` | `ApproachState` | `retreatTimer <= 0` AND target still in aggro range | None |
| 11 | `RetreatState` | `IdleState` | `retreatTimer <= 0` AND target left aggro range or invalid | Clear `currentTarget` |
| 12 | `FleeState` | `IdleState` | No `Player` entity within aggro radius | None |

---

## State Behaviour Reference

### `IdleState`

The mob is not engaged with a target. Bukkit's built-in awareness system is disabled (`setAware(false)`) so vanilla AI goals do not interfere. Every 60 ticks the mob pathfinds to a random point within 8 blocks of its spawn `origin` at 60% movement speed. Every 10 ticks an aggro scan queries nearby `Player` entities within `aggro_range` blocks using `World.getNearbyEntities()`, converts them to `SwordPlayer` wrappers through `SwordEntityArbiter`, and stores the closest one in `nearestScannedTarget`. When that field becomes non-null, transition #2 fires and the mob immediately enters `ApproachState`.

### `ApproachState`

The mob has acquired a target. Bukkit awareness is re-enabled and `Mob.setTarget()` is set to the target living entity. Each tick the mob pathfinds toward its target at 110% speed if the distance squared is more than `APPROACH_DISTANCE_SQUARED + 4.0` (a 2-block hysteresis band prevents oscillation at the boundary); pathfinding stops when the mob is within the threshold. Every 20 ticks an ally scan counts other `Hostile` entities registered in `SwordEntityArbiter` that share the same current target UUID. If that count reaches `SURROUND_MIN_ALLIES` the group transitions to `SurroundState`; if the mob instead closes to within `APPROACH_DISTANCE_SQUARED` it transitions directly to `PreAttackState`.

### `SurroundState`

The mob is part of a coordinated group attack. Every 20 ticks each mob in the group re-evaluates its arc slot by building a list of all allies (including itself) targeting the same entity, sorting them by `UUID` for a deterministic ordering, then computing a position on a circle of radius `approach_distance` around the target using its index in that list. Arc slot 0 is the "front slot"; the mob at slot 0 sets `frontSlot = true` and immediately transitions to `PreAttackState` via transition #6. All other mobs pathfind to their arc positions and wait. If the ally count falls below `SURROUND_MIN_ALLIES` the group reverts to `ApproachState` (transition #5).

### `PreAttackState`

The mob has committed to an attack. Pathfinding stops immediately. On entry: `TRIAL_SPAWNER_DETECTION_OMINOUS` particles are spawned at the mob's eye location (count 25, spread 0.4), and `BLOCK_TRIAL_SPAWNER_AMBIENT_OMINOUS` sound is broadcast to all players within 20 blocks. The `preAttackTimer` is set to `PRE_ATTACK_TICKS` and counts down by 1 each tick. Throughout the wind-up, `Mob.lookAt()` is called with maximum yaw and pitch speed so the mob continuously faces its target. When the timer reaches zero, transition #7 fires.

### `AttackState`

The mob charges toward its target at 160% speed. Each tick, once the distance squared drops to 6.25 (2.5-block melee reach), `Hostile.randomAttack()` is called. `randomAttack()` selects uniformly from `possibleAttacks` — a `List<Consumer<Combatant>>` populated at construction time. The default list contains one entry: a basic melee hit that calls `SwordEntity.hit()` with a `Prefab.Attacks.defaultMobHit` packet and 0.5-magnitude knockback directed away from the mob. After the attack fires, `attackDone` is set to `true` and transition #8 sends the mob to `RetreatState`. Transition #9 provides an escape path if the target moves out of aggro range before melee reach is achieved.

### `RetreatState`

After landing an attack the mob backs off before re-engaging. On entry the mob's position relative to the target is used to compute a retreat direction vector; it pathfinds 8 blocks directly away from the target at 110% speed. The `retreatTimer` is initialised to `RETREAT_TICKS` and counts down by 1 per tick. Once the timer reaches zero, transition #10 re-enters `ApproachState` if the target is still within aggro range, or transition #11 returns to `IdleState` if the target has left.

### `FleeState`

The mob's health has dropped below `FLEE_HEALTH_FRACTION` of its maximum. `currentTarget` is cleared on transition entry. `Mob.setAware(true)` is called so the mob can move freely. Every 20 ticks the nearest `Player` within aggro range is located and a flee direction is computed as the vector pointing away from that player; the mob pathfinds 16 blocks along that vector at 150% speed. The direction is recalculated on a cadence rather than every tick to reduce the cost of `World.getNearbyPlayers()`. Transition #12 checks for the complete absence of players in aggro range and returns to `IdleState`.

---

## Cadence Table

| Timer field | Owner state(s) | Cadence | Action |
|-------------|---------------|---------|--------|
| `idleWanderTimer` | `IdleState` | 60 ticks (3 s) | Pick random wander target near origin |
| `aggroScanTimer` | `IdleState` | 10 ticks (0.5 s) | Scan for nearby players; set `nearestScannedTarget` |
| `allyScanTimer` | `ApproachState`, `SurroundState` | 20 ticks (1 s) | Count allies targeting the same player; recompute arc in `SurroundState` |
| `fleeScanTimer` | `FleeState` | 20 ticks (1 s) | Recalculate flee direction toward nearest player |
| `preAttackTimer` | `PreAttackState` | counts down from `PRE_ATTACK_TICKS` | Fires attack on reaching 0 |
| `retreatTimer` | `RetreatState` | counts down from `RETREAT_TICKS` | Allows re-engage on reaching 0 |

Timers that count upward reset to zero at their cadence threshold and are stored as fields on `Hostile` so their values persist across state boundaries. Timers that count downward are initialised in `onEnter` and reset to zero in `onExit`.

---

## How to Add a New State

1. Create a class in `btm.sword.system.entity.ai.state` that extends `HostileAIFacade`.
2. Implement `name()`, `onEnter(Hostile)`, `onTick(Hostile)`, and `onExit(Hostile)`.
3. If the state requires its own timer or flag, add the field (and Lombok annotations) to `Hostile.java`.
4. Add any config values needed to `Config.Hostile` (see the section below).
5. All concrete state classes must have a public no-argument constructor because `StateMachine.createState()` instantiates them via `clazz.getDeclaredConstructor().newInstance()`.

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
| `HostileStateMachine.java` | Extends `StateMachine<Hostile>`; registers all 12 transitions |
| `state/IdleState.java` | Patrol and aggro scan |
| `state/ApproachState.java` | Pathfind toward target; ally scan |
| `state/SurroundState.java` | Arc formation; front-slot attack delegation |
| `state/PreAttackState.java` | Wind-up timer; ominous particles and sound |
| `state/AttackState.java` | Charge and melee hit via `randomAttack()` |
| `state/RetreatState.java` | Back off 8 blocks; hold for `RETREAT_TICKS` |
| `state/FleeState.java` | Flee from nearest player at low HP |
