# Scheduling System

## Overview

The scheduling system provides time-aware task management that integrates with a global time scale for slow-motion and speed-up effects. It wraps Java's `ScheduledExecutorService` and Bukkit's scheduler to ensure all game logic runs on the main thread.

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `TimeArbiter` | `system/control/TimeArbiter.java` | Central time-scale manager. Maintains two task registries (time-bound and time-independent). Provides factory methods for creating repeating tasks. Manages global time scaling and its effects on entity movement speed. |
| `SwordScheduler` | `system/control/SwordScheduler.java` | Simple utility: `runBukkitTaskLater()` (async delay then main-thread execution), `runBukkitTask()` (immediate main-thread), `runConsumerNextTick()`. |
| `PredicateRunnablePair` | `system/control/PredicateRunnablePair.java` | Pairs a `Predicate` (termination condition) with a `Runnable` (cleanup action). Used as conditional callbacks for task termination. |
| `EntityController` | `system/control/EntityController.java` | Entity-level control utilities (teleport). |
| `TimeArbiter.TaskHandle` | Inner class | Wraps a `ScheduledFuture<?>` with pause/resume/cancel semantics, restart-on-timescale-change support, and debug info (calling class/method). |

## Task Types

### Time-Bound Tasks

Created via `runTimeBoundBukkitTaskOnTimer()`. Their period is divided by `GLOBAL_TIME_SCALE`:

- At scale 1.0: runs at normal speed
- At scale 0.5: runs at half speed (period doubled)
- At scale 2.0: runs at double speed (period halved)

Used for: attack iterations, resource regeneration, soulfire transfer, combat animations.

### Time-Independent Tasks

Created via `runTimeIndependentBukkitTaskOnTimer()`. Period is unaffected by time scale.

Used for: entity ticking, affliction processing, input timeouts.

### Fixed-Iteration Tasks

Created via `runFixedIterationTaskTimer()`. Runs for a set number of iterations then auto-terminates.

Used for: dash particle effects.

## Execution Model

All tasks use a two-stage execution:

1. **Async scheduling** via `Sword.getScheduler()` (a `ScheduledExecutorService`)
2. **Main-thread execution** via `Bukkit.getScheduler().runTask()`

This ensures timing precision from the async scheduler while maintaining Bukkit API thread safety.

## Task Lifecycle

Each task iteration:

1. Check if marked for restart (time scale changed) -> reschedule if needed
2. Check if cancelled -> return
3. Check if paused (time-bound only) -> run paused runnable if present
4. Run pre-check runnable
5. Evaluate all `PredicateRunnablePair` conditions -> if any true, execute its runnable and cancel the task
6. Run post-check runnable

## Global Time Scale

`setGlobalTimeScale(double)`:

1. Clamps to [0.0, 2.0]
2. Marks all time-bound tasks for restart
3. Applies movement speed effects to all entities via potion effects (Slowness for scale < 1, Speed for scale > 1)
4. Stores the scale for future task creation

## Dependencies

- **Sword.getScheduler()** -- `ScheduledExecutorService` created in `Sword.onEnable()`
- **Bukkit scheduler** -- For main-thread task execution
- **SwordEntityArbiter** -- For applying speed effects to all entities

## Known Limitations

- `SwordScheduler.runBukkitTaskLater()` uses `Sword.getScheduler()` for timing but does not participate in the time-scale system. Only `TimeArbiter`-created tasks respect global time scale.
- The potion-based movement speed system is coarse (5 discrete levels per direction) and clears ALL potion effects when changing speed, which could interfere with gameplay effects.
- Tasks cannot be paused individually in a meaningful way since the `paused` flag is only checked for time-bound tasks.
