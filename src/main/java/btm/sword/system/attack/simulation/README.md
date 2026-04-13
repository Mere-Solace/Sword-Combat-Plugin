# system/attack/simulation

This package implements a high-frequency, off-thread collision loop for the new volume-based attack
system. The simulation runs at 200 Hz on a dedicated thread; all Bukkit API calls are marshalled back
to the main thread through two thread-safe bridges.

## Class inventory

| Class | Role |
|-------|------|
| `VolumeSimulation` | Singleton 200 Hz scheduler. Drives the broad-phase/narrow-phase pipeline each tick. Manages the active attack list and owner-to-attack lookup. |
| `SimulationAttack` | Per-attack simulation state: attacker UUID, trajectory, pre-allocated volume buffer, timing window, hit set, orientation flags, and optional end callback. |
| `VolumeTrajectory` | `@FunctionalInterface`. The single method `sample(t, worldTransform, Volume)` populates a volume in place for a given normalized time. |
| `KeyframedTrajectory` | Implements `VolumeTrajectory` with Catmull-Rom position, slerp rotation, and lerped half-extents across `VolumeKeyframe` lists. |
| `SweepTrajectory` | Implements `VolumeTrajectory` using a `SweepCurve` (Catmull-Rom). Produces a capsule whose endpoints span `[t, t+DT]` on the curve. |
| `Volume` | Abstract mutable output buffer. Holds the AABB used for broad-phase culling; subclasses add shape-specific fields. |
| `ObbVolume` | `Volume` for OBB attacks: `center`, `halfExtents`, `rotation`. Used by `KeyframedTrajectory`. |
| `CapsuleVolume` | `Volume` for sweep attacks: `start`, `end`, `radius`. Used by `SweepTrajectory`. |
| `VolumeKeyframe` | Record: normalized time, local position, half-extents, rotation, shape, optional effect, and a jump flag. Holds one OBB/sphere pose in the attacker's local frame. |
| `VolumeShape` | Enum: `OBB` or `SPHERE`. Used by the editor and `VolumeKeyframe` to control visualization. **Not read by `KeyframedTrajectory.sample()` — see known issues.** |
| `SweepCurve` | Catmull-Rom control points + `RadiusPoint` profile. Provides `radius(t)` via linear interpolation. |
| `SpatialGrid` | Broad-phase 2-block cell grid. Rebuilt from scratch every tick. `insert + query` reduces collision checks from O(n²) to neighbour cells only. |
| `CollisionDetector` | Pure JOML, stateless, no Bukkit calls. `capsuleVsAabb`, `obbVsAabb` (SAT, 15 axes), `sphereVsAabb`. |
| `CollisionEvent` | Immutable record: attacker UUID, victim UUID, contact point, `HitValuePacket`. Posted on the simulation thread. |
| `CollisionEventBridge` | `ConcurrentLinkedQueue<CollisionEvent>`. Simulation thread enqueues; main thread drains via `drainToMain()`. Resolves UUIDs via `SwordEntityArbiter` and calls `victim.hit()`. |
| `EntitySnapshotMap` | `ConcurrentHashMap<UUID, EntityBoundingBoxSnapshot>`. Main thread writes every tick via `SwordEntity.onTick()`; simulation thread reads without Bukkit calls. |
| `EffectsDispatcher` | Fires `KeyframeEffect` particle/sound bursts when normalized time crosses a keyframe's `t`. Schedules Bukkit calls on the main thread via `Bukkit.getScheduler().runTask`. |
| `KeyframeEffect` | Record: list of `ParticleEffect` + optional `SoundCue`. Attached to a `VolumeKeyframe`. |
| `ParticleEffect` | Record: Bukkit `Particle`, count, offset, spread, optional `DustOptions`. |
| `SoundCue` | Record: Bukkit `Sound`, `SoundCategory`, volume, pitch. |

## Data flow

