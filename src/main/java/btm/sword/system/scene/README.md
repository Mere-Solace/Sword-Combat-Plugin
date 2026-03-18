# Scene System

The `btm.sword.system.scene` package implements the main-menu camera scene and the
infrastructure for any future cutscene or cinematic transition.

---

## Overview

When a player first joins the server they are placed into a **scene**: a controlled
camera experience with ambient music and HUD decorations. The system is split across
three distinct layers of responsibility:

| Layer | Class | Responsibility |
|---|---|---|
| Camera ownership | `CameraController` / `CameraSystem` | Exactly one controller drives a player's camera at any time |
| Camera motion | `BezierCameraController`, `GentleDriftCameraController` | Concrete motion strategies |
| Scene orchestration | `SceneManager` | Music loop + scene-viewer tracking |

---

## Camera Controller Lifecycle

`CameraController` is an abstract base class. Its three lifecycle hooks are:

1. **`onStart()`** — called once when the controller becomes active. Spawn entities,
   save player state, begin the tick loop.
2. **`onTick()`** — called every 50 ms (one server tick) while active. Advance camera
   position, update HUD.
3. **`onStop()`** — called once on teardown. Cancel tick tasks, despawn entities, restore
   player state.

### Single-Owner Enforcement

`SwordPlayer` holds exactly one `activeCameraController` reference. The enforcement
happens inside `CameraController.start(SwordPlayer)`:

```
player has existing controller?
  → log warning
  → call existing.stop()      ← forcibly evicts prior controller
→ set owner
→ set player.activeCameraController = this
→ call onStart()
```

`stop()` clears `player.activeCameraController` **before** calling `onStop()`, so
re-entrant stop calls (e.g. from a tick cleanup path) are safe. Subclasses must cache
the `Player` reference during `onStart()` because `owner` is nulled before `onStop()`
runs.

---

## Controller Subtypes

### `GentleDriftCameraController` — Main Menu

Used for the main-menu idle scene. The camera sits at a fixed world anchor point
(`Config.Scene.CAMERA_ANCHOR`) and applies:

- **Sinusoidal Y-offset**: `sin(tick × DRIFT_SPEED) × DRIFT_AMPLITUDE` — a gentle
  vertical bob.
- **Slow yaw drift**: `tick × YAW_DRIFT_RATE` — the camera slowly pans across the
  scene.

An invisible `ArmorStand` is placed at the drifted position each tick; the player is
put into `SPECTATOR` mode targeting it. This is the mechanism by which the player's
viewpoint follows the stand.

`HudDisplayGroup` is spawned in `onStart()`, repositioned in `onTick()` to track the
anchor, and removed in `onStop()`.

### `BezierCameraController` — Cutscenes

Used for scripted transitions. The camera travels along a `CameraPath` (cubic Bézier
curve in world space) from `t = 0` to `t = 1` over a configurable number of ticks.

- An `ArmorStand` is spawned at the curve's start position; the player spectates it.
- Each tick advances `t` by `1 / durationTicks` and teleports the stand to the new
  curve position.
- When `t ≥ 1.0`, the controller calls `stop()` on itself — the scene ends automatically.

---

## `CameraPath`

`CameraPath` wraps a `ControlVectors` and evaluates the cubic Bézier curve at any
`t ∈ [0, 1]`. The returned `Location` has its direction set to the curve's tangent
(computed via central finite difference). `ControlVectors` suppliers read from
`Config.Scene`, so hot-reloaded values are picked up without restarting.

---

## `HudDisplayGroup` Lifecycle

Managed exclusively by `GentleDriftCameraController`.

| Event | Action |
|---|---|
| `onStart()` | `hudGroup.spawn(anchor)` — title, subtitle, and two decorative `ItemDisplay` entities created |
| `onTick()` | `hudGroup.tick(anchor, tick)` — all entities teleported to anchor-relative offsets; decorative items orbit |
| `onStop()` | `hudGroup.remove()` — all entities despawned, references cleared |

HUD content (title text, subtitle, sword items) is currently hard-coded. Future work
can expose it through `Config.Scene`.

---

## `CameraSystem` vs `SceneManager`

These two classes intentionally separate concerns:

**`CameraSystem`** is a thin static utility. It has no state of its own — all camera
ownership lives on `SwordPlayer.activeCameraController`. It provides:
- `hasActiveController(SwordPlayer)` — ownership query.
- `stopController(SwordPlayer)` — safe stop with null-guard.

**`SceneManager`** is the higher-level orchestrator. It owns:
- `sceneViewers` — the set of players currently in any scene (used by input listeners
  to gate player actions).
- `seenThisSession` — per-session flag so the main menu only auto-plays on first join.
- `musicLoops` — one `TimeArbiter.TaskHandle` per player; the music track replays on a
  period derived from `Config.Scene.MENU_MUSIC_DURATION_TICKS`.

Entry point: `SceneManager.startMainMenuScene(SwordPlayer)` constructs a
`GentleDriftCameraController`, starts it, and starts the music loop.
Exit point: `SceneManager.stopScene(SwordPlayer)` stops music, stops the camera
controller, and removes the viewer flag.

---

## `AvatarDisplay` (stub)

`AvatarDisplay` is a placeholder for a player mannequin that would be visible to the
player during the main-menu scene. Because SPECTATOR mode makes the player invisible
to themselves, a mannequin built from `ItemDisplay` entities is required to show the
player's equipped gear.

**Tracked under TODO #233-avatar.** Requirements when implemented:
- `HEAD`: `ItemDisplay` with a player skull (`SkullMeta` set to the owning player's
  texture UUID).
- `CHEST` / `LEGS` / `FEET`: `ItemDisplay` entities mirroring the player's equipment
  slots.
- Transformation matrices applied to each part to approximate a standing pose.
- Fixed showcase position in the scene; `GentleDriftCameraController` oriented to face
  the mannequin.

---

## Hook Points

| Trigger | Location | Effect |
|---|---|---|
| Player first join | `SwordPlayer.onSpawn()` | `SceneManager.startMainMenuScene()` if `!hasSeenScene()` |
| Shift press | `InputListener.onSneakEvent()` | `SceneManager.onShiftInput()` → `stopScene()` |
| Forced spectator / game mode change | `PlayerListener.gameChangeEvent()` | `CameraSystem.stopController()` to clean up if scene was interrupted |

---

## Extension Points

- **New controller types**: Extend `CameraController`, implement the three hooks. Call
  `controller.start(player)` to activate — single-owner enforcement is automatic.
- **New `CameraPath` shapes**: Construct a `CameraPath` with a different `ControlVectors`.
  The path evaluates any cubic Bézier; non-Bézier shapes would require a new path class
  implementing the same `evaluate(t, world)` contract.
- **New scene types**: Add a static method to `SceneManager` (e.g. `startVictoryScene`)
  following the same pattern as `startMainMenuScene`.
- **Config-driven HUD**: Replace hard-coded strings and offsets in `HudDisplayGroup`
  with `Config.Scene` entries.
