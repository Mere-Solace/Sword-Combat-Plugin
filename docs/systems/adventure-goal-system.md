# Adventure Goal System

## Overview

Paper exposes a first-class AI goal API under the `com.destroystokyo.paper.entity.ai` package. Goals are behaviour units registered against a specific `Mob` instance at runtime. They are checked, activated, and ticked by Paper's internal scheduler alongside vanilla goals. Sword uses this API to drive the pathfinding layer of `Hostile` entities: each FSM state owns one custom `Goal` subclass that handles movement commands while the FSM state itself handles flag updates, timer management, and transition condition checks.

The central access point is `MobGoalArbiter`, which holds the singleton `MobGoals` reference obtained once at plugin startup via `Bukkit.getMobGoals()`.

---

## API Overview

### `MobGoals`

Retrieved via `Bukkit.getMobGoals()` and stored as `MobGoalArbiter.GOALS`. All goal management flows through this object.

| Method | Description |
|--------|-------------|
| `addGoal(mob, priority, goal)` | Registers a goal on the mob with the given integer priority |
| `removeGoal(mob, goal)` | Deregisters a goal by instance |
| `removeAllGoals(mob)` | Removes every goal (vanilla and custom) from the mob |
| `removeAllGoals(mob, GoalType)` | Removes all goals of a specific `GoalType` from the mob |
| `hasGoal(mob, key)` | Returns `true` if a goal with the given `GoalKey` is currently registered |
| `getGoal(mob, key)` | Returns the goal instance for the given key, or `null` if absent |
| `getGoals(mob, GoalType)` | Returns all goals of a given type registered on the mob |

### `Goal<T extends Mob>`

The five lifecycle methods called by Paper on each goal instance:

| Method | When called | FSM analogue |
|--------|-------------|--------------|
| `shouldActivate()` | Every tick while the goal is registered but not active | Pre-activation gate |
| `shouldStayActive()` | Every tick while the goal is active | Continuation check (secondary exit condition) |
| `start()` | Once, when the goal transitions from inactive to active | `onEnter` |
| `stop()` | Once, when the goal transitions from active to inactive | `onExit` |
| `tick()` | Every tick while the goal is active | `onTick` |

Two additional methods declare the goal's identity:

| Method | Description |
|--------|-------------|
| `getKey()` | Returns a `GoalKey<T>` used for lookup and removal |
| `getTypes()` | Returns an `EnumSet<GoalType>` declaring which behaviour categories this goal occupies |

A `GoalKey<T>` is constructed with `GoalKey.of(Mob.class, new NamespacedKey(plugin, "goal_name"))`. The key must be unique per goal class; goals are looked up and removed by key.

### `GoalType`

An enum that categorises goal behaviour. Paper's priority system only allows one active goal per `GoalType` at a time: among all registered goals of the same type, the one with the lowest priority integer that returns `true` from `shouldActivate()` (or `shouldStayActive()` if already active) wins. Goals of different types can run simultaneously.

Common types used in Sword goals:

| Type | Meaning |
|------|---------|
| `MOVE` | Controls the mob's movement / pathfinding |
| `LOOK` | Controls where the mob looks |
| `TARGET` | Controls target selection |
| `JUMP` | Controls jumping behaviour |

All Sword movement goals declare `GoalType.MOVE` to prevent conflicting movement goals from running at the same time.

---

## Priority System

Lower integer = higher priority. A goal with priority 1 will always beat a goal with priority 5 if both are registered and both return `true` from `shouldActivate()`. Priorities only compete within the same `GoalType`.

Vanilla mobs arrive with built-in goals at various priorities. These will conflict with custom pathfinding unless cleared. The standard pattern is to call `MobGoalArbiter.GOALS.removeAllGoals(mob)` immediately after the mob spawns to wipe all vanilla goals before registering custom ones. Selective removal by `GoalType` can be used if some vanilla behaviour (e.g., `LOOK`) is to be retained.

