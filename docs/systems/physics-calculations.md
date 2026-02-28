# Physics and Math Calculations

## Overview

The math and physics subsystems provide 3D vector utilities, Bezier curve generation, coordinate basis transformations, and hitbox detection algorithms used throughout the combat system.

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `BezierUtil` | `utility/math/BezierUtil.java` | Generates cubic Bezier curve functions in 3D. |
| `ControlVectors` | `utility/math/ControlVectors.java` | Record holding 4 control points (start, end, c1, c2) for Bezier curves. Supports basis transformation via `adjustToBasis()`. |
| `Basis` | `utility/math/Basis.java` | Orthonormal basis (right, up, forward) representing a local coordinate frame. |
| `VectorUtil` | `utility/math/VectorUtil.java` | Static utilities: basis construction, basis rotation, coordinate transforms, angle calculations (pitch, yaw, angle between vectors), plane projection. |
| `HitboxUtil` | `utility/entity/HitboxUtil.java` | Static hitbox detection: line, secant, sphere, ray, sphereAtRayHit. |
| `EntityUtil` | `utility/entity/EntityUtil.java` | Entity helpers including ground detection. |

## Bezier Curves

The cubic Bezier function `BezierUtil.cubicBezier3D(ControlVectors)` returns a `Function<Double, Vector>` mapping parameter `t` to a 3D point:

```
P(t) = (1-t)^3 * P0 + 3(1-t)^2 * t * P1 + 3(1-t) * t^2 * P2 + t^3 * P3
```

Where P0=start, P1=c1, P2=c2, P3=end. The `t` parameter typically ranges from `attackStartValue` to `attackEndValue` (configured in `Config.Combat`).

**Important**: The `cubicBezier3D` implementation mutates the cloned control vectors via `multiply()` on each call. Since `ControlVectors` accessor methods return clones, this is safe but creates garbage on every evaluation.

## Basis Construction

`VectorUtil.getBasis(Location, Vector)`:

1. Normalizes the direction vector.
2. Computes `right = dir x UP` (cross product with world up).
3. Handles the degenerate case when looking straight up/down by using a yaw-based horizontal reference.
4. Computes `up = right x dir`.
5. Returns `Basis(right, up, forward=dir)`.

`VectorUtil.getBasisWithoutPitch(Entity)`:

- Uses entity body yaw only (no pitch).
- Produces a flat basis with up = world up.
- For players, uses `getBodyYaw()` which differs from head yaw.

## Coordinate Transformation

`VectorUtil.transformWithNewBasis(Basis, Vector)` converts a local-space vector to world-space:

```
world = right * x + up * y + forward * z
```

This is used by `ControlVectors.adjustToBasis()` to orient attack paths relative to the player's facing direction.

## Hitbox Detection Algorithms

### Secant

Steps along the line between two points at intervals of `thickness`, checking `getNearbyLivingEntities(thickness)` at each step. Returns all unique entities found. Used for attack sweep detection.

### Line

Similar to secant but from origin along a direction for a max range.

### Sphere

Simple `getNearbyLivingEntities(radius)` from a center point.

### Ray

Uses Bukkit's `rayTraceEntities()` for precise single-target detection with configurable ray size and filter predicate.

## Physics Constants (from Config.Physics)

- `THROWN_ITEMS_GRAVITY_DAMPER` (46.0) -- Higher = less gravity effect on thrown items
- `THROWN_ITEMS_TRAJECTORY_ROTATION` (0.03696 rad/tick) -- Spin rate of thrown items
- Attack velocity vectors for knockback
- Display offset values for thrown item positioning

## Known Limitations

- No spatial acceleration or deceleration curves for dashes; velocity is applied as an instantaneous impulse.
- The secant hitbox detection creates many temporary `Location` objects via `clone()` and `add()` on every step, which could cause GC pressure during heavy combat.
- `VectorUtil.rotateBasis()` mutates vectors in-place via a `List<Vector>` parameter, which is inconsistent with the immutable-clone pattern used elsewhere.
