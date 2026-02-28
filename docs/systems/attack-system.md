# Attack System

## Overview

The attack system drives all melee combat in Sword: Combat Evolved. It defines how weapon swings are shaped in 3D space using cubic Bezier curves, how those shapes are sampled for hit detection each tick, how damage values are packaged and applied to targets, and how different weapon styles and movement states select different attack configurations.

The system is composed of three layers:

1. **Math layer** — `ControlVectors`, `Basis`, and `BezierUtil` define a parametric path through 3D space, oriented to the attacker's current facing direction.
2. **Attack execution layer** — `Attack` and its subclasses execute that path iteratively over time, performing hit detection and effects on each iteration.
3. **Action layer** — `AttackAction`, `DashAttackAction`, and `PunchAction` select the correct attack based on weapon style, combo step, and movement state, then call `execute()`.

---

## Key Classes

| Class | Package | Role |
|-------|---------|------|
| `Attack` | `system/attack` | Base class for all attacks. Owns the Bezier path function, iteration loop, hit collection, and damage dispatch. |
| `SweepAttack` | `system/attack` | Extends `Attack`. Intended to drive a visual sweep display effect; visual logic currently commented out; inherits all hit detection from `Attack`. |
| `ItemDisplayAttack` | `system/attack` | Extends `Attack`. Moves an existing `ItemDisplay` entity along the Bezier path. Supports a `displayOnly` flag that suppresses hit detection. |
| `UmbralBladeAttack` | `system/attack` | Extends `ItemDisplayAttack`. Used for all UmbralBlade melee swings; adds a tighter hit filter (entity must be within 20 blocks of the attack location). |
| `ImpalingUmbralBladeAttack` | `system/attack` | Extends `UmbralBladeAttack`. Uses `HitboxUtil.ray()` instead of `secant()` for a single-target piercing check; on hit, sets `blade.hitEntity` and issues `BladeRequest.IMPALE`. |
| `GeneratedAttackProfile` | `system/attack` | Implements `AttackProfile` with runtime-generated `ControlVectors` (used in the heavy sweep where the curve is built around the target at the moment of attack). |
| `HitValuePacket` | `system/attack` | Bundles the five damage values passed to `SwordEntity.hit()`. Each value is a `Supplier<T>` to remain live with Config hot-reload. |
| `AttackProfile` | `system/attack/style` | Interface: `controlVectors()`, `knockbackFunction()`, `normalVector()`. |
| `AttackType` | `system/attack/style` | Enum implementing `AttackProfile`. Defines all named attack curves as four-point `ControlVectors` and a `knockbackFunction`. |
| `WeaponAttackStyle` | `system/attack/style` | Enum associating weapon items with their ground combo chain, aerial moves, and directional dash attacks. Read from item PDC. |
| `AttackAction` | `system/action/attack` | Static entry point for player basic attacks. Reads `WeaponAttackStyle`, checks grounded/aerial, selects profile, and creates `SweepAttack`. |
| `DashAttackAction` | `system/action/attack` | Static entry point for dash attacks. Handles the UmbralBlade quick-attack shortcut and generic dash-direction attacks. |
| `PunchAction` | `system/action/attack` | Static entry point for unarmed attacks. Uses `HitboxUtil.ray()` directly without a Bezier path. |
| `BezierUtil` | `utility/math` | Provides `cubicBezier3D(ControlVectors)` returning a `Function<Double, Vector>`. |
| `ControlVectors` | `utility/math` | Record holding four `Vector` points: `start`, `end`, `c1` (first control), `c2` (second control). Immutable (accessors return clones). |
| `Basis` | `utility/math` | Encapsulates a right-handed orthonormal basis (`right`, `up`, `forward`) derived from a `Location` and optional pitch. |
| `HitboxUtil` | `utility/entity` | Static hitbox utilities: `secant()`, `ray()`, `line()`, `sphere()`, etc. |
| `Prefab.Attacks` | `utility/Prefab` | Named `HitValuePacket` constants (`basicAttack`, `punch`, `grabHit`, `thrownWeapon`, `umbralItemDisplayAttack`, `defaultMobHit`). |

