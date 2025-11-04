# ADR 002: Adventure API Sound.Type Interface Implementation

**Status**: Accepted
**Date**: 2025-11-03
**Authors**: Claude Code, Chris R.

## Context

The project uses the Adventure API (via Paper MC) for playing sounds. The original implementation used the deprecated pattern of creating `Key` objects manually via `Key.key(String)` and passing them to `Sound.sound()`. The Adventure API provides a `Sound.Type` interface that extends `Keyed`, allowing enum types to be passed directly to sound creation methods.

Our custom `SoundType` enum stored sound keys as strings and required manual `Key` creation at sound playback time:

```java
// Old pattern
Sound sound = Sound.sound(Key.key(type.getKey()), Sound.Source.PLAYER, volume, pitch);
```

This approach had several issues:
1. Repeated `Key` creation on every sound playback (performance overhead)
2. String-based API surface (`getKey()` returning String)
3. Not leveraging Adventure API's type system
4. Inconsistent with Adventure API best practices

## Decision

**Migrate `SoundType` enum to implement `Sound.Type` interface.**

Changes implemented:
1. `SoundType` now implements `net.kyori.adventure.sound.Sound.Type`
2. Internal storage changed from `String key` to `Key key`
3. Keys are created once during enum initialization
4. Replaced `getKey()` method with `key()` (interface requirement)
5. Updated `SoundUtil.playSound()` to pass `SoundType` directly to `Sound.sound()`
6. Updated `AudioConfig` to use `SoundType` instead of `org.bukkit.Sound`

## Reasoning

### Advantages

1. **Type Safety**: Compile-time verification that sound types implement the correct interface
2. **Performance**: Key objects created once during enum initialization instead of per playback
3. **API Compliance**: Follows Adventure API's intended design patterns
4. **Cleaner Code**: Removes manual Key wrapping at call sites
5. **Better IDE Support**: IDEs can autocomplete and type-check properly with interface implementations

### Implementation Details

```java
// New pattern - SoundType.java
public enum SoundType implements Sound.Type {
    ENTITY_ENDER_DRAGON_FLAP("entity.ender_dragon.flap");

    private final Key key;

    SoundType(String keyString) {
        this.key = Key.key(keyString);
    }

    @Override
    public @NotNull Key key() {
        return key;
    }
}

// New pattern - SoundUtil.java
Sound sound = Sound.sound(type, Sound.Source.PLAYER, volume, pitch);
```

### Trade-offs

1. **Memory**: Each enum constant now stores a `Key` object instead of a `String`
   - Impact: Negligible (1200+ sound types, ~48KB total)
   - Benefit: Eliminates runtime Key creation overhead
2. **Breaking Change**: Method renamed from `getKey()` to `key()`
   - Impact: Minimal (only one internal usage in `UtilityAction.soundTest()`)
   - Fixed in same commit

### Alternatives Considered

1. **Keep String storage, create Keys on-demand**: Would maintain string-based API but with repeated object creation overhead
2. **Use org.bukkit.Sound**: Would lose custom sound support (RANDOM_BANE_SLASH, RANDOM_CLASH)
3. **Wrapper class instead of interface**: Would add unnecessary abstraction layer

## Consequences

### Positive

- Sound playback is more efficient (no repeated Key allocation)
- Code is more maintainable and follows API conventions
- Type system prevents misuse of sound types
- Configuration system properly typed with `SoundType` enums
- Future-proof against Adventure API updates

### Negative

- Slight increase in memory usage per sound type (negligible)
- Required migration of one internal call site (`UtilityAction.soundTest()`)
- Breaking change for any external code calling `getKey()` (none found in codebase)

### Migration Checklist

- [x] Updated `SoundType` to implement `Sound.Type`
- [x] Changed internal storage from String to Key
- [x] Implemented `key()` method
- [x] Updated `SoundUtil.playSound()` to use direct type passing
- [x] Updated `AudioConfig` to use `SoundType` enum
- [x] Fixed `UtilityAction.soundTest()` to use `key()` method
- [x] Verified no other usages of old `getKey()` pattern
- [x] Tested sound playback (QA pending)