Sword uses the following priority convention for custom goals:

| Priority | Intended use |
|----------|-------------|
| 1 | Movement goals that have taken control (approach, attack charge, flee) |
| 2 | Background movement goals (idle wander, backoff strafe) |

These are guidelines, not enforced by the framework. Adjust per state as needed; the key constraint is that any goal that must override another must have a strictly lower priority integer.

---

## Awareness and AI Flags

Two `Mob` flags interact with the goal system:

| Method | Effect                                                                                                                                           |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `Mob.setAi(boolean)` | When `false`, Paper's entire AI loop is suspended: no goals run, no pathfinding updates, no vanilla behaviour. This is a kill switch for all AI. |
| `Mob.setAware(boolean)` | When `false`, the mob does not notice or react to its environment (players, projectiles, etc.), and all AI ticks are skipped.                    |

For custom goals to activate and for pathfinding targets to be pursued, **both `ai=true` and `aware=true` must hold**. The relevant flags are not set at the goal level; they are set by FSM state transitions on `Hostile`.

The pattern used in the Hostile AI system:

- Idle and passive states set `mob.setAware(false)` so vanilla AI (including any surviving vanilla goal remnants) does not interfere with custom idle wander behaviour.
- Combat states (`ApproachState`, `PreAttackState`, `AttackState`, `FleeState`) set `mob.setAware(true)` so the mob reacts to the environment and pathfinding targets are properly pursued.

`setAi(false)` is not currently used in Sword; it would be appropriate for stunned or frozen states where all AI should halt.

---

## Integration with the Hostile FSM

### Layered Responsibility

The FSM and the goal system serve complementary but distinct roles:

| Layer | Responsibility |
|-------|---------------|
| FSM state (`HostileAIFacade` subclass) | Timer management, flag updates, transition condition evaluation, awareness flag control |
| `Goal` subclass | Pathfinding commands, continuous movement, listener scoping |

This separation keeps the FSM free of direct pathfinder API calls and keeps goals free of state machine state. Neither layer needs to know the other's internal structure.

### Lifecycle Mapping

When an FSM state activates:

1. `State.onEnter(Hostile)` calls `MobGoalArbiter.GOALS.addGoal(mob, priority, new XxxGoal(mob, hostile))`.
2. Paper's goal evaluator calls `goal.shouldActivate()` on the next tick. The goal should return `true` immediately (the FSM has already made the decision to enter the state).
3. Paper calls `goal.start()`. Any listeners needed for the goal's active lifetime are registered here (see `RandomWanderGoal`).
4. Each subsequent tick: Paper calls `goal.tick()` for pathfinding commands; the FSM independently calls `state.onTick()` for timer decrements and transition checks.

When an FSM state deactivates:

1. `State.onExit(Hostile)` calls `MobGoalArbiter.GOALS.removeGoal(mob, XxxGoal.KEY)`.
2. Paper calls `goal.stop()`. Listeners registered in `start()` are unregistered here.

`shouldStayActive()` acts as a secondary exit condition: it is checked every tick while the goal is active. Returning `false` causes Paper to stop the goal automatically. In Sword, the primary exit mechanism is the FSM transition (which removes the goal via `onExit`); `shouldStayActive()` is a secondary safety net for conditions the goal itself can evaluate without consulting the FSM.

### Why Goals Instead of Direct Pathfinder Calls

`mob.getPathfinder().moveTo(location)` is a one-shot command. Once issued, it is not re-evaluated, does not integrate with the priority system, and provides no lifecycle hooks. Custom goals provide:

- **Ongoing management**: `tick()` re-issues or adjusts the pathfinding target every tick, handling dynamic targets.
- **Priority arbitration**: Conflicting `GoalType.MOVE` goals are resolved automatically by the priority integer, with no custom logic required.
- **Obstacle avoidance**: Goals participate in Paper's vanilla navigation system, which understands the world geometry.
- **Listener scoping**: `start()` and `stop()` provide precise lifetime control for event listeners, avoiding the manual registration/unregistration bookkeeping that direct pathfinder calls require.