---

## Architecture

### Bezier Sweep Path

Every `Attack` traces a cubic Bezier curve in the attacker's local coordinate space. The sequence is:

1. **Basis construction** — At `startAttack()` time, `generateBezierFunction()` calls `VectorUtil.getBasis()` or `VectorUtil.getBasisWithoutPitch()` on the attacker's eye location, producing a `Basis` with `right`, `up`, and `forward` unit vectors oriented to where the player is currently looking.

2. **Control vector adjustment** — The `ControlVectors` from the `AttackProfile` are defined in a canonical space where forward is +Z, right is +X, up is +Y. `ControlVectors.adjustToBasis(basis, rangeMultiplier)` transforms all four points into world space by calling `VectorUtil.transformWithNewBasis()` on each, then multiplying by the range multiplier from Config. `GeneratedAttackProfile` skips this step because its control vectors are already built in world space.

3. **Bezier function** — `BezierUtil.cubicBezier3D(adjusted)` returns a closure `Function<Double, Vector>` implementing the standard cubic Bezier formula:

   ```
   B(t) = (1-t)^3 * P0  +  3*(1-t)^2*t * P1  +  3*(1-t)*t^2 * P2  +  t^3 * P3
   ```

   where `P0 = start`, `P1 = c1`, `P2 = c2`, `P3 = end`.

4. **Origin** — The attack origin defaults to the attacker's chest position (`attackingEntity.getLocation().add(attacker.getChestVector())`). Dash attacks and UmbralBlade attacks may override origin via `setOrigin()` or `setOriginOfAll()`.

5. **Iteration** — `startAttack()` schedules a repeating task via `TimeArbiter.runTimeBoundBukkitTaskOnTimer()`. Each iteration:
   - Increments `curIteration` atomically.
   - Evaluates `weaponPathFunction.apply(t)` where `t = attackStartValue + (interpolationStep * iteration)`.
   - Sets `attackLocation = origin + cur` (world position of the current Bezier point).
   - Computes `to = cur - prev` (the displacement vector for this step, used for knockback and ground-collision detection).
   - Calls `drawAttackEffects()`, `performHitLogic()`, `swingTest()`.
   - Updates `prev = cur` after each tick.

The effect is that the attack origin stays fixed at the chest, and `attackLocation` sweeps through a curve in world space derived from the player's facing at the moment of attack. Because the basis is captured once at `startAttack()`, rotating mid-swing does not alter the path.

### Hit Detection

`Attack.collectHitEntities()` calls `HitboxUtil.secant(origin, attackLocation, secantRadius, filter)`. The secant method steps from `origin` to `attackLocation` in increments of `secantRadius` (configured under `Config.Combat.HITBOXES_SECANT_RADIUS`), calling `getNearbyLivingEntities(thickness)` at each point, and collects all matching entities that pass the entity filter (not the attacker, valid, living).

`UmbralBladeAttack` overrides `collectHitEntities()` to add a secondary distance guard: entities must be within 20 blocks of `attackLocation` to register.

`ImpalingUmbralBladeAttack` replaces the secant entirely with `HitboxUtil.ray()`, which wraps Bukkit's `World.rayTraceEntities()`. It fires a ray of radius 0.75 from `attackLocation` in the direction of `to` (normalized), up to 2 blocks. This finds one target precisely and then immediately cancels the attack iteration task, stopping further hits.

Each entity is only hit once per attack instance. `hitDuringAttack` (a `HashSet<LivingEntity>`) records all entities struck during the current attack, and `applyHitEffects()` skips any target already in the set.

### Damage Application

When `hit()` is called, the attack dispatches to `SwordEntity.hit(Combatant source, HitValuePacket v, Vector knockbackVelocity)`. The base `Attack.hit()` uses `Prefab.Attacks.basicAttack`. `ItemDisplayAttack.hit()` uses `Prefab.Attacks.umbralItemDisplayAttack`.

`SwordEntity.hit()` unpacks the five values from the `HitValuePacket` and applies:

