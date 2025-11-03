# Configuration Guide

This guide explains how to configure Sword: Combat Evolved using the `config.yaml` file.

## Overview

Sword: Combat Evolved uses a comprehensive configuration system that allows server administrators to tune gameplay without modifying code. All configuration values are hot-reloadable via the `/sword reload` command.

### Configuration File Locations

**Live Configuration (Edit This):**
```
plugins/Sword/config.yaml
```

**Default Template (Don't Edit):**
```
src/main/resources/config.yaml (inside the plugin JAR)
```

### How It Works

1. **First Startup**: When the plugin loads for the first time, it automatically extracts the default `config.yaml` from the JAR to `plugins/Sword/config.yaml`
2. **Configuration Editing**: Server administrators edit `plugins/Sword/config.yaml` on the file system
3. **Hot Reload**: `/sword reload` re-reads values from `plugins/Sword/config.yaml`
4. **Template**: The config.yaml inside the JAR is never modified - it only serves as the initial template

**Important:** Changes to the JAR's internal config.yaml have no effect on a running server. Always edit `plugins/Sword/config.yaml` in the plugins folder.

## Quick Start

1. **Find the config file** in `plugins/Sword/config.yaml`
2. **Edit values** using any text editor (VS Code, Notepad++, nano, vim)
3. **Save the file**
4. **Reload in-game** with `/sword reload`
5. **Check console** for validation errors

## Configuration Structure

The configuration is organized into logical sections:

| Section | Description | Common Uses |
|---------|-------------|-------------|
| [`physics`](#physics-system) | Projectile motion, gravity, velocity | Thrown item behavior |
| [`combat`](#combat-system) | Damage, hitboxes, attack patterns | Combat tuning |
| [`display`](#display--visual-effects) | Particles, animations, rendering | Visual effects |
| [`detection`](#detection--collision) | Ground checks, collision detection | Gameplay feel |
| [`timing`](#timing--cooldowns) | Cooldowns, delays, intervals | Pacing and balance |
| [`audio`](#audio) | Sound effects, volumes, pitches | Audio feedback |
| [`entities`](#entity-behavior) | Movement modifiers, effects | Entity control |
| [`world`](#world-interaction) | Block interaction, explosions | World effects |
| [`debug`](#developer--debug) | Testing, profiling, visualization | Development |

## Section Details

### Physics System

Controls projectile motion, gravity, and velocity calculations.

**Key Values:**

```yaml
physics:
  thrown_items:
    gravity_damper: 46.0          # Gravity strength (30-60)
    trajectory_rotation: 0.03696  # Rightward curve (π/85)

  attack_velocity:
    grounded_damping:
      horizontal: 0.3  # Movement slowdown during attacks
      vertical: 0.4
```

**Tuning Tips:**
- **Lower gravity_damper** (30-40) = Items fall faster, shorter throws
- **Higher gravity_damper** (50-60) = Floatier arc, longer throws
- **Adjust trajectory_rotation** to compensate for throw direction

---

### Combat System

Damage calculations, hitboxes, attack patterns, and combat mechanics.

**Key Values:**

```yaml
combat:
  attacks:
    base_damage: 20.0  # Base damage before modifiers (10-30)

    range_multipliers:
      basic_1: 1.4     # First combo hit reach
      basic_2: 1.4     # Second combo hit
      basic_3: 1.4     # Finisher

    down_air_threshold: -0.5  # Angle for down-air (-1.0 to 0.0)

  hitboxes:
    secant_radius: 0.4  # Attack detection radius (0.3-0.6)
```

**Tuning Tips:**
- **base_damage** scales all weapon damage proportionally
- **range_multipliers** affect combat spacing and feel
- **secant_radius** controls attack generosity (higher = more forgiving)
- **down_air_threshold**: -1.0 = straight down only, -0.3 = 45° cone

**Thrown Damage:**

```yaml
combat:
  thrown_damage:
    sword_axe:
      base_shards: 2
      toughness_damage: 75.0      # Armor penetration
      knockback_grounded: 0.7
      knockback_airborne: 1.0
```

---

### Display & Visual Effects

Particle systems, animations, teleportation, and rendering.

**Key Values:**

```yaml
display:
  default_teleport_duration: 2  # Smoothness of entity movement

  item_display_follow:
    update_interval: 2    # Ticks between position updates
    particle_interval: 4  # Particle display frequency

  bezier:
    num_steps: 50  # Attack curve smoothness (30-100)
```

**Tuning Tips:**
- **teleport_duration**: 1 = instant, 5 = very smooth but laggy
- **update_interval**: Lower = smoother but more expensive
- **num_steps**: Higher = smoother curves, more particles

---

### Detection & Collision

Ground detection, entity detection, ray tracing.

**Key Values:**

```yaml
detection:
  ground_check:
    max_distance: 0.3  # Ground detection range (0.1-0.5)
```

**Tuning Tips:**
- **max_distance**: 0.1 = very precise, 0.5 = handles slopes/stairs better

---

### Timing & Cooldowns

Grace periods, timeouts, delays, and duration values.

**Key Values:**

```yaml
timing:
  thrown_items:
    catch_grace_period: 20      # Ticks before catching own throw
    disposal_timeout: 1000       # Despawn time for grounded items

  attacks:
    combo_window_base: 3  # Attack combo timing window
```

**Tuning Tips:**
- **catch_grace_period**: Prevents instant re-catch exploits (10-30 ticks)
- **disposal_timeout**: Balance item cleanup vs player convenience
- **combo_window_base**: Affects combat pacing (1-5 ticks)

---

### Audio

Sound effect parameters (volume, pitch).

**Key Values:**

```yaml
audio:
  throw:
    sound: ENTITY_ENDER_DRAGON_FLAP
    volume: 0.5  # 0.0-1.0
    pitch: 0.4   # 0.5-2.0 (lower = deeper)

  attack:
    sound: ENTITY_ENDER_DRAGON_FLAP
    volume: 0.35
    pitch: 0.6
```

**Tuning Tips:**
- **volume**: 0.0 = silent, 1.0 = full volume
- **pitch**: 0.5 = very deep, 2.0 = very high
- See [Minecraft Sound List](https://minecraft.wiki/w/Sounds.json) for available sounds

---

### Entity Behavior

Movement modifiers, slowness effects, entity-specific settings.

**Key Values:**

```yaml
entities:
  throw_preparation:
    effect: SLOWNESS
    amplifier: 2  # Slowness II (40% speed)

  pinned_rotation:
    lock_rotation: true
    reset_velocity: true
```

---

### World Interaction

Block detection, marker placement, explosion settings.

**Key Values:**

```yaml
world:
  explosions:
    power: 1.0
    set_fire: false
    break_blocks: false
```

---

### Developer / Debug

Testing values, experimental features, performance tuning.

**Key Values:**

```yaml
debug:
  verbose_reload: false
  show_hitbox_particles: false
  show_bezier_control_points: false
```

**Debug Features:**
- **verbose_reload**: Logs detailed reload information
- **show_hitbox_particles**: Visualizes attack hitboxes
- **show_bezier_control_points**: Shows attack curve control points

## Hot-Reloading

### How It Works

The configuration system supports hot-reloading without server restart:

1. Edit `config.yaml` with your changes
2. Save the file
3. Run `/sword reload` in-game
4. Check console for validation messages

**What Happens:**
- Configuration file is re-parsed from disk
- YAML syntax is validated
- New values are applied to the active configuration
- If errors occur, previous values remain in effect

### Live Testing Workflow

**You can modify values while the server is running!**

```bash
# 1. Keep server running (no restart needed)
./gradlew runServer

# 2. In another terminal, edit the live config
nano run/plugins/Sword/config.yaml
# Or: vim plugins/Sword/config.yaml
# Or: Use your IDE to edit the file

# 3. Change a value
combat:
  attacks:
    base_damage: 15.0  # Changed from 20.0

# 4. Save the file (no server action needed yet)

# 5. In-game, reload config
/sword reload

# 6. Test immediately - new values are now active!
# (throw items, test combat, check changes)

# 7. Iterate: Edit again, save, /sword reload, test
```

**Key Point:** You edit `plugins/Sword/config.yaml` on the server filesystem while it's running, then use `/sword reload` to apply changes instantly. No server restart required!

### What Gets Reloaded

✅ **Reloaded:**
- All numeric values (damage, ranges, timings)
- Physics parameters (gravity, velocity)
- Visual settings (particles, animations)
- Audio settings (volumes, pitches)
- Timing windows and cooldowns

❌ **Not Reloaded:**
- Active abilities in progress
- Entity references (thrown items, displays)
- Event listener registrations
- Player statistics or data

### Validation

The config system validates:
- **YAML syntax** - Proper indentation and structure
- **Value types** - Numbers vs strings vs booleans
- **Value ranges** - Some values have min/max bounds (documented in config)

**If validation fails:**
- Error messages appear in console
- Previous configuration remains active
- Plugin continues running normally

## Best Practices

### 1. Backup Before Changes

```bash
# Create backup
cp plugins/Sword/config.yaml plugins/Sword/config.yaml.backup

# Restore if needed
cp plugins/Sword/config.yaml.backup plugins/Sword/config.yaml
```

### 2. Test in Development

- Use a development server for major changes
- Test combat feel with actual gameplay
- Verify physics behave as expected

### 3. Document Your Changes

Keep a changelog of what you tuned and why:

```yaml
# My Server Config Notes:
# 2025-11-03: Reduced base_damage from 20 to 15 (too powerful)
# 2025-11-03: Increased gravity_damper from 46 to 52 (throws too short)
combat:
  attacks:
    base_damage: 15.0  # [Changed from 20.0]
```

### 4. Make Small Changes

- Adjust one system at a time
- Test between changes
- Iterate based on player feedback

### 5. Understand Dependencies

Some values affect each other:
- **Damage + Knockback** = Combined lethality
- **Gravity + Throw Speed** = Throw distance
- **Attack Range + Cast Duration** = Hit windows

## Common Tuning Scenarios

### Example: Live Tuning Thrown Item Gravity

**Goal:** Make thrown items stay in the air longer

**Live Testing Process:**

```bash
# Start server
./gradlew runServer

# Connect and test current behavior
# Items falling too fast!

# Edit config while server runs
vim run/plugins/Sword/config.yaml

# Try increasing gravity damper
physics:
  thrown_items:
    gravity_damper: 55.0  # Up from 46.0

# Save and reload
/sword reload  # In-game

# Test - still too fast!
# Edit again (server still running)
gravity_damper: 65.0  # Try higher

# Save and reload
/sword reload

# Test - too floaty now!
# Edit one more time
gravity_damper: 60.0  # Sweet spot!

# Save and reload
/sword reload

# Perfect! Keep this value
```

**Result:** Found optimal value (60.0) through live iteration without any server restarts.

---

### Scenario: Thrown items fall too fast

**Problem:** Items hit the ground too quickly

**Solution:**
```yaml
physics:
  thrown_items:
    gravity_damper: 60.0  # Increase from default 46.0
```

---

### Scenario: Combat feels too punishing

**Problem:** New players dying too quickly

**Solution:**
```yaml
combat:
  attacks:
    base_damage: 15.0       # Reduce from 20.0
  thrown_damage:
    sword_axe:
      toughness_damage: 50.0  # Reduce from 75.0
```

---

### Scenario: Hitboxes feel inconsistent

**Problem:** Attacks missing when they look like hits

**Solution:**
```yaml
combat:
  hitboxes:
    secant_radius: 0.5  # Increase from 0.4 (more forgiving)
```

---

### Scenario: Thrown items despawn too quickly

**Problem:** Items disappear before players can retrieve them

**Solution:**
```yaml
timing:
  thrown_items:
    disposal_timeout: 2000  # Increase from 1000 (100 seconds)
```

## Troubleshooting

### Config not reloading

**Symptoms:** `/sword reload` succeeds but values don't change

**Possible Causes:**
1. Wrong file path (editing wrong config)
2. YAML syntax errors
3. Value outside valid range

**Solutions:**
```bash
# Verify file location
ls plugins/Sword/config.yaml

# Check console for errors after reload
# Look for: "Configuration validation failed"

# Verify YAML syntax online
# https://www.yamllint.com/
```

### YAML syntax errors

**Common Mistakes:**

❌ **Bad:**
```yaml
physics:
thrown_items:  # Missing indentation
  gravity_damper: 46.0
```

✅ **Good:**
```yaml
physics:
  thrown_items:  # Proper indentation (2 spaces)
    gravity_damper: 46.0
```

❌ **Bad:**
```yaml
audio:
  throw:
    volume: 0.5.0  # Invalid number
```

✅ **Good:**
```yaml
audio:
  throw:
    volume: 0.5  # Valid decimal
```

### Values not in expected range

**Symptoms:** Value reverts to default after reload

**Solution:** Check config comments for valid ranges:

```yaml
# Range: 30-60 (lower = faster fall, higher = floatier arc)
gravity_damper: 46.0  # Must be within 30-60
```

## Advanced Configuration

### Custom Sound Effects

Change attack/throw sounds to any Minecraft sound:

```yaml
audio:
  attack:
    sound: ENTITY_WITHER_SHOOT  # Custom sound
    volume: 0.8
    pitch: 1.2
```

**Available Sounds:**
- See [Minecraft Sounds](https://minecraft.wiki/w/Sounds.json) for full list
- Format: `CATEGORY_ENTITY_ACTION` (e.g., `ENTITY_PLAYER_HURT`)

### Physics Experimentation

Create custom throw physics profiles:

```yaml
# Floaty mode (low gravity, high arc)
physics:
  thrown_items:
    gravity_damper: 80.0
    trajectory_rotation: 0.01

# Realistic mode (high gravity, flat arc)
physics:
  thrown_items:
    gravity_damper: 30.0
    trajectory_rotation: 0.05
```

### Performance Tuning

Adjust update frequencies for performance:

```yaml
display:
  item_display_follow:
    update_interval: 3  # Reduce from 2 (less frequent updates)
    particle_interval: 6  # Reduce from 4 (fewer particles)

timing:
  intervals:
    item_motion_update: 2  # Increase from 1 (less frequent physics)
```

## Reference Values

### Minecraft Time Units

```yaml
# 1 tick = 50ms (at 20 TPS)
# 20 ticks = 1 second
# 1200 ticks = 1 minute
```

### Math Constants

```yaml
# π (Pi) ≈ 3.14159265359
# φ (Golden Ratio) ≈ 1.618033988749
# Fibonacci: 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144...
```

### Common Design Values

```yaml
# 0.3-0.5: Typical reaction time windows (seconds)
# 2-3: "Rule of thirds" for difficulty scaling
# 5-7: Magic number for comfortable choice count
# 0.618/1.618: Golden ratio for pleasing proportions
```

## See Also

- [Command Reference](commands.md) - How to use `/sword reload`
- [config.yaml](../../src/main/resources/config.yaml) - Full configuration file with detailed comments
- [Issue #66](https://github.com/Mere-Solace/Sword-Combat-Plugin/issues/66) - Configuration system design
- [ConfigManager.java](../../src/main/java/btm/sword/config/ConfigManager.java) - Configuration implementation