```
Main thread (50 ms / tick)
  SwordEntity.onTick()
      └─► EntitySnapshotMap.snapshot(uuid, bb, yaw, pitch)

  Combatant.launchAttackDef(def)
      ├─► def.createVolume()              → ObbVolume | CapsuleVolume
      └─► VolumeSimulation.addAttack(SimulationAttack)

  CollisionEventBridge.drainToMain()
      └─► SwordEntityArbiter.getByUuid() → victim.hit(combatant, hitValue, knockback)

Simulation thread (5 ms / tick)
  VolumeSimulation.doTick()
    ├── for each SimulationAttack:
    │     read EntitySnapshotMap (or lockedCenter)  → build worldTransform
    │     VolumeTrajectory.sample(t, worldTransform, volume)
    │     SpatialGrid.insert(uuid, aabbMin, aabbMax)
    │     EffectsDispatcher.dispatch(...)            → schedules Bukkit particle/sound on main thread
    │
    ├── for each entity snapshot:
    │     SpatialGrid.query(entity.min, entity.max) → candidates
    │     Volume.intersects(entityMin, entityMax)    → narrow phase
    │     CollisionEventBridge.post(CollisionEvent)
    │
    └── expire finished attacks → Bukkit.getScheduler().runTask(onEnd callback)
```

## Threading contract

| Thread | Writes | Reads |
|--------|--------|-------|
| Main | `EntitySnapshotMap` (every tick) | `CollisionEventBridge` queue (drain) |
| Main | `VolumeSimulation.activeAttacks` (via `addAttack`) | — |
| Simulation | `Volume` buffer (per attack) | `EntitySnapshotMap` |
| Simulation | `CollisionEventBridge` queue (post) | `VolumeSimulation.activeAttacks` |
| Simulation | `SimulationAttack.hitThisAttack` | — |

`activeAttacks` is a `CopyOnWriteArrayList` — safe for concurrent add from main thread and iteration
from the simulation thread. `hitThisAttack` is a `ConcurrentHashMap.newKeySet()` shared between
`ActiveAttack` (main thread) and `SimulationAttack` (simulation thread).

## Extension points

- **New volume primitive**: Add a `Volume` subclass with `intersects()` delegating to a new
  `CollisionDetector` method. Add a `VolumeTrajectory` implementation that populates it. Add an
  `AttackPrimitive` enum entry in `def/`.
- **Per-attack effects beyond keyframes**: Override or extend `EffectsDispatcher`, or add a per-tick
  callback field to `SimulationAttack`.
- **Sweep trajectory effects**: `EffectsDispatcher.dispatch` only handles `KeyframedTrajectory`.
  `SweepTrajectory` attacks produce no effects on crossing any t-threshold today.

## Known issues

**`VolumeShape.SPHERE` is not respected by collision detection.**
`VolumeKeyframe.shape` is stored and read by the editor and `VolumeEditorMode` for visualization.
However, `KeyframedTrajectory.sample()` ignores the shape field entirely — it always interpolates
and outputs an OBB through `ObbVolume`. A keyframe marked `SPHERE` will collide as an OBB, not as a
sphere, using whatever `halfExtents` values are set. Fix: check `kf.shape()` in `sample()` and
branch to sphere collision (via `CollisionDetector.sphereVsAabb`) when the shape is `SPHERE`.
(`KeyframedTrajectory:55`, `VolumeShape.java`)

**`EffectsDispatcher` uses `Bukkit.getScheduler()` directly.**
This bypasses `SwordScheduler`/`TimeArbiter`. Global time scaling will not affect effect timing.
(`EffectsDispatcher:45`)

**`EffectsDispatcher` calls `Bukkit.getWorlds().getFirst()` for the world.**
If there are multiple worlds, effects will always fire in the first world regardless of where the
attacker is. The world should come from the attacker's snapshot instead. (`VolumeSimulation:149`)

**Contact point is the AABB centre of the attack volume, not the actual intersection point.**
`CollisionEventBridge` receives a `contactPoint` computed as `(aabbMin + aabbMax) / 2`. For large
OBBs this is far from the actual surface of contact. (`VolumeSimulation:180-182`)

**`CollisionEventBridge.drainToMain` uses a fixed zero-knockback vector.**
All volume-based attacks apply zero knockback regardless of what the `AttackDef.knockbackFunction`
returns. The field is stored but never invoked on the collision path. (`CollisionEventBridge:70`)

**`onEnd` callback uses `Bukkit.getScheduler()` directly.**
Same bypass of `SwordScheduler` as `EffectsDispatcher`. (`VolumeSimulation:202`)
