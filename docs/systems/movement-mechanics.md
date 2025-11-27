# Movement Mechanics

This document provides detailed explanations of the movement abilities in Sword: Combat Evolved, including the mathematical formulas, algorithms, and design rationale behind dash, toss, and grab mechanics.

## Table of Contents

1. [Dash Mechanic](#dash-mechanic)
2. [Toss Mechanic](#toss-mechanic)
3. [Grab Mechanic](#grab-mechanic)
4. [Configuration Reference](#configuration-reference)

---

## Dash Mechanic

### Overview

The dash ability is a directional teleport that moves the player rapidly in their facing direction. It combines collision detection, particle effects, velocity application, and an automatic grab-on-contact feature.

### Algorithm

The dash executes through the following phases:

#### Phase 1: Raytracing
```text
1. Start from player eye location + initial_offset_y
2. Trace along player facing direction
3. Check for collisions every step_size blocks
4. Stop at first solid block OR max_distance
```

**Collision Detection**:
- Uses cylindrical hitbox with radius `Config.Movement.DASH_RAY_HITBOX_RADIUS` (0.7 blocks)
- Checks blocks with offset `Config.Movement.DASH_IMPEDANCE_CHECK_OFFSET_Y` (0.5 blocks up)
- Respects passable blocks (air, grass, flowers)

#### Phase 2: Velocity Application

The dash applies velocity in two components:

**Horizontal Velocity**:
```text
velocity_horizontal = direction * distance * forward_multiplier * base_power
```

**Mathematical Formula**:

$\vec{v}_h = \hat{d} \cdot s \cdot m_f \cdot p_b$

Where:
- $\vec{v}_h$ = Horizontal velocity vector (blocks/tick)
- $\hat{d}$ = Player facing direction (normalized unit vector)
- $s$ = Actual dash distance (blocks)
- $m_f$ = Forward multiplier (`Config.Movement.DASH_FORWARD_MULTIPLIER` = 0.5)
- $p_b$ = Base power (`Config.Movement.DASH_BASE_POWER` = 0.7)

**Example**:
```text
For a 10-block dash:
velocity_horizontal = direction * 10.0 * 0.5 * 0.7
                    = direction * 3.5 blocks/tick
```

**Vertical Velocity**:
```text
velocity_vertical = (distance * upward_multiplier + upward_boost) * base_power
```

**Mathematical Formula**:

$v_v = (s \cdot m_u + b_u) \cdot p_b$

Where:
- $v_v$ = Vertical velocity component (blocks/tick)
- $s$ = Actual dash distance (blocks)
- $m_u$ = Upward multiplier (`Config.Movement.DASH_UPWARD_MULTIPLIER` = 0.15)
- $b_u$ = Upward boost (`Config.Movement.DASH_UPWARD_BOOST` = 0.05)
- $p_b$ = Base power (`Config.Movement.DASH_BASE_POWER` = 0.7)

**Example**:
```
For a 10-block dash:
velocity_vertical = (10.0 * 0.15 + 0.05) * 0.7
                  = (1.5 + 0.05) * 0.7
                  = 1.085 blocks/tick
```

**Velocity Damping** (applied per tick):
```
velocity *= damping_factor
```

**Mathematical Formula**:

$\vec{v}_{t+1} = \vec{v}_t \cdot d$

Where:
- $\vec{v}_{t+1}$ = Velocity at next tick
- $\vec{v}_t$ = Current velocity
- $d$ = Damping factor (`Config.Movement.DASH_VELOCITY_DAMPING` = 0.6)

**Example** (velocity decay over 5 ticks):
```
Initial: v = 3.5 blocks/tick
Tick 1:  v = 3.5 * 0.6 = 2.1 blocks/tick
Tick 2:  v = 2.1 * 0.6 = 1.26 blocks/tick
Tick 3:  v = 1.26 * 0.6 = 0.756 blocks/tick
Tick 4:  v = 0.756 * 0.6 = 0.454 blocks/tick
Tick 5:  v = 0.454 * 0.6 = 0.272 blocks/tick
```

#### Phase 3: Grab Detection

After dash execution, the system checks for grabbable entities:

```
grab_radius = sqrt(DASH_GRAB_DISTANCE_SQUARED)
              = sqrt(8.5) ≈ 2.9 blocks
```

**Mathematical Formula**:

$r_{grab} = \sqrt{d^2_{grab}}$

Where:

- $r_{grab}$ = Grab detection radius (blocks)
- $d^2_{grab}$ = `Config.Movement.DASH_GRAB_DISTANCE_SQUARED` = 8.5 blocks²

**Example**:
```
grab_radius = √8.5
           ≈ 2.915 blocks
```

Entities within this radius are pulled toward the player using the grab mechanic.

#### Phase 4: Visual Effects

**Particle Trail**:
- Spawns `Config.Movement.DASH_PARTICLE_COUNT` (100) particles
- Spread: X=1.25, Y=1.25, Z=1.25 blocks
- Updates every `Config.Movement.DASH_PARTICLE_TASK_PERIOD` (2 ticks)

**Sound Effects**:
- **Flap Sound**: Volume 0.6, Pitch 1.0 (whoosh effect)
- **Sweep Sound**: Volume 0.3, Pitch 0.6 (trail effect)

### Design Rationale

**Why Distance-Based Velocity?**
- Longer dashes feel more impactful
- Prevents overpowered short-range dashing
- Creates risk/reward: longer dash = harder to control

**Why Upward Boost?**
- Prevents players from getting stuck on edges
- Makes dash feel more fluid and forgiving
- Enables creative vertical movement

**Why Velocity Damping?**
- Prevents infinite sliding
- Gives player control back quickly
- Reduces collision glitches from high speeds

---

## Toss Mechanic

### Overview

The toss ability throws the player's sword in an arc trajectory. The sword becomes a physics-based projectile that deals damage on impact and explodes.

### Trajectory Calculation

The toss uses a **two-phase arc trajectory**:

#### Phase 1: Upward Launch
```
iterations = TOSS_UPWARD_PHASE_ITERATIONS (2)
velocity_y = TOSS_UPWARD_VELOCITY_Y (0.25 blocks/tick)

For i = 0 to iterations:
    position.y += velocity_y
```

**Mathematical Formula**:

$y_{final} = y_{initial} + n \cdot v_y$

Where:

- $y_{final}$ = Final Y position after upward phase
- $y_{initial}$ = Starting Y position
- $n$ = Number of iterations (`Config.Movement.TOSS_UPWARD_PHASE_ITERATIONS` = 2)
- $v_y$ = Upward velocity (`Config.Movement.TOSS_UPWARD_VELOCITY_Y` = 0.25 blocks/tick)

**Example**:
```
Total upward displacement = 2 * 0.25 = 0.5 blocks
```

**Purpose**: Creates initial upward arc for visual appeal and clearance.

#### Phase 2: Forward Projectile
```
iterations = TOSS_FORWARD_PHASE_ITERATIONS (3)
base_force = TOSS_BASE_FORCE (1.5 blocks/tick)
might_multiplier = TOSS_MIGHT_MULTIPLIER_BASE + (might_level * INCREMENT)
                 = 2.5 + (might_level * 0.1)

velocity = player_facing * base_force * might_multiplier
```

**Mathematical Formula**:

$\vec{v}_{toss} = \hat{d}_{player} \cdot F_{base} \cdot M_{might}$

Where:

- $\vec{v}_{toss}$ = Toss velocity vector (blocks/tick)
- $\hat{d}_{player}$ = Player facing direction (normalized)
- $F_{base}$ = Base force (`Config.Movement.TOSS_BASE_FORCE` = 1.5 blocks/tick)
- $M_{might}$ = Might multiplier

**Might Multiplier Formula**:

$M_{might} = M_{base} + (L_{might} \cdot I_{increment})$

Where:

- $M_{base}$ = Base multiplier (`Config.Movement.TOSS_MIGHT_MULTIPLIER_BASE` = 2.5)
- $L_{might}$ = Might enchantment level (0-10)
- $I_{increment}$ = Increment per level (`Config.Movement.TOSS_MIGHT_MULTIPLIER_INCREMENT` = 0.1)

**Might Scaling**:
| Might Level | Multiplier | Effective Force |
|-------------|------------|-----------------|
| 0           | 2.5        | 3.75 blocks/tick |
| 5           | 3.0        | 4.50 blocks/tick |
| 10          | 3.5        | 5.25 blocks/tick |

### Damage Calculation

The toss damage is **velocity-based**:

```
damage = BASE_THROWN_DAMAGE + (item_velocity_magnitude * ITEM_VELOCITY_MULTIPLIER)
       = 12.0 HP + (velocity * 1.5)
```

**Mathematical Formula**:

$D_{toss} = D_{base} + (|\vec{v}_{item}| \cdot M_{velocity})$

Where:

- $D_{toss}$ = Total toss damage (HP)
- $D_{base}$ = Base thrown damage (`Config.Combat.BASE_THROWN_DAMAGE` = 12.0 HP)
- $|\vec{v}_{item}|$ = Item velocity magnitude (blocks/tick)
- $M_{velocity}$ = Velocity multiplier (`Config.Combat.ITEM_VELOCITY_MULTIPLIER` = 1.5)

**Example** (Might level 5):
```
velocity_magnitude = 3.0 * 4.5 = 13.5 blocks/tick (from might scaling)
damage = 12.0 + (13.5 * 1.5)
       = 12.0 + 20.25
       = 32.25 HP
```

**Additional Damage Factors**:
- **Sword Damage Multiplier**: 1.0× (full item damage applies)
- **Shard Damage**: 2 base shards
- **Toughness Damage**: 30.0 HP
- **Soulfire Reduction**: 5.0 points

### Explosion Mechanics

On impact, the toss creates an explosion:

```
explosion_power = TOSS_EXPLOSION_POWER (2.0)
set_fire = Config.World.EXPLOSIONS_SET_FIRE (false)
break_blocks = Config.World.EXPLOSIONS_BREAK_BLOCKS (false)
```

**Knockback**:
```
knockback = explosion_direction * TOSS_KNOCKBACK_MULTIPLIER (0.3)
```

**Mathematical Formula**:

$\vec{K}_{toss} = \hat{d}_{explosion} \cdot M_{knockback}$

Where:

- $\vec{K}_{toss}$ = Knockback vector (blocks/tick)
- $\hat{d}_{explosion}$ = Direction from explosion center to target (normalized)
- $M_{knockback}$ = Knockback multiplier (`Config.Movement.TOSS_KNOCKBACK_MULTIPLIER` = 0.3)

### Animation

The toss includes a visual wind-up animation:

```
animation_iterations = TOSS_ANIMATION_ITERATIONS (15)
location_offset = player_facing * TOSS_LOCATION_OFFSET_MULTIPLIER (0.3)
particle_height = player_height * TOSS_PARTICLE_HEIGHT_MULTIPLIER (0.5)
```

This creates a circular particle effect around the player during the throw.

### Design Rationale

**Why Two-Phase Trajectory?**
- Initial upward arc looks more natural
- Prevents instant ground collision
- Allows throws over obstacles

**Why Velocity-Based Damage?**
- Rewards longer throws (more time to accelerate)
- Creates skill ceiling for optimal throw angle
- Makes gravity/trajectory settings meaningful

**Why Explosion on Impact?**
- Clear visual feedback for hit
- Area-of-effect for grouped enemies
- Satisfying feedback for successful throw

---

## Grab Mechanic

### Overview

The grab ability pulls nearby entities toward the player using continuous force application.

### Force Application

```
pull_direction = normalize(player_position - entity_position)
pull_force = pull_direction * GRAB_PULL_STRENGTH
           = pull_direction * 0.8 blocks/tick

entity_velocity += pull_force
```

**Mathematical Formula**:

$\vec{F}_{pull} = \frac{\vec{P}_{player} - \vec{P}_{entity}}{|\vec{P}_{player} - \vec{P}_{entity}|} \cdot S_{pull}$

$\vec{v}_{entity} \mathrel{+}= \vec{F}_{pull}$

Where:

- $\vec{F}_{pull}$ = Pull force vector (blocks/tick)
- $\vec{P}_{player}$ = Player position
- $\vec{P}_{entity}$ = Entity position
- $S_{pull}$ = Pull strength (`Config.Movement.GRAB_PULL_STRENGTH` = 0.8 blocks/tick)
- $\vec{v}_{entity}$ = Entity velocity (updated each tick)

**Example** (entity 3 blocks away):
```
pull_direction = (0, 0, -3) / 3 = (0, 0, -1) [normalized]
pull_force = (0, 0, -1) * 0.8 = (0, 0, -0.8) blocks/tick
```

Applied every tick for `Config.Movement.GRAB_HOLD_DURATION` (40 ticks = 2 seconds).

### Range Detection

```
max_range = GRAB_MAX_RANGE (3.0 blocks)

Entities within sphere of radius max_range are affected.
```

### Integration with Dash

The dash automatically triggers grab on entities within grab range:

```
if (dash_completed && entity_within_grab_radius):
    apply_grab(entity)
```

This creates a **"dash-grab" combo** where dashing through enemies pulls them along.

### Design Rationale

**Why Continuous Force?**
- Allows entity to maintain some momentum
- Feels more natural than instant teleport
- Can be counteracted by player movement

**Why Limited Duration?**
- Prevents permanent entity locking
- Creates timing window for follow-up attacks
- Balances the ability's power

**Why Integrate with Dash?**
- Creates satisfying combat flow
- Rewards aggressive dash usage
- Enables gap-closing against fleeing enemies

---

## Configuration Reference

All movement mechanics are configured via [`Config.Movement`](../../../src/main/java/btm/sword/config/Config.java):

### Dash Configuration
- `DASH_MAX_DISTANCE` (10.0 blocks) - Maximum dash distance
- `DASH_CAST_DURATION` (5 ticks) - Cast time before dash
- `DASH_BASE_POWER` (0.7) - Velocity multiplier
- `DASH_FORWARD_MULTIPLIER` (0.5) - Horizontal velocity scaling
- `DASH_UPWARD_MULTIPLIER` (0.15) - Vertical velocity scaling
- `DASH_UPWARD_BOOST` (0.05) - Base upward velocity
- `DASH_VELOCITY_DAMPING` (0.6) - Per-tick velocity reduction
- `DASH_GRAB_DISTANCE_SQUARED` (8.5 blocks²) - Auto-grab radius

### Toss Configuration
- `TOSS_BASE_FORCE` (1.5 blocks/tick) - Base throw velocity
- `TOSS_MIGHT_MULTIPLIER_BASE` (2.5) - Base might scaling
- `TOSS_MIGHT_MULTIPLIER_INCREMENT` (0.1) - Per-level increase
- `TOSS_UPWARD_VELOCITY_Y` (0.25 blocks/tick) - Initial upward arc
- `TOSS_KNOCKBACK_MULTIPLIER` (0.3) - Explosion knockback
- `TOSS_EXPLOSION_POWER` (2.0) - Explosion radius

### Grab Configuration
- `GRAB_PULL_STRENGTH` (0.8 blocks/tick) - Pull force
- `GRAB_MAX_RANGE` (3.0 blocks) - Detection radius
- `GRAB_HOLD_DURATION` (40 ticks) - Pull duration

For implementation details, see:
- [`MovementAction.java`](../../src/main/java/btm/sword/system/action/MovementAction.java) - Movement ability execution
- [`ThrownItem.java`](../../src/main/java/btm/sword/system/action/utility/thrown/ThrownItem.java) - Toss projectile physics
