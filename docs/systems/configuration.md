# Configuration System

## Overview

The configuration system provides centralized, type-safe, hot-reloadable access to all tunable values. It uses a self-registering `ConfigEntry` pattern where each config value declares its YAML path, default, type, loader, and assignment lambda.

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `Config` | `config/Config.java` | Static class containing all config values as static fields organized in inner classes (Direction, SwordColor, Angle, Physics, Movement, Combat, Entity, Audio, Display, etc.). Each field has a `static { register(...) }` block. |
| `Config.ConfigEntry<T>` | Inner class | Holds: `path` (YAML key), `defaultValue`, `type` (Class), `assign` (Consumer to set field), `loader` (functional interface for YAML parsing). |
| `ConfigManager` | `config/ConfigManager.java` | Singleton. Manages `config.yaml` file lifecycle: create from defaults, load, reload, save, reset. Iterates `Config.ENTRIES` list to load all values. |

## How It Works

1. Each config value is a `public static` field in a nested class of `Config`.
2. Immediately after the field declaration, a `static { register(...) }` block adds a `ConfigEntry` to `Config.ENTRIES`.
3. On `ConfigManager.loadConfig()`, each entry's `loader` reads from the YAML `ConfigurationSection` and the `assign` consumer updates the static field.
4. If a YAML key is missing, the `defaultValue` is used.

### Example

```java
public static double DASH_BASE_POWER = 3;
static { register(
    "movement.dash_base_power",
    DASH_BASE_POWER, Double.class,
    v -> DASH_BASE_POWER = v,
    ConfigurationSection::getDouble
); }
```

## Config Sections

| Section | Purpose |
|---------|---------|
| `Direction` | Immutable directional vectors (UP, DOWN, NORTH, SOUTH). Not from YAML. |
| `SwordColor` | TextColor and Color values for UI elements. |
| `Angle` | Rotation constants (e.g., umbral blade idle period). |
| `Physics` | Thrown item physics, attack velocity, gravity. |
| `Movement` | Dash power, distances, delays, particle/sound params. |
| `Combat` | Attack timing, hitbox radius, damage multipliers, attack profiles. |
| `Entity` | Combat profile defaults (shards, toughness, soulfire, form), toughness break params. |
| `Audio` | Volume and pitch for combat sounds. |
| `Display` | Status display brightness values. |

## Hot Reload

`/sword reload` calls `ConfigManager.getInstance().reload()` which re-reads `config.yaml` and re-applies all entries. This allows runtime tuning without server restart.

## Custom Loaders

Special types have dedicated loaders:

- `loadTextColor` -- Parses hex color strings
- `loadColor` -- Parses hex to `org.bukkit.Color`
- `loadFloat` -- Casts double to float
- `loadEnum` -- Generic enum parser
- `loadSoundType` -- SoundType enum
- `loadAttackType` -- AttackType enum
- `loadStringList` -- List of strings
- `loadEntityTypeList` -- List of EntityType enums

## Dependencies

- **Sword plugin instance** -- For data folder and resource loading
- Referenced by virtually every system for constants and tuning

## Known Limitations

- `Config.java` is very large (500+ lines) and growing. As more features are added, it may become difficult to navigate.
- There is no validation on loaded values (e.g., ensuring dash power > 0).
- The `Direction` class is not configurable -- its values are hardcoded constants with clone-based immutability.
- Config values are static mutable fields, which means they are not thread-safe if accessed from async contexts (though in practice all access is on the main thread).
