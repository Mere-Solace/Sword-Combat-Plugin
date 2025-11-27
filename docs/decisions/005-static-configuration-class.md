# ADR 005: Static Configuration Class

**Status**: Accepted (Supersedes ADR 003)
**Date**: 2025-11-19
**Authors**: iAmGiG, Mere Solace

## Context

ADR 003 introduced a hybrid configuration pattern that reduced boilerplate by flattening simple config groups (2-3 values) while keeping complex groups (5+ values) nested. While this was a significant improvement over the initial over-modularized approach, it still required substantial boilerplate:

**Problems with Hybrid Pattern (ADR 003)**:
```java
// Still required nested section classes with constructors
@Getter
public class PhysicsConfig {
    private final ThrownItemsConfig thrownItems;

    @Getter
    public static class ThrownItemsConfig {
        private final double gravityDamper;
        private final double trajectoryRotation;
        // ... 9 more fields

        // 15+ lines of constructor logic with null checks
        public ThrownItemsConfig(ConfigurationSection section) {
            if (section != null) {
                this.gravityDamper = section.getDouble("gravity_damper", 46.0);
                this.trajectoryRotation = section.getDouble("trajectory_rotation", 0.03696);
                // ... repeated pattern for each field
            } else {
                this.gravityDamper = 46.0;
                this.trajectoryRotation = 0.03696;
                // ... repeated defaults
            }
        }
    }
}

// Access pattern - verbose method chaining
ConfigManager.getInstance().getPhysics().getThrownItems().getGravityDamper()
```

**Issues**:
1. **Still verbose**: Each config value requires constructor logic + getter + field declaration (3+ lines)
2. **Poor IDE experience**: Values hidden behind method chains - hard to discover
3. **Runtime overhead**: Creating new section objects on every reload
4. **Cognitive load**: Developers must decide when to flatten vs nest
5. **Null safety boilerplate**: Every nested class needs null checks and default fallbacks

## Decision

**We use a fully static Config class with package-private setters and generic loading methods.**

All configuration values are stored as static fields in nested static classes. ConfigManager loads values from YAML into these static fields using a generic `get<T>()` method. Setters are package-private (only ConfigManager can modify values).

### Core Principles

1. **Static Fields**: All config values are `public static` fields for direct access
2. **Package-Private Setters**: Only `ConfigManager` can modify values (encapsulation)
3. **Generic Loading**: Single `get<T>()` method handles all types with compile-time safety
4. **Clone Pattern**: Reference types (Vector) return clones to prevent mutation
5. **ALL_CAPS Naming**: All config fields use `ALL_CAPS_WITH_UNDERSCORES` format (Java constant convention)
6. **Maximum 2 Levels of Nesting**: Config paths never exceed 2 levels (e.g., `Config.Combat.FIELD_NAME`)
7. **Field/Setter Pairing**: Each field is immediately followed by its setter for clear association

## Implementation

### Naming Conventions

**ALL_CAPS with Underscores**:

- All config field names use `ALL_CAPS_WITH_UNDERSCORES` format
- This makes config constants visually distinct from regular variables
- Follows Java constant naming conventions (static final → static mutable constants)

**Flattened Prefixes**:

- Removed nested class names become field prefixes
- `Combat.Attacks.baseDamage` → `Combat.ATTACKS_BASE_DAMAGE`
- `Movement.Dash.maxDistance` → `Movement.DASH_MAX_DISTANCE`
- `Physics.ThrownItems.gravityDamper` → `Physics.THROWN_ITEMS_GRAVITY_DAMPER`

**2-Level Maximum Nesting**:

- OLD: `Config.Combat.Attacks.RangeMultipliers.basiup` (4 levels - TOO DEEP)
- NEW: `Config.Combat.ATTACKS_RANGE_MULTIPLIERS_BASIC_1` (2 levels - CORRECT)
- Flattening prevents deeply nested access patterns
- Keeps code readable and IDE autocomplete responsive

### Config Class Structure