- **`reapedSoulfire`** — soulfire transferred from the target to the attacker via `SoulfireManager.transferSoulfire()`.
- **`invulnerableTicks`** — duration the target is immune to further hits.
- **`shardDamage`** — direct health (Shards resource) reduction.
- **`toughnessDamage`** — reduction to the Toughness (shield) resource.
- **`soulfireLoss`** — soulfire removed from the target (independent of what the attacker gains).
- **`knockbackVelocity`** — velocity applied to the target entity.

The `HitValuePacket` fields are `Supplier<T>` so they re-read live config values on every call, ensuring hot-reloaded damage numbers take effect immediately.

### Ground Collision

`Attack.swingTest()` fires a block ray trace each iteration from `attackLocation` in direction `to`. If a block is hit, block-dust particles spawn and a `COLLIDE` particle plays at the impact point. Damage reduction on ground contact is noted as a future feature.

### Callback and Chaining

Attacks support two post-completion hooks:

- **`setCallback(Runnable, int msDelay)`** — Schedules a `Runnable` after the attack completes, used by UmbralBlade attack states to signal `attackCompleted = true`.
- **`setNextAttack(Attack, int msDelay)`** — Links attacks sequentially. When the current attack ends, `nextAttack.execute(attacker)` is called after the specified delay. This is how windup-then-strike sequences are constructed in `UmbralBlade.loadBasicAttacks()` and `performWideUmbralSweepAttack()`.

`setOriginOfAll(Location)` propagates a fixed origin through the entire chain so a multi-phase attack does not drift if the attacker moves between phases.

### onEntityHitInstructions and onAttackConnectInstructions

`setOnEntityHitInstructions(Consumer<SwordEntity>)` registers a callback called once per entity hit. Used to spawn bleed particles on UmbralBlade attacks.

`setAttackConnectInstructions(ConsumerToConsumePair<?>...)` is a varargs array of pre-bound consumers executed when any entity is hit. Used in `AttackAction.basicSlash()` to damage the held item stack by 20 durability per hit.

---

## Data Flow: basicAttack Entry Point

```
InputExecutionTree.step()  ->  InputAction.cast()
  -> AttackAction.basicAttack(executor, comboStep)
     reads item PDC -> WeaponAttackStyle.fromString()
     if PUNCH         -> PunchAction.throwPunch()
     if grounded      -> basicSlash(executor, item, attackChain.get(comboStep-1), true)
     if airborne      -> basicSlash(executor, item, neutralAir/downAir, false)

basicSlash():
  new SweepAttack(item, profile, orientWithPitch, 40, 60, 0.1, 0.9)
    .setAttackConnectInstructions(...)
    .execute(executor)

Attack.execute():
  attacker = executor
  filter = entity not self, is LivingEntity, isValid
  cast() -> onRun()
    setTimeOfLastAttack
    calcValueReductive(FINESSE) -> cooldown
    startAttack()
      applySelfAttackEffects()
      playSwingSoundEffects()
      generateBezierFunction()   <- captures current basis
      determineOrigin()          <- defaults to chest
      prev = bezier(start - step)
      schedule TimerTask:
        each iteration: bezier(t) -> cur, hit detection, particles, ground check
        on end: handleCallback(), endingLogic(), nextAttack (if any)
```

---

## Attack Types and the WeaponAttackStyle Hierarchy

`AttackProfile` is an interface with three members. `AttackType` is an enum that implements it with hard-coded named curves. `WeaponAttackStyle` groups `AttackType` values into the set of attacks available for a given weapon category.

Currently only `SLASH` is fully wired with a three-step ground combo (`SLASH1`, `SLASH2`, `SLASH3`), aerial variants (`N_AIR`, `D_AIR`), and four directional dash attacks. `PUNCH`, `THRUST`, and `BASH` are defined but do not yet have attack chains.

Down-air detection uses the dot product of the player's look direction with the world up vector. If `dot < Config.Combat.ATTACKS_DOWN_AIR_THRESHOLD`, the down-air profile (`D_AIR`) is selected; otherwise neutral air (`N_AIR`).

