# Player Data Persistence

## Overview

The player data system manages persistent per-player data via JSON serialization using Gson. Each player has a `PlayerData` object containing their UUID, first login date, and `CombatProfile`.

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `PlayerData` | `system/playerdata/PlayerData.java` | Data object: UUID, `dateOfFirstLogin`, `CombatProfile`. |
| `PlayerDataManager` | `system/playerdata/PlayerDataManager.java` | Static manager: load/save from `plugins/sword/playerdata.json`, register players, lookup by UUID. Uses Gson with `RuntimeTypeAdapterFactory` for polymorphic `AspectValue` serialization. |
| `SwordClassType` | `system/playerdata/SwordClassType.java` | Enum of character class types (e.g., `SWORD_THROWER`). |
| `RuntimeTypeAdapterFactory` | `utility/data/RuntimeTypeAdapterFactory.java` | Gson adapter factory for polymorphic type serialization (distinguishes `AspectValue` from `ResourceValue`). |

## Serialization

Gson is configured with a `RuntimeTypeAdapterFactory` that adds a `"type"` discriminator field:

- `"aspect"` -> `AspectValue`
- `"resource"` -> `ResourceValue`

Data file: `plugins/sword/playerdata.json` containing a `Map<UUID, PlayerData>`.

## Lifecycle

1. `PlayerDataManager.initialize()` loads existing data from disk and registers all online players.
2. `register(LivingEntity)` creates a new `PlayerData` if none exists (using `putIfAbsent`).
3. `PlayerDataManager.shutdown()` saves all data to disk.

**Critical Note**: `PlayerDataManager.shutdown()` is currently **commented out** in `Sword.onDisable()` with a TODO (#129). Data is not persisted on server shutdown.

## Dependencies

- **CombatProfile** -- Stored within `PlayerData`
- **Gson** -- JSON serialization
- **RuntimeTypeAdapterFactory** -- Polymorphic type handling

## Known Limitations

- **Persistence is disabled** -- The `shutdown()` call in `Sword.onDisable()` is commented out, meaning no data survives server restarts.
- `PlayerData` is minimal -- only UUID, date, and `CombatProfile`. No skill unlock tracking, match history, or settings.
- `CombatProfile` is recreated with defaults on every new `PlayerData` construction; there is no profile migration or versioning.
- File-based JSON storage does not scale well for large player counts. A database backend would be needed for production use.
- Error handling in `loadPlayerData()` throws a `RuntimeException` on IO failure, which would crash the plugin.