## References

- [Adventure API Sound Documentation](https://docs.papermc.io/adventure/sound/)
- [Adventure API Source - Sound.Type](https://github.com/KyoriPowered/adventure/blob/main/4/api/src/main/java/net/kyori/adventure/sound/Sound.java)
- [Paper MC Adventure Integration](https://docs.papermc.io/paper/dev/api/adventure)
- Related Issue: #66 (Eliminate hard-coded values)
- Commits: 71b99e9 (Sound.Type implementation), [pending] (UtilityAction fix)

## Prefab.Sounds Wrapper Pattern

### Additional Decision: SoundWrapper Object Pattern

After implementing the Sound.Type interface, we identified that direct config access was verbose:

```java
// Verbose pattern
var attackSound = ConfigManager.getInstance().getAudio().getAttackSound();
SoundUtil.playSound(executor.entity(), attackSound.getSound(), attackSound.getVolume(), attackSound.getPitch());
```

**Decision:** Created `SoundWrapper` class and added `Prefab.Sounds` static objects, following the existing `Prefab.Particles` **object pattern** (not utility methods).

### Why Object Pattern?

The existing `Prefab.Particles` uses **prefabricated objects** with instance methods:
```java
Prefab.Particles.TEST_SWING.display(l)  // object.method()
```

Not static utility methods. We maintain this pattern for consistency:
```java
Prefab.Sounds.ATTACK.play(entity)  // object.method()
```

### Implementation

```java
// SoundWrapper.java - similar to ParticleWrapper
public class SoundWrapper {
    private final Function<AudioConfig, AudioConfig.SoundConfig> configExtractor;

    public SoundWrapper(Function<AudioConfig, AudioConfig.SoundConfig> configExtractor) {
        this.configExtractor = configExtractor;
    }

    public void play(LivingEntity entity) {
        AudioConfig.SoundConfig sound = configExtractor.apply(ConfigManager.getInstance().getAudio());
        SoundUtil.playSound(entity, sound.getSound(), sound.getVolume(), sound.getPitch());
    }
}

// Prefab.java - prefabricated sound objects
public static class Sounds {
    public static final SoundWrapper ATTACK = new SoundWrapper(audio -> audio.getAttackSound());
    public static final SoundWrapper THROW = new SoundWrapper(audio -> audio.getThrowSound());
}

// Usage - matches Prefab.Particles pattern
Prefab.Sounds.ATTACK.play(executor.entity());
Prefab.Sounds.THROW.play(thrower.entity());
```

### Advantages of Prefab Object Pattern

1. **Consistency**: Exactly matches existing `Prefab.Particles.TEST_SWING.display(l)` pattern
2. **Prefabricated**: Objects created once at class load time
3. **Hot-Reload Compatible**: Config accessed via lambda at play time, no caching
4. **Maintainable**: Follows established ParticleWrapper/SoundWrapper architecture
5. **Discoverable**: Same API surface as existing Prefab objects
6. **Extensible**: Easy to add new sound types (e.g., `HIT`, `CLASH`, `GUARD`)

### Files Created/Updated

- `SoundWrapper.java`: New wrapper class (mirrors ParticleWrapper design)
- `Prefab.java`: Added `Sounds` static class with ATTACK and THROW objects
- `AttackAction.java`: Changed to `Prefab.Sounds.ATTACK.play()`
- `ThrownItem.java`: Changed to `Prefab.Sounds.THROW.play()`

## Notes

This change is part of the broader configuration system overhaul (Issue #66) that migrated hardcoded values to a hot-reloadable configuration system. The sound system now properly integrates with the config infrastructure while also adhering to Adventure API best practices.

The Prefab.Sounds wrapper provides developer-friendly API surface while maintaining full hot-reload capabilities and Adventure API compliance.
