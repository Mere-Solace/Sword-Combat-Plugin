# Combat Mechanics

## Overview

The combat system implements a Bezier-curve-driven melee attack system with combo chains, hitbox detection, damage packets, knockback, toughness breaking, soulfire transfer, and afflictions.

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `Attack` | `system/attack/Attack.java` | Core attack class. Generates a cubic Bezier path, iterates along it on a timer, performs hit detection via `HitboxUtil.secant()`, and applies damage via `SwordEntity.hit()`. Supports chained attacks via `nextAttack`. |
| `SweepAttack` | `system/attack/SweepAttack.java` | Extends `Attack` with visual sweep display using `ItemDisplay` entities (currently commented out). |
| `UmbralBladeAttack` | `system/attack/UmbralBladeAttack.java` | Umbral-blade-specific attack variant. |
| `ItemDisplayAttack` | `system/attack/ItemDisplayAttack.java` | Attack with visual item display tracking. |
| `ImpalingUmbralBladeAttack` | `system/attack/ImpalingUmbralBladeAttack.java` | Impalement attack variant. |
| `AttackProfile` | `system/attack/style/AttackProfile.java` | Defines attack shape via `ControlVectors`, knockback function, and normal vector. |
| `AttackType` | `system/attack/style/AttackType.java` | Enum classifying attacks. |
| `WeaponAttackStyle` | `system/attack/style/WeaponAttackStyle.java` | Maps weapons to their attack profiles. |
| `HitValuePacket` | `system/attack/HitValuePacket.java` | Bundles damage values using `Supplier<>` for lazy evaluation: reapedSoulfire, invulnerableTicks, shardDamage, toughnessDamage, soulfireLoss. |
| `AttackAction` | `system/action/attack/AttackAction.java` | Static methods for creating and executing basic attacks. |
| `DashAttackAction` | `system/action/attack/DashAttackAction.java` | Dash + attack combination. |
| `PunchAction` | `system/action/attack/PunchAction.java` | Unarmed attack. |

## Attack Execution Flow

1. An `Attack` is created with an `AttackProfile` (containing `ControlVectors`) and timing parameters.
2. `execute(Combatant)` stores the attacker, builds a filter predicate, and calls `cast()`.
3. `cast()` calls `onRun()` which:
   - Records attack time on the attacker
   - Calculates attack duration based on FINESSE stat
   - Calls `startAttack()`
4. `startAttack()`:
   - Computes interpolation parameters (step size, ms per iteration)
   - Generates the Bezier function from control vectors oriented to the attacker's basis
   - Schedules a repeating timer via `TimeArbiter.runTimeBoundBukkitTaskOnTimer()`
5. Each iteration:
   - Evaluates the Bezier function at the current interpolation value
   - Computes the attack location (origin + Bezier vector)
   - Draws particle effects at the attack location
   - Performs hit detection: `HitboxUtil.secant(origin, attackLocation, secantRadius, filter)`
   - Applies damage to any hit entities not already hit during this attack
   - Checks for ground collision via `rayTraceBlocks`
6. On completion:
   - Executes callback if set
   - Chains to `nextAttack` if it exists

## Bezier Curve Attack Paths

Attacks follow cubic Bezier curves defined by 4 control vectors (start, end, c1, c2). These are transformed into the attacker's local coordinate space using `Basis` vectors (right, up, forward), creating attack arcs that follow the player's facing direction.

The `BezierUtil.cubicBezier3D()` function returns `t -> Vector` where t ranges from `attackStartValue` to `attackEndValue`.

## Hitbox Detection

`HitboxUtil` provides multiple detection methods:

- **secant** -- Steps along a line between two points, checking `getNearbyLivingEntities()` at each step. Used for attack sweeps.
- **line** -- Similar to secant but from a point along a direction vector.
- **sphere** -- Radius check around a point.
- **ray** -- Bukkit's `rayTraceEntities()` for single-target precision.
- **sphereAtRayHit** -- Sphere at the point where a ray hits.

## Damage Model

The toughness-first damage model works as follows:

1. Toughness absorbs damage first. While toughness > 0, shards (HP) cannot be reduced.
2. When toughness reaches 0, it "breaks" with visual effects and enters a weakened regen state.
3. While broken, shard damage is applied normally.
4. After enough shards are lost (`SHARDS_LOST_PERCENT_TOUGHNESS_RESET * maxShards`), toughness recharges to `TOUGHNESS_RECHARGE_PERCENT`.
5. If shards reach 0, the entity dies.

## Soulfire Transfer

`SoulfireManager.transferSoulfire()` creates animated particle packets that travel from the hit entity to the attacker. Each packet is a time-bound task that interpolates from a random initial direction toward the receiver's chest location. On arrival, the soulfire amount is added to the receiver's resource.

## Afflictions

`Affliction` is an abstract timed debuff system. Subclasses implement `onApply()` and `end()`. The base class handles:

- Duration tracking with tick counting
- Automatic reapplication or extension if already active
- Entity validity checks
- Currently `GroundedAffliction` is the only concrete implementation.

## Dependencies

- **EntityAspects** -- Damage applied to resources
- **TimeArbiter** -- Attack iteration timing, time-scale awareness
- **BezierUtil / VectorUtil / Basis** -- Path computation
- **HitboxUtil** -- Entity detection
- **Prefab** -- Particle effects and sound presets
- **Config** -- Timing, radius, and modifier constants

## Known Limitations

- `SweepAttack` visual display logic is entirely commented out, making sweep attacks visually identical to basic attacks.
- The attack chaining system (nextAttack) creates attacks eagerly but chains them with simple delays rather than animation blending.
- Hit detection uses a fixed `secantRadius` from config rather than per-attack-profile hitbox sizes.
- The damage model has no armor reduction; the ARMOR aspect exists but is unused in damage calculations.