```java
public class Config {
    public static class Physics {
        // Thrown Items configuration
        public static double THROWN_ITEMS_GRAVITY_DAMPER = 46.0;
        static void setTHROWN_ITEMS_GRAVITY_DAMPER(double value) { THROWN_ITEMS_GRAVITY_DAMPER = value; }

        public static double THROWN_ITEMS_TRAJECTORY_ROTATION = 0.03696;
        static void setTHROWN_ITEMS_TRAJECTORY_ROTATION(double value) { THROWN_ITEMS_TRAJECTORY_ROTATION = value; }

        // Display offset (field/setter pairs)
        public static float THROWN_ITEMS_DISPLAY_OFFSET_X = -0.5f;
        static void setTHROWN_ITEMS_DISPLAY_OFFSET_X(float value) { THROWN_ITEMS_DISPLAY_OFFSET_X = value; }

        public static float THROWN_ITEMS_DISPLAY_OFFSET_Y = 0.1f;
        static void setTHROWN_ITEMS_DISPLAY_OFFSET_Y(float value) { THROWN_ITEMS_DISPLAY_OFFSET_Y = value; }

        public static float THROWN_ITEMS_DISPLAY_OFFSET_Z = 0.5f;
        static void setTHROWN_ITEMS_DISPLAY_OFFSET_Z(float value) { THROWN_ITEMS_DISPLAY_OFFSET_Z = value; }
    }
}
```

**Result**: 2 lines per config value (field + setter, paired together)

### Generic Loading Method

```java
public class ConfigManager {
    /**
     * Generic method to load a value from config with type safety.
     */
    @SuppressWarnings("unchecked")
    private <T> T get(String path, T defaultValue) {
        if (defaultValue instanceof Double) {
            return (T) Double.valueOf(config.getDouble(path, (Double) defaultValue));
        } else if (defaultValue instanceof Integer) {
            return (T) Integer.valueOf(config.getInt(path, (Integer) defaultValue));
        } else if (defaultValue instanceof Float) {
            return (T) Float.valueOf((float) config.getDouble(path, (Float) defaultValue));
        } else if (defaultValue instanceof Boolean) {
            return (T) Boolean.valueOf(config.getBoolean(path, (Boolean) defaultValue));
        } else if (defaultValue instanceof String) {
            return (T) config.getString(path, (String) defaultValue);
        } else if (defaultValue instanceof List) {
            return (T) config.getStringList(path);
        }
        return defaultValue;
    }
}
```

### Section Loading

```java
private void loadPhysicsConfig() {
    ConfigurationSection physics = config.getConfigurationSection("physics");
    if (physics == null) return;

    // Direct loading with type inference from defaults
    Config.Physics.setTHROWN_ITEMS_GRAVITY_DAMPER(
        get("physics.thrown_items.gravity_damper", 46.0)
    );
    Config.Physics.setTHROWN_ITEMS_TRAJECTORY_ROTATION(
        get("physics.thrown_items.trajectory_rotation", 0.03696)
    );
    Config.Physics.setTHROWN_ITEMS_DISPLAY_OFFSET_X(
        get("physics.thrown_items.display_offset.x", -0.5f)
    );
    Config.Physics.setTHROWN_ITEMS_DISPLAY_OFFSET_Y(
        get("physics.thrown_items.display_offset.y", 0.1f)
    );
    Config.Physics.setTHROWN_ITEMS_DISPLAY_OFFSET_Z(
        get("physics.thrown_items.display_offset.z", 0.5f)
    );
}
```

### Access Pattern

```java
// OLD (ADR 003):
double gravity = ConfigManager.getInstance().getPhysics().getThrownItems().getGravityDamper();

// NEW (ADR 005):
double gravity = Config.Physics.THROWN_ITEMS_GRAVITY_DAMPER;
```

## Reasoning

### Advantages

1. **Dramatic Boilerplate Reduction**:
   - ADR 003: ~3-5 lines per value (field + constructor logic + getter)
   - ADR 005: 1-2 lines per value (field + setter)
   - **Result**: 60-80% reduction in config-related code

2. **Superior IDE Experience**:
   - Direct field access: `Config.Physics.` triggers autocomplete
   - See all available values immediately
   - Jump to definition shows actual value and documentation
   - No method chaining - shorter, clearer code

3. **Performance**:
   - No object instantiation on reload (just field assignment)
   - No garbage collection pressure from section objects
   - Static fields = single memory location

4. **Maintainability**:
   - Adding new config value: just add field + setter
   - No decision fatigue (flatten vs nest)
   - Clear organization through static class hierarchy
   - Generic `get<T>()` eliminates repetitive loading code

5. **Encapsulation**:
   - Package-private setters prevent external modification
   - Only ConfigManager can change values
   - Public fields for read access (no getter boilerplate)

6. **Type Safety**:
   - Compile-time type checking
   - Generic method ensures type correctness
   - Default values define expected types

### Trade-offs

1. **Static State**:
   - Static fields are global mutable state
   - Mitigated by package-private setters (controlled mutation)
   - Hot-reload works by updating static fields

