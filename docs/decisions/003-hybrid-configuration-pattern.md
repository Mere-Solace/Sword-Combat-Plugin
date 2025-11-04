# ADR 003: Hybrid Configuration Pattern

**Status**: Accepted
**Date**: 2025-11-03
**Authors**: Mere Solace

## Context

During the implementation of Issue #66 (Eliminate Hard-Coded Values), the initial approach created excessive nested static classes for configuration values. This over-modularization created significant boilerplate overhead:

**Before (Over-Modularized)**:
```java
// 3 simple values requiring 15+ lines of boilerplate
public static class ThrowPreparationConfig {
    private final PotionEffectType effect;
    private final int duration;
    private final int amplifier;

    public ThrowPreparationConfig(ConfigurationSection section) {
        // 10+ lines of loading logic
    }
    // 3 getters
}
```

**After (Hybrid Pattern)**:
```java
// 3 simple values as direct fields - 3 lines total
private final PotionEffectType throwPreparationEffect;
private final int throwPreparationDuration;
private final int throwPreparationAmplifier;
```

This excessive modularization slowed development velocity and added cognitive overhead when adding new configuration values.

## Decision

**We use a hybrid configuration pattern: simple configs (2-3 values) are flattened to direct fields with Lombok @Getter, while complex hierarchical configs (5+ values or reusable sub-structures) retain nested static classes.**

### Pattern Rules

1. **Flatten to Direct Fields When**:
   - Config group has 2-3 simple values
   - No sub-hierarchy or reusable structure
   - Values are conceptually independent
   - Example: throw preparation (effect, duration, amplifier)

2. **Keep Nested Classes When**:
   - Config group has 5+ related values
   - Represents a reusable sub-structure
   - Has meaningful sub-hierarchy
   - Example: combat profile (shards, toughness, soulfire, form resources)

3. **Use Fully Flat When**:
   - Config has 30+ independent parameters
   - Values don't form natural groupings
   - Example: MovementConfig (40+ dash/toss parameters)

## Reasoning

### Advantages

1. **Development Velocity**: Adding a new config value drops from 15 lines (nested class + getter + loading) to 1 line (direct field)
2. **Reduced Boilerplate**: 60% reduction in wrapper class count (EntityConfig: 5 nested classes → 2)
3. **Code Clarity**: Direct field names like `throwPreparationEffect` are self-documenting
4. **Maintained Structure**: Complex configs (CombatProfileConfig) keep organization where it adds value
5. **Lombok Integration**: `@Getter` on class level eliminates getter boilerplate

### Trade-offs

1. **Longer Field Names**: Flattened fields use prefixed names (`dashMaxDistance` vs nested `dash.maxDistance`)
2. **IDE Autocomplete**: Nested classes group related fields in IDE autocomplete; flat fields appear alphabetically
3. **Judgment Required**: Developers must decide when to flatten vs nest (mitigated by clear rules above)

### Alternatives Considered

- **Always Flat (Option 1)**: Would lose valuable structure for complex configs like CombatProfile
- **Always Nested (Current Problem)**: Creates excessive boilerplate for simple configs
- **Builder Pattern (Option 3)**: Adds complexity without significant benefit for our use case

## Consequences

### Positive

- **93% faster** to add new config values (15 lines → 1 line)
- **60% reduction** in config wrapper classes across codebase
- Simpler onboarding for contributors adding configuration
- Faster iteration during live testing with `/sword reload`

### Negative

- Mixed patterns require understanding when to use each approach
- Refactoring existing nested configs requires careful field naming

## Implementation Examples

### Example 1: Flattened Simple Config (EntityConfig.java)

```java
@Getter
public class EntityConfig {
    // Flattened throw preparation (3 simple values - no wrapper needed)
    private final PotionEffectType throwPreparationEffect;
    private final int throwPreparationDuration;
    private final int throwPreparationAmplifier;

    public EntityConfig(FileConfiguration config) {
        ConfigurationSection throwPrep = entities.getConfigurationSection("throw_preparation");
        if (throwPrep != null) {
            this.throwPreparationEffect = PotionEffectType.getByName(throwPrep.getString("effect", "SLOWNESS"));
            this.throwPreparationDuration = throwPrep.getInt("duration", 1);
            this.throwPreparationAmplifier = throwPrep.getInt("amplifier", 2);
        } else {
            // Inline defaults
        }
    }
}
```

### Example 2: Nested Complex Config (EntityConfig.java)

```java
@Getter
public class EntityConfig {
    // Keep nested for complex hierarchical config (12 values, reusable structure)
    private final CombatProfileConfig combatProfile;

    @Getter
    public static class CombatProfileConfig {
        private final float shardsCurrent;
        private final int shardsRegenPeriod;
        private final float shardsRegenAmount;
        // ... 9 more resource-related fields

        public CombatProfileConfig(ConfigurationSection section) {
            // Structured loading preserves ResourceValue parameter mapping
        }
    }
}
```

### Example 3: Fully Flat Config (MovementConfig.java)

```java
@Getter
public class MovementConfig {
    // 40+ independent parameters - fully flattened (no meaningful sub-grouping)
    private final double dashMaxDistance;
    private final long dashCastDuration;
    private final double dashBasePower;
    private final double dashInitialOffsetY;
    // ... 36+ more fields

    public MovementConfig(FileConfiguration config) {
        ConfigurationSection dash = movement.getConfigurationSection("dash");
        this.dashMaxDistance = dash.getDouble("max_distance", 10.0);
        this.dashCastDuration = dash.getLong("cast_duration", 5L);
        // ... direct loading with defaults
    }
}
```

## Usage Guidelines

When adding new configuration values:

1. **Count the values**: 2-3 values = flatten, 5+ values = consider nesting
2. **Check hierarchy**: Does grouping add semantic meaning? If no, flatten
3. **Check reusability**: Will this structure be used elsewhere? If yes, nest
4. **Naming convention**: Flattened fields use `{section}{PropertyName}` (e.g., `dashMaxDistance`)
5. **Lombok @Getter**: Always use class-level `@Getter` to eliminate getter boilerplate

## References

- [Issue #66: Eliminate Hard-Coded Values](https://github.com/Mere-Solace/Sword-Combat-Plugin/issues/66)
- Implementation commit: `3070420` (Migrate combat profile stats to configuration system)
- Reference implementations:
  - [EntityConfig.java](../../src/main/java/btm/sword/config/section/EntityConfig.java) - Hybrid pattern
  - [MovementConfig.java](../../src/main/java/btm/sword/config/section/MovementConfig.java) - Fully flat pattern
  - [config.yaml](../../src/main/resources/config.yaml) - Configuration schema