---

## Current State

### What Exists

`MobGoalArbiter` holds the static `GOALS` reference:

```java
// btm.sword.system.entity.ai.MobGoalArbiter
public static final MobGoals GOALS = Bukkit.getMobGoals();
```

`RandomWanderGoal` is the only fully implemented goal. It demonstrates every structural requirement for a Sword goal:

- A `static final GoalKey<Mob> KEY` built from `GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "random_wander"))`.
- `getTypes()` returning `EnumSet.of(GoalType.MOVE)`.
- `shouldActivate()` returning `!mob.getPathfinder().hasPath()` — starts the goal when the mob has no active path.
- `shouldStayActive()` returning `true` — the goal runs until explicitly removed by the FSM.
- `start()` registering the class as a Bukkit event listener.
- `stop()` calling `HandlerList.unregisterAll(this)` to clean up the listener.
- `tick()` re-issuing a random movement command whenever the path is exhausted.

### What Is Not Yet Wired

The FSM states in `btm.sword.system.entity.ai.state` currently issue pathfinding via direct `mob.getPathfinder().moveTo()` calls. The goal integration described above is the planned architecture; at time of writing, FSM states do not yet call `addGoal` / `removeGoal` in their `onEnter` / `onExit` methods, and the transition wiring in `HostileStateMachine.initTransitions()` is fully commented out.

`RandomWanderGoal` exists as a prototype for goal structure; it is not yet registered by any FSM state's `onEnter`.

---

## Planned Goal Classes

One goal class per FSM state, living in the `goal/` subpackage. Goal class names, planned source states, and `shouldStayActive` semantics:

| FSM State | Goal Class | `shouldStayActive` condition |
|-----------|------------|------------------------------|
| `IdleState` | `IdleWanderGoal` | No target acquired (`nearestScannedTarget == null`) |
| `ApproachState` | `ApproachGoal` | Target still valid and within aggro range |
| `SurroundState` | `SurroundHoldGoal` | Ally count still at or above `SURROUND_MIN_ALLIES` |
| `PreAttackState` | (none — pathfinding is stopped on entry; no goal needed) | — |
| `AttackState` | `AttackChargeGoal` | Not yet within melee reach (`distSq > 6.25`) |
| `RetreatState` | `OnGuardBackoffGoal` | `retreatTimer > 0` |
| `FleeState` | `FleeGoal` | At least one `Player` within aggro radius |

`PreAttackState` does not require a goal because pathfinding is explicitly halted on entry (`mob.getPathfinder().stopPathfinding()` or equivalent) and the mob only calls `lookAt()` during the wind-up; a `LOOK`-type goal could be added if finer look-speed control is needed.

---

## Code Example: `ApproachGoal` Skeleton

The following skeleton follows the style established by `RandomWanderGoal`. It shows the full structural shape of an FSM-integrated goal: constructor, fields, all interface methods, key, and types.

