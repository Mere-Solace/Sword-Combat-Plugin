# Entity System

## Overview

The entity system provides a wrapper layer over Bukkit's `LivingEntity` hierarchy, adding combat resources, affliction tracking, status displays, and lifecycle management. Every entity that participates in the Sword combat system is wrapped in a `SwordEntity` subclass and registered in the `SwordEntityArbiter`.

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `SwordEntity` | `system/entity/base/SwordEntity.java` | Abstract base wrapping `LivingEntity`; provides resources via `EntityAspects`, hit/damage logic, status display, ticking, item management, basis vectors, and impalement tracking. |
| `Combatant` | `system/entity/impl/Combatant.java` | Abstract; adds combat capabilities: grab, dash, throw, UmbralBlade ownership, ability casting locks, and stat calculation helpers. |
| `SwordPlayer` | `system/entity/impl/SwordPlayer.java` | Player-specific combatant: owns `InputExecutionTree`, `PlayerMenuManager`, handles right-click hold / sneak hold detection, visual stats (health bar, exp bar, target indicator), inventory upkeep, and dummy management. |
| `Hostile` | `system/entity/impl/Hostile.java` | Mob combatant wrapping `Mob`; has `Pathfinder`, equipment, and stub AI methods (patrol, approach, charge, retreat, flee, randomAttack). |
| `Passive` | `system/entity/impl/Passive.java` | Minimal wrapper for non-combat entities (animals, villagers). |
| `Dummy` | `system/entity/impl/Dummy.java` | Test target extending `Passive`; overrides hit logic to prevent death (resets shards instead) and to skip knockback. Owned by a `SwordPlayer`. |
| `SwordEntityArbiter` | `system/entity/SwordEntityArbiter.java` | Static registry mapping `UUID -> SwordEntity`. Maintains two separate maps: `onlineSwordPlayers` and `existingSwordNPCs`. Provides `getOrAdd()`, `register()`, `remove()`, and bulk operations. |

## Hierarchy

```
SwordEntity (abstract)
 +-- Passive
 |    +-- Dummy
 +-- Combatant (abstract)
      +-- SwordPlayer
      +-- Hostile
```

## Lifecycle

1. **Registration** -- When a `LivingEntity` enters the world or a player joins, `SwordEntityArbiter.register()` is called. For players, `PlayerDataManager.register()` is invoked first to ensure data exists, then a `SwordPlayer` is created. For NPCs, `initializeNPC()` dispatches by `EntityType` to create the appropriate subclass.
2. **Ticking** -- `SwordEntity` constructor calls `startTicking()`, which schedules a `TimeArbiter` task running every 50ms. The `onTick()` method handles invulnerability timers, grounded checks, air dash resets, impalement slowness, and status display updates.
3. **Spawning** -- `onSpawn()` resets resources and tick counter, applies movement speed from `TimeArbiter`.
4. **Death** -- `onDeath()` ends the status display, stops resource tasks, and marks non-player entities as destroyed. Players are never marked destroyed on death (they respawn).
5. **Removal** -- For players, `onLeave()` disposes the UmbralBlade, ends displays, and marks as destroyed. NPCs are removed from the arbiter when their entity is removed from the world.

## Status Display

Each `SwordEntity` has a `TextDisplay` entity riding on top of it showing name, HP (shards), and toughness as a colored bar. The display is auto-recreated if it becomes invalid. Players hide their own display so they do not see it above themselves.

## Dependencies

- **EntityAspects / CombatProfile** -- Resources and stats
- **TimeArbiter / SwordScheduler** -- Ticking and scheduled tasks
- **InputExecutionTree** -- Player input handling (SwordPlayer only)
- **UmbralBlade** -- Per-combatant weapon (Combatant only)
- **PlayerDataManager** -- Persistent data (SwordPlayer only)

## Known Limitations

- The `Hostile` class has extensive stub methods (`surround`, `approach`, `charge`, `retreat`, `flee`) with empty implementations. AI behavior is not yet functional.
- `Passive` is a thin wrapper with no meaningful behavior beyond the base class.
- `SwordEntityArbiter.initializeNPC()` uses a massive switch statement to classify entity types, which must be manually updated when new entity types are added to Minecraft.
- The entity hit logic in `Dummy` duplicates most of `SwordEntity.hit()` rather than calling `super.hit()` with overrides, creating maintenance risk.
