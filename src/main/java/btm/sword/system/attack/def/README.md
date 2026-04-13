# system/attack/def

This package defines the data model for the new volume-based attack system. An `AttackDef` is a fully
specified, immutable attack description — its timing, volume trajectory, hit values, and knockback
function. `AttackDef` instances are the authoritative record of what an attack _is_; they are created
once and shared freely across threads.

## Class inventory

| Class | Role |
|-------|------|
| `AttackDef` | Immutable attack description built with a fluent `Builder`. Owns the trajectory, timing, hit packet, and orientation flags. |
| `AttackPrimitive` | Enum with two values — `VOLUME` (OBB) and `SWEEP` (capsule). Each entry knows how to allocate the correct `Volume` subtype via `createVolume()`. |
| `AttackDefSerializer` | Converts `AttackDef` ↔ Bukkit `YamlConfiguration`. SWEEP reads/writes a `curve` section; VOLUME reads/writes a `keyframes` list. Knockback functions are not serializable and always default to zero on load. |
| `AttackRegistry` | Static `ConcurrentHashMap<String, AttackDef>` registry. Supports per-file load (`loadAll`), directory scan (`loadDirectory`), and directory sync (`syncDirectory` — removes stale entries then reloads). |

## Data flow

```
attacks/*.yml
      │
      ▼  (Sword.onEnable / /sword reload)
AttackRegistry.loadDirectory()
      │  AttackDefSerializer.load()
      ▼
ConcurrentHashMap<String, AttackDef>   ← thread-safe; read from any thread
      │
      ▼  Combatant.launchAttackDef(def)
AttackPrimitive.createVolume()  ──►  ObbVolume / CapsuleVolume
```

## How an AttackDef is built

Use the fluent builder:

```java
AttackDef def = new AttackDef.Builder("heavy_slash")
    .duration(500)
    .keyframes(keyframeList)          // sets type = VOLUME automatically
    .onHit(hitValuePacket)
    .knockback((contact, attacker) -> new Vector3f(...))
    .build();
```

Shorthand setters `keyframes(list)` and `sweep(curve)` both implicitly set the `type` field and
construct the appropriate `VolumeTrajectory`. Call `trajectory(impl)` with an explicit `type()` for
custom trajectory implementations.

## Extension points

- **New trajectory type**: Implement `VolumeTrajectory` (in `simulation/`), add a new `AttackPrimitive`
  enum entry with the corresponding `Volume` subtype, and add a builder shorthand method.
- **YAML fields**: Add to both `AttackDefSerializer.load()` and `AttackDefSerializer.save()` symmetrically
  or the YAML round-trip will drop the field.
- **Runtime-only attacks** (e.g., procedurally generated): Build an `AttackDef` in code and register it
  with `AttackRegistry.register(def)` directly — no YAML file required.

## Known issues

- `AttackDefSerializer.saveHitValue` writes values to a malformed path: the base `path` variable already
  includes `.hit-value` in the `save()` method, but `saveHitValue` appends field names without
  a parent key — the path ends up as `attacks.id.hit-value.shard-damage` when it should be
  `attacks.id.hit-value.shard-damage`. The resulting YAML is nested one level too high. **Actual bug
  to verify:** trace the path string through `save()` → `saveHitValue()`. (See `AttackDefSerializer:226`.)
- `knockbackFunction` is always zeroed on YAML load (documented). There is no runtime-injectable
  knockback registry to compensate — attacks loaded from disk have no knockback.
- `syncDirectory` reads every YAML file twice: once to build the stale-id removal set, then again inside
  `loadDirectory`. A single pass would suffice.

## Interactions

| Dependency | Direction | How |
|------------|-----------|-----|
| `simulation/` | outward | `AttackPrimitive.createVolume()` produces `ObbVolume` / `CapsuleVolume` |
| `simulation/` | outward | `AttackDef.trajectory` holds `KeyframedTrajectory` or `SweepTrajectory` |
| `Combatant` | inward | `launchAttackDef(def)` reads all `AttackDef` fields |
| `Sword.onEnable` | inward | `AttackRegistry.loadDirectory` scans the plugin data folder |
| `dev/` | inward | `AttackDevSession`, `SaveConfirmDialog`, `SweepRecordingAction` all create and register `AttackDef` instances |