```java
package btm.sword.system.entity.ai.goal;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.entity.impl.Hostile;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Pathfinds toward the {@link Hostile}'s current target at 110% movement speed.
 *
 * <p>Registered by {@code ApproachState.onEnter} and removed by {@code ApproachState.onExit}.
 * {@link #shouldStayActive()} returns {@code false} when the target becomes invalid or leaves
 * aggro range, acting as a secondary exit signal that mirrors the FSM's own transition conditions.
 */
public class ApproachGoal implements Goal<@NotNull Mob> {

    private static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "approach"));

    private static final double APPROACH_SPEED = 1.1;

    private final Mob mob;
    private final Hostile hostile;

    /**
     * Constructs an ApproachGoal for the given mob and its Sword wrapper.
     *
     * @param mob     the Bukkit {@link Mob} instance to pathfind
     * @param hostile the {@link Hostile} wrapper owning the current target reference
     */
    public ApproachGoal(final Mob mob, final Hostile hostile) {
        this.mob = mob;
        this.hostile = hostile;
    }

    /**
     * Activates immediately on registration. The FSM has already decided to enter
     * {@code ApproachState}; no additional gate is needed here.
     *
     * @return {@code true} always
     */
    @Override
    public boolean shouldActivate() {
        return true;
    }

    /**
     * Returns {@code false} when the target is no longer valid or has left aggro range,
     * causing Paper to stop this goal without waiting for the FSM transition.
     *
     * @return {@code true} while the target is valid and within aggro range
     */
    @Override
    public boolean shouldStayActive() {
        if (hostile.getCurrentTarget() == null) {
            return false;
        }
        LivingEntity targetEntity = hostile.getCurrentTarget().getLivingEntity();
        if (targetEntity == null || !targetEntity.isValid()) {
            return false;
        }
        return mob.getLocation().distanceSquared(targetEntity.getLocation())
            <= Config.Hostile.AGGRO_RANGE_SQUARED;
    }

    /**
     * Called once when this goal becomes active. Sets the mob as aware so pathfinding
     * targets are pursued, then issues the first pathfind command.
     */
    @Override
    public void start() {
        mob.setAware(true);
        pathfindToTarget();
    }

    /**
     * Called once when this goal deactivates (either via {@link #shouldStayActive()}
     * returning {@code false} or via explicit removal by the FSM state's {@code onExit}).
     */
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }

    /**
     * Called every tick while active. Re-issues the pathfind command only when outside
     * the approach threshold to avoid jitter at the boundary.
     */
    @Override
    public void tick() {
        if (hostile.getCurrentTarget() == null) {
            return;
        }
        LivingEntity targetEntity = hostile.getCurrentTarget().getLivingEntity();
        if (targetEntity == null) {
            return;
        }
        double distSq = mob.getLocation().distanceSquared(targetEntity.getLocation());
        if (distSq > Config.Hostile.APPROACH_DISTANCE_SQUARED) {
            pathfindToTarget();
        }
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }

    private void pathfindToTarget() {
        LivingEntity targetEntity = hostile.getCurrentTarget().getLivingEntity();
        if (targetEntity != null) {
            mob.getPathfinder().moveTo(targetEntity, APPROACH_SPEED);
        }
    }
}
```

Key structural points illustrated by this skeleton:

- `KEY` is `static final`; it is shared across all instances so any instance can be used to remove the goal by key.
- `shouldActivate()` returns `true` unconditionally — the FSM's decision to enter a state is the activation gate.
- `shouldStayActive()` mirrors the FSM's own transition conditions as a secondary safety net.
- `start()` and `stop()` are the correct place for any side effects (awareness flag, listener registration, initial pathfind command).
- `tick()` issues pathfinding commands; it never touches FSM state or timer fields on `Hostile`.

---

## Interactions with Other Systems

| System | Interaction |
|--------|-------------|
| `HostileStateMachine` / FSM states | Each state's `onEnter` registers a goal; `onExit` removes it. The FSM is the authoritative behaviour driver; goals handle the pathfinding layer only. |
| `MobGoalArbiter` | Provides the `MobGoals` reference used for all `addGoal` / `removeGoal` calls. |
| `Config.Hostile` | Goals read distance thresholds (`AGGRO_RANGE_SQUARED`, `APPROACH_DISTANCE_SQUARED`) and speed multipliers from `Config.Hostile`. Config is hot-reloaded; goals read the current static field value on each tick, so hot-reload takes effect immediately without re-registering goals. |
| `SwordEntityArbiter` | Goals may call `SwordEntityArbiter.get(entity)` when they need to convert a Bukkit entity handle to a `Hostile` wrapper, consistent with the pattern used by FSM states. |
| Bukkit Event System | Goals that need event listeners (see `RandomWanderGoal`) register as `Listener` in `start()` and unregister in `stop()`, scoping listener lifetime to the goal's active period. |
