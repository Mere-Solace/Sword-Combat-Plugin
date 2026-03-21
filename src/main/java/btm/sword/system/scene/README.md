# Scene System

The `btm.sword.system.scene` package implements the camera and cutscene infrastructure.
The `animation` sub-package adds DEU/BDEngine animation playback on top of the camera stack.

---

## Package Layout

```
scene/
├── CameraController.java          — abstract base; lifecycle and single-owner enforcement
├── CameraSystem.java              — thin static utility (ownership queries, safe stop)
├── CameraService.java             — packet attach/detach (delegates to PacketAdapter)
├── CameraSession.java             — per-attachment handle; single-use
├── PacketAdapter.java             — ProtocolLib wrapper for ClientboundSetCameraPacket
├── BezierCameraController.java    — drives camera along a CameraPath (Bézier curve)
├── CameraPath.java                — cubic Bézier world-space trajectory record
├── DEUAnimationController.java    — plays a DEU/BDE animation as a cutscene
└── animation/
    ├── AnimationDef.java          — immutable descriptor loaded from animations.yml
    ├── AnimationRegistry.java     — static registry; load, reload, setLoop
    └── CutsceneInputHandler.java  — routes inputs while ActivationContext.CUTSCENE is active
```

---

## Camera Stack

The packet camera stack has four layers, each with a single responsibility:

| Layer | Class | Responsibility |
|---|---|---|
| Ownership lifecycle | `CameraController` / `CameraSystem` | Exactly one controller per player at a time |
| Concrete motion | `BezierCameraController`, `DEUAnimationController` | Drive the camera for a specific use case |
| Packet attach/detach | `CameraService` / `CameraSession` | Send `ClientboundSetCameraPacket`; return a handle |
| NMS wrapper | `PacketAdapter` | ProtocolLib call; reset by targeting player's own entity ID |

Packet camera does **not** require spectator mode. The player's game mode is preserved.
`BezierCameraController` falls back to spectator-mode targeting only when
`CameraService.isAvailable()` returns `false` (ProtocolLib not loaded).

---

## `CameraController` Lifecycle

`CameraController` is the abstract base for all controllers. Its three hooks are:

1. **`onStart()`** — called once when the controller becomes active. Spawn entities, attach camera, start tick loops.
2. **`onTick()`** — called every 50 ms while active. Advance camera, update state.
3. **`onStop()`** — called once on teardown. Cancel tasks, detach camera, clean up entities.

### Single-Owner Enforcement

`start(SwordPlayer)` is `final`. The sequence is:

```
player has existing controller?
  → log warning
  → call existing.stop()
→ set owner
→ set player.activeCameraController = this
→ call onStart()
```

`stop()` clears `player.activeCameraController` **before** calling `onStop()`, making
re-entrant stop calls safe. Subclasses must cache the player reference in `onStart()`
because `owner` is nulled before `onStop()` runs.

---

## Controller Subtypes

### `BezierCameraController`

Drives the camera along a `CameraPath` (cubic Bézier curve in world space) over a
configurable number of ticks.

- Spawns an invisible `ArmorStand` at the path start position.
- Attaches the player's camera to the stand via `CameraService` (1-tick delay for
  client-side entity tracking).
- Each tick advances `t` by `1 / durationTicks` and teleports the stand to
  `path.evaluate(t, world)`.
- When `t >= 1.0`, calls `stop()` on itself — the controller self-terminates.

Intended for scripted cinematic transitions where the path is authored ahead of time.

### `DEUAnimationController`

Plays a DEU/BDEngine animation as a cutscene for its owner player.

- On start: loads the DEU `DisplayEntityGroup` by `groupTag`, creates a
  `PacketDisplayEntityGroup` at the player's location, shows it only to that player,
  and starts a `DisplayAnimator`.
- If `attachCamera` is `true`: also calls `DisplayAnimator.playCamera` so the player's
  viewpoint follows the animation's embedded camera track.
- Sets `ActivationContext.CUTSCENE` on the player, blocking all combat inputs.
- `onTick()` is a no-op — DEU manages its own animation loop internally.
- On stop: stops animator, hides and unregisters the packet group, restores
  `ActivationContext.NORMAL`.

```java
AnimationDef def = AnimationRegistry.get("main_menu").orElseThrow();
new DEUAnimationController(def, /* attachCamera */ true, /* loop */ true).start(player);
```

---

## `CameraPath`

`CameraPath` is a record wrapping a `ControlVectors`. `evaluate(t, world)` returns a
`Location` on the cubic Bézier curve with yaw/pitch aligned to the tangent (central
finite difference). Used by `BezierCameraController` to drive the ArmorStand position.

---

## `CameraSystem`

Thin static utility. No state of its own — all camera ownership lives on `SwordPlayer`.

| Method | Purpose |
|---|---|
| `hasActiveController(SwordPlayer)` | Ownership query |
| `stopController(SwordPlayer)` | Safe stop with null-guard |

---

## Animation Sub-Package

### `AnimationDef`

Immutable record: `key`, `groupTag`, `animTag`, `defaultLoop`. Loaded from `animations.yml`.
`key` is the logical identifier used throughout Sword code. `groupTag` and `animTag` are
the DEU tags identifying the data on disk.

### `AnimationRegistry`

Static registry initialized once via `AnimationRegistry.initialize(plugin)` in `Sword.onEnable()`.

| Method | Behavior |
|---|---|
| `get(key)` | Returns `Optional<AnimationDef>` |
| `all()` | Returns unmodifiable collection of all defs |
| `setLoop(key, loop)` | Updates in-memory def and persists to `animations.yml` immediately |
| `isLooping(key)` | Reads the current loop flag for a key |
| `reload()` | Re-parses `animations.yml`; safe to call at any time |

On each `reload()`, the registry scans `plugins/DisplayEntityUtils/bdenginedatapacks/`
for `.zip` files that have no corresponding `.deg` file in the `groups/` directory and
logs conversion instructions for each unconverted pack.

### `CutsceneInputHandler`

Called from `SwordPlayer.act(InputType)` before the normal trie traversal when
`ActivationContext.CUTSCENE` is active.

| Input | Effect |
|---|---|
| `SHIFT` / `SHIFT_TAP` | Calls `CameraSystem.stopController(player)`; restores `ActivationContext.NORMAL` |
| All others | Silently suppressed |

---

## `animations.yml` Format

```yaml
animations:
  main_menu:
    group: "main_menu_group"
    anim:  "main_menu_anim"
    loop:  true
  slash_test_default:
    group: "slash_test"
    anim:  "slash_default"
    loop:  false
```

Keys under `animations:` become the logical keys used in `AnimationRegistry.get()` and
`Config.Animation` string entries.

---

## Activation Context Integration

`ActivationContext.CUTSCENE` is set by `DEUAnimationController.onStart()` and cleared
in `onStop()`. While active, `CutsceneInputHandler` intercepts all inputs before the
trie, so no combat actions or abilities fire during a cutscene. Shift exits immediately.

---

## Extension Points

- **New controller types**: Extend `CameraController`, implement the three hooks, call
  `controller.start(player)`. Single-owner enforcement is automatic.
- **New Bézier paths**: Construct a `CameraPath` with a different `ControlVectors` and
  pass it to `BezierCameraController`.
- **New animations**: Add entries to `animations.yml` (or `Config.Animation` string
  constants pointing to keys), then reference via `AnimationRegistry.get(key)` and
  pass the def to `DEUAnimationController`.
- **Input handling during cutscenes**: Extend `CutsceneInputHandler.handle()` to map
  additional `InputType` values to dialogue or chapter-skip actions.
