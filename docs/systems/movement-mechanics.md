# Movement Mechanics

## Overview

The movement system handles dashing (forward/backward), tossing grabbed entities, and strafe movement. Dashes can target interactive items in the world, creating a grab-dash mechanic.

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `Dash` | `system/action/movement/Dash.java` | Core dash implementation. Handles straight dashes, item-targeted dashes, flat ground dashes, and particle/sound effects. |
| `DashDirection` | `system/action/movement/DashDirection.java` | Enum: `FORWARD`, `BACKWARD`, `NONE`. |
| `MovementAction` | `system/action/movement/MovementAction.java` | Static entry points: `dash()`, `toss()`. |
| `GrabAction` | `system/action/utility/GrabAction.java` | Initiates grab on targeted entity within range. |

## Dash Mechanics

1. **Direction** -- Forward dashes use the player's eye direction; backward dashes negate it.
2. **Ground detection** -- Uses `EntityUtil.isOnGround()` to determine if the player is grounded.
3. **Flat dash** -- If grounded and not looking steeply up/down, the dash uses `getFlatDir()` (yaw only, no pitch) with doubled power.
4. **Air dash** -- When airborne, uses full 3D direction. Increments `airDashesPerformed`; limited by `CombatProfile.maxAirDodges`. Air dashes reset when the player touches the ground (checked every 3 ticks).
5. **Item dash** -- If the player's main hand is empty or holding a Soul Link, the dash checks for interactive `ItemDisplay` entities via `HitboxUtil.ray()`. If found and within range after the dash, the item is grabbed via `InteractiveItemArbiter.onGrab()`.
6. **Impedance check** -- Before dashing to an item, a `rayTraceBlocks` check ensures no solid blocks obstruct the path.
7. **Speed buff** -- A temporary Speed potion effect is applied during dashes.
8. **Cooldown** -- Calculated via `calcCooldown(CELERITY, 200, 1000, 10)` -- base 1000ms, reduced by 10ms per CELERITY point, minimum 200ms.

## Grab Mechanics

`GrabAction.grab()` uses `HitboxUtil.lineFirst()` to find the nearest entity in the player's look direction. On success:

- The target is marked as grabbed
- The grabber is marked as grabbing
- The grabber can then: punch (LEFT while grabbing), throw (SWAP while grabbing to release), or impale (LEFT while holding Soul Link, triggers UmbralBlade GRAB_IMPALE request).

`MovementAction.toss()` launches the grabbed entity along the grabber's direction with knockback.

## Particle and Sound Effects

Dashes produce cloud and smoke particles that decay over 5 iterations. Block particles are spawned when grabbing items near the ground. Wing flap and sweep sounds play on item grabs.

## Dependencies

- **Config.Movement** -- All movement constants (power, distances, delays, particle parameters)
- **TimeArbiter** -- Fixed-iteration task for dash particles
- **HitboxUtil** -- Ray and line checks for item/entity targeting
- **InteractiveItemArbiter** -- Item grab handling
- **EntityUtil** -- Ground detection

## Known Limitations

- `umbralDash()` method exists but is empty (stub).
- The item dash grab distance check uses a hardcoded squared distance from config rather than scaling with dash power.
- Several magic numbers in the dash-to-item logic (e.g., `Math.log(length)` for velocity scaling) could benefit from config-driven tuning.