The `WeaponAttackStyle` is read from the item's persistent data container using `KeyRegistry.ATTACK_STYLE_KEY`. Untagged items fall through to `PUNCH`.

---

## Prefab.Attacks: Named HitValuePackets

| Constant | reapedSoulfire | invulnTicks | shardDamage | toughnessDmg | soulfireLoss |
|----------|---------------|-------------|-------------|--------------|--------------|
| `basicAttack` | 5.0 | config | config | config | config |
| `punch` | 7.5 | 2 | 1 | 5.0 | 5.0 |
| `grabHit` | 1.0 | 0 | 0 | 5.0 | 5.0 |
| `thrownWeapon` | 0.0 | config | config | config | config |
| `umbralItemDisplayAttack` | 0.0 | 5 | 1 | 15.0 | 10.0 |
| `defaultMobHit` | 5.0 | 15 | 1 | 10.0 | 10.0 |

Config-referenced values are read live via `Supplier` on each call.

---

## Extension Points

**Adding a new weapon style** — Add an entry to `WeaponAttackStyle` with the appropriate `AttackProfile` references. Tag items with `KeyRegistry.ATTACK_STYLE_KEY`. No changes to `AttackAction` are required for ground combos; only the switch-case needs extension for styles that need special dispatch logic.

**Adding a new named attack curve** — Add an entry to the `AttackType` enum with the four `ControlVectors` and a knockback function.

**Adding a new attack subclass** — Extend `Attack` (or `ItemDisplayAttack`). Override `collectHitEntities()` for custom hit geometry, `hit()` for different damage packets, `drawAttackEffects()` for custom visuals, and `startupLogic()`/`endingLogic()` for setup and teardown.

**Custom knockback** — The `knockbackFunction` in `AttackProfile` is a `Function<Attack, Vector>`. It receives the live `Attack` instance, so it can reference `getRightVector()`, `getForwardVector()`, `getTo()`, or any other state at the moment of impact.

---

## Interactions with Other Systems

- **InputExecutionTree** — Calls `AttackAction.basicAttack(executor, comboStep)` when a LEFT input combo resolves on a SLASH weapon. The `comboStep` (1, 2, or 3) selects which `AttackType` from `WeaponAttackStyle.attacks()`. Aerial attacks call `swordPlayer.resetTree()` to prevent combo continuation.
- **UmbralBlade** — `AttackingQuickState.onEnter()` calls `blade.performSimpleAttack()`, and `AttackingHeavyState.onEnter()` calls `blade.performWideUmbralSweepAttack()`. These methods construct `UmbralBladeAttack` instances with an `attackEndCallback` that sets `blade.attackCompleted = true`, which triggers the `AttackingQuick/Heavy -> Recalling` transition. `ImpalingUmbralBladeAttack` issues `BladeRequest.IMPALE` directly on hit.
- **SwordEntity** — Final recipient of damage via `hit(Combatant, HitValuePacket, Vector)`. Manages invulnerability window, shard loss, toughness break, and soulfire transfer.
- **TimeArbiter / SwordScheduler** — All iteration timing and post-attack delays go through these. Direct use of `Bukkit.getScheduler()` is prohibited.
- **Config** — `ATTACK_CLASS_TIMING_*`, `ATTACKS_CAST_TIMING_*`, `HITBOXES_SECANT_RADIUS`, `ATTACK_CLASS_HIT_*`, `THROWN_DAMAGE_*` are all hot-reloadable entries.

---

## Known Limitations

- `SweepAttack` visual logic (the sweep display entity) is fully commented out. The class currently behaves identically to `Attack`.
- Attack iteration uses `HashMap`-backed transition maps (in state machines); the attack system itself is sequential but depends on the `TimeArbiter` timer, which is subject to server tick jitter.
- The `cast()` method in `Attack` has a TODO (#139) noting that the separation between input-level casting duration and attack execution is not yet dynamic.
- `comboStep` in `AttackAction.basicAttack()` is 1-indexed and clamped to the length of the attack chain; callers must ensure the value is in range.
- Aerial attacks do not support full combo chains. `resetTree()` is called immediately so each aerial input produces exactly one attack.