2. **Testing Complexity**:
   - Static fields harder to mock in tests
   - Can provide test utilities to set values if needed
   - Most tests should use actual config values anyway

3. **Thread Safety**:
   - Reload operation not atomic (fields updated sequentially)
   - Acceptable: config reloads are rare admin actions
   - Values are primitives or immutable types (no concurrent modification issues)

4. **Migration Required**:
   - All existing config access must be updated
   - Find/replace can automate most of it
   - Breaking change (but internal API only)

### Alternatives Considered

1. **Keep ADR 003 Hybrid Pattern**:
   - Still too verbose (3-5 lines per value)
   - Method chaining harder to read
   - Rejected: doesn't solve core boilerplate problem

2. **Dependency Injection**:
   - Pass Config instance everywhere
   - Adds boilerplate at call sites
   - Overkill for single-plugin architecture
   - Rejected: complexity without benefit

3. **Builder Pattern**:
   - Fluent API for config construction
   - Doesn't reduce reading boilerplate
   - Adds method chaining overhead
   - Rejected: optimizes wrong direction (building vs using)

## Consequences

### Positive

- **93% faster** to add new config values (15 lines → 1-2 lines)
- **80% reduction** in config-related code across codebase
- **Clearer code**: `Config.Combat.ATTACKS_BASE_DAMAGE` vs method chains
- **Better discoverability**: IDE autocomplete shows all values
- **Maintained hot-reload**: Still works via `/sword reload`
- **Simpler onboarding**: No pattern decisions, just add field + setter

### Negative

- **One-time migration cost**: 10 files need config access updated
- **Static state**: Requires understanding of static field semantics
- **Testing**: May need test utilities for setting config in tests

### Neutral

- **config.yaml unchanged**: Users see no difference
- **Hot-reload behavior unchanged**: Still reloads from disk
- **Default values moved**: From nested constructors to static field initialization

## Migration Path

### For Developers

**Pattern Replacement**:
```java
// Find:    ConfigManager.getInstance().getSection().getSubsection().getValue()
// Replace: Config.Section.SUBSECTION_VALUE
```

**Example**:
```java
// OLD:
double damage = ConfigManager.getInstance().getCombat().getAttacks().getBaseDamage();

// NEW:
double damage = Config.Combat.ATTACKS_BASE_DAMAGE;
```

### Files Requiring Update

1. `AttackAction.java`
2. `MovementAction.java`
3. `ThrownItem.java`
4. `Attack.java`
5. `CombatProfile.java`
6. `SwordEntity.java`
7. `UmbralBlade.java`
8. `DisplayUtil.java`
9. `EntityUtil.java`
10. `SoundWrapper.java`

### Breaking Changes

**Removed**:
- All `ConfigManager.get*()` section accessor methods
- All `*Config` section classes (PhysicsConfig, CombatConfig, etc.)

**Added**:
- Static `Config` class with all values
- Generic `ConfigManager.get<T>()` (private)

**Unchanged**:
- `ConfigManager.reload()` - still works
- `ConfigManager.getInstance()` - still exists
- `config.yaml` structure - no changes
- Hot-reload behavior - identical

## Implementation Examples

### Example 1: Simple Values (Physics)

**Before (ADR 003)**:
```java
@Getter
public class PhysicsConfig {
    private final ThrownItemsConfig thrownItems;

    @Getter
    public static class ThrownItemsConfig {
        private final double gravityDamper;
        private final double trajectoryRotation;

        public ThrownItemsConfig(ConfigurationSection section) {
            if (section != null) {
                this.gravityDamper = section.getDouble("gravity_damper", 46.0);
                this.trajectoryRotation = section.getDouble("trajectory_rotation", 0.03696);
            } else {
                this.gravityDamper = 46.0;
                this.trajectoryRotation = 0.03696;
            }
        }
    }
}
```

**After (ADR 005)**:
```java
public static class Physics {
    public static double THROWN_ITEMS_GRAVITY_DAMPER = 46.0;
    static void setTHROWN_ITEMS_GRAVITY_DAMPER(double value) { THROWN_ITEMS_GRAVITY_DAMPER = value; }

    public static double THROWN_ITEMS_TRAJECTORY_ROTATION = 0.03696;
    static void setTHROWN_ITEMS_TRAJECTORY_ROTATION(double value) { THROWN_ITEMS_TRAJECTORY_ROTATION = value; }
}
```

