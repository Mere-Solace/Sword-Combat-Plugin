# system/attack

This package implements all melee attack execution in Sword: Combat Evolved. Attacks define a cubic Bezier sweep path through 3D space, iterate along that path to detect hits, apply damage via `HitValuePacket`, and optionally drive a visual `ItemDisplay` entity.

## Package Contents

| Class | Description |
|-------|-------------|
| `Attack` | Base attack class. Owns the Bezier path, iteration loop via `TimeArbiter`, hit detection via `HitboxUtil.secant()`, and damage dispatch to `SwordEntity.hit()`. |
| `SweepAttack` | Extends `Attack`. Intended to manage a sweep visual display; display code is currently commented out. Inherits all hit detection from `Attack`. |
| `ItemDisplayAttack` | Extends `Attack`. Moves an existing `ItemDisplay` entity along the Bezier path each `displaySteps` iterations. Supports `displayOnly` mode to skip hit detection. |
| `UmbralBladeAttack` | Extends `ItemDisplayAttack`. Adds a distance guard (entity must be within 20 blocks of the attack location) to the secant hit check. Used for all UmbralBlade melee swings. |
| `ImpalingUmbralBladeAttack` | Extends `UmbralBladeAttack`. Replaces the secant check with a single `HitboxUtil.ray()` call. On hit, sets `blade.hitEntity` and issues `BladeRequest.IMPALE`. Cancels the iteration task immediately after the first hit. |
| `GeneratedAttackProfile` | Implements `AttackProfile` with a runtime-computed, world-space `BezierShape`. Used when the sweep geometry is built dynamically at attack time (e.g., the heavy sweep targeting a specific entity). |
| `HitValuePacket` | Bundles the five damage values (`reapedSoulfire`, `invulnerableTicks`, `shardDamage`, `toughnessDamage`, `soulfireLoss`) passed to `SwordEntity.hit()`. Each value is a `Supplier<T>` for Config hot-reload compatibility. |

## Sub-package: style

| Class | Description |
|-------|-------------|
| `AttackProfile` | Interface: `shape()`, `knockbackFunction()`, `normalVector()`. Implemented by both `AttackType` and `GeneratedAttackProfile`. |
| `AttackShape` | Interface for parametric sweep paths. `resolve(Basis, double)` returns a `Function<Double, Vector>` mapping `t ∈ [0, 1]` to world-space offsets. |
| `BezierShape` | Implements `AttackShape` using a cubic Bézier curve. Local-space (default) applies `ControlVectors.adjustToBasis` at resolve time; world-space mode skips transformation. |
| `AttackType` | Enum of named attack curves. Each entry owns a `BezierShape` and a knockback function. Also exposes `controlVectors()` for non-attack consumers (e.g., blade lunge). |
| `WeaponAttackStyle` | Enum mapping weapon item tags to their ground combo chain, aerial moves, and directional dash attacks. Read from item PDC via `KeyRegistry.ATTACK_STYLE_KEY`. |

## How an Attack Works

1. An `AttackAction` (or blade state) creates an `Attack` subclass instance with a chosen `AttackProfile` and calls `execute(combatant)`.
2. `execute()` captures the attacker, builds an entity filter, and calls `cast()` → `onRun()` → `startAttack()`.
3. `startAttack()` captures the attacker's current `Basis` (local coordinate frame) and calls `attackProfile.shape().resolve(basis, rangeMultiplier)` to obtain a `Function<Double, Vector>` path function. Shape implementations handle their own world-space transformation internally.
4. A repeating task fires every `msPerIteration` ms, advancing parameter `t` from `attackStartValue` to `attackEndValue`. Each step calls `drawAttackEffects()`, `performHitLogic()` (secant or ray intersection), and `swingTest()` (ground collision).
5. Each entity hit is passed to `SwordEntity.hit()` with the appropriate `HitValuePacket`.
6. When `curIteration >= attackIterations`, the task ends, the optional callback fires, and the next chained attack (if any) is scheduled.

## Action Layer (system/action/attack)

This package contains the static action classes that are the external entry points into the attack system:

| Class | Description |
|-------|-------------|
| `AttackAction` | Called by `InputExecutionTree` for basic combo attacks. Reads `WeaponAttackStyle`, checks grounded/aerial state, selects the `AttackProfile` for the current combo step, and creates a `SweepAttack`. |
| `DashAttackAction` | Called during dash inputs. Routes to the UmbralBlade quick-attack shortcut if conditions are met, otherwise creates a directional `Attack` with the dash origin locked to the dash direction. |
| `PunchAction` | Unarmed attack. Uses `HitboxUtil.ray()` directly from the chest position; no Bezier path. |

## Extension

- Add a new named curve: add an entry to `AttackType` with a `ControlVectors` (automatically wrapped in `BezierShape`).
- Add a new weapon category: add an entry to `WeaponAttackStyle` with the appropriate chain of `AttackType` references.
- Add a new shape type (arc, line, polygon, etc.): implement `AttackShape` and return the appropriate path function from `resolve()`.
- Custom hit geometry: extend `Attack` and override `collectHitEntities()`.
- Custom damage: override `hit()` and use a different `HitValuePacket` from `Prefab.Attacks`.

For full architecture details see `docs/systems/attack-system.md`.
