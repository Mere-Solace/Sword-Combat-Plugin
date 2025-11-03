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

## Notes

This change is part of the broader configuration system overhaul (Issue #66) that migrated hardcoded values to a hot-reloadable configuration system. The sound system now properly integrates with the config infrastructure while also adhering to Adventure API best practices.