**Lines of code**: 30+ lines → 6 lines (80% reduction)
**Note**: Field/setter pairing keeps related code together

### Example 2: Reference Types (Vectors)

```java
public static class Direction {
    private static final Vector UP = new Vector(0, 1, 0);
    public static Vector UP() { return UP.clone(); }

    private static final Vector DOWN = new Vector(0, -1, 0);
    public static Vector DOWN() { return DOWN.clone(); }
}

// Usage (immutable via cloning):
Vector dir = Config.Direction.UP();  // Gets a clone, can't mutate original
```

### Example 3: Flattened Structure (2-Level Maximum)

```java
public static class Combat {
    // Attacks - Base configuration
    public static double ATTACKS_BASE_DAMAGE = 20.0;
    static void setATTACKS_BASE_DAMAGE(double value) { ATTACKS_BASE_DAMAGE = value; }

    // Attacks - Range Multipliers (flattened from nested class)
    public static double ATTACKS_RANGE_MULTIPLIERS_BASIC_1 = 1.4;
    static void setATTACKS_RANGE_MULTIPLIERS_BASIC_1(double value) { ATTACKS_RANGE_MULTIPLIERS_BASIC_1 = value; }

    public static double ATTACKS_RANGE_MULTIPLIERS_BASIC_2 = 1.4;
    static void setATTACKS_RANGE_MULTIPLIERS_BASIC_2(double value) { ATTACKS_RANGE_MULTIPLIERS_BASIC_2 = value; }

    public static double ATTACKS_RANGE_MULTIPLIERS_BASIC_3 = 1.4;
    static void setATTACKS_RANGE_MULTIPLIERS_BASIC_3(double value) { ATTACKS_RANGE_MULTIPLIERS_BASIC_3 = value; }
}

// Usage (2 levels maximum):
double damage = Config.Combat.ATTACKS_BASE_DAMAGE;
double range = Config.Combat.ATTACKS_RANGE_MULTIPLIERS_BASIC_1;

// NOT: Config.Combat.Attacks.RangeMultipliers.basic1 (4 levels - too deep!)
```

## Usage Guidelines

### Adding New Config Values

1. **Add static field** with default value (ALL_CAPS):
   ```java
   public static double NEW_PARAMETER = 42.0;
   ```

2. **Add package-private setter** immediately after (field/setter pairing):
   ```java
   static void setNEW_PARAMETER(double value) { NEW_PARAMETER = value; }
   ```

3. **Load in ConfigManager**:
   ```java
   Config.Section.setNEW_PARAMETER(
       get("section.subsection.new_parameter", 42.0)
   );
   ```

4. **Add to config.yaml**:
   ```yaml
   section:
     subsection:
       new_parameter: 42.0  # Description
   ```

**Important**: Always use ALL_CAPS naming and keep field/setter pairs together!

### Accessing Values

```java
// Direct field access (preferred)
double value = Config.Physics.THROWN_ITEMS_GRAVITY_DAMPER;

// In expressions
if (damage > Config.Combat.ATTACKS_BASE_DAMAGE * 2) {
    // ...
}

// Pass to methods
applyDamage(target, Config.Combat.ATTACKS_BASE_DAMAGE);

// Note: ALL_CAPS makes config constants visually distinct from variables
```

### Reference Types (Vectors)

```java
// CORRECT: Use clone method
Vector direction = Config.Direction.UP();

// WRONG: Direct field access (would expose mutable reference)
// Vector direction = Config.Direction.UP;  // Doesn't exist - use UP() method
```

## References

- [Issue #116: Implement New Config](https://github.com/Mere-Solace/Sword-Combat-Plugin/issues/116)
- [ADR 003: Hybrid Configuration Pattern](003-hybrid-configuration-pattern.md) - Superseded by this ADR
- Implementation files:
  - [Config.java](../../src/main/java/btm/sword/config/Config.java) - Static config class
  - [ConfigManager.java](../../src/main/java/btm/sword/config/ConfigManager.java) - Generic loading
  - [config.yaml](../../src/main/resources/config.yaml) - Configuration schema

## Status of Old System

**ADR 003 (Hybrid Pattern)**: Superseded

The hybrid pattern served as an intermediate step but is now replaced by the fully static approach. The old `*Config` section classes will be removed once migration is complete.

**Timeline**:
- ADR 003: Implemented 2025-11-03 (Issue #66)
- ADR 005: Implemented 2025-11-19 (Issue #116)
- Migration: ✅ Completed 2025-11-20 (All files updated)
