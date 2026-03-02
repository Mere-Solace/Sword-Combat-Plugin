# Stat System (EntityAspects)

## Overview

The stat system manages 12 named stats per entity, split into 4 regenerating **Resources** and 8 static **Aspects**. These stats drive all combat calculations including damage, cooldowns, movement speed, and ability costs.

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `AspectType` | `system/entity/aspect/AspectType.java` | Enum of 12 stat types. |
| `Aspect` | `system/entity/aspect/Aspect.java` | Base stat: holds `baseValue` and `effPercent` multiplier. Effective value = `baseValue * effPercent`. |
| `Resource` | `system/entity/aspect/Resource.java` | Extends `Aspect` with `curValue`, regeneration task (via `TimeArbiter`), regen period/amount, and percentage multipliers for dynamic buffs/debuffs. |
| `AspectValue` | `system/entity/aspect/value/AspectValue.java` | Data carrier for a single stat value (used in `CombatProfile`). |
| `ResourceValue` | `system/entity/aspect/value/ResourceValue.java` | Extends `AspectValue` with regen period and regen amount fields. |
| `EntityAspects` | `system/entity/base/EntityAspects.java` | Container object holding all 12 stats in a fixed-order array. Provides typed accessors for each stat. Constructed from a `CombatProfile`. |
| `CombatProfile` | `system/entity/base/CombatProfile.java` | Defines base stat values per entity. Loaded from `Config` defaults. Also holds `maxAirDodges` and `PlayerSkillContainer`. |

## The 12 Stats

### Resources (regenerate over time)

| Name | Purpose |
|------|---------|
| **SHARDS** | Health points. Entity dies when shards reach 0. |
| **TOUGHNESS** | Shield/armor layer. Must be broken before shards take damage. Regenerates with modified rate after breaking. |
| **SOULFIRE** | Mana/energy. Consumed by skills and abilities. Gained by hitting enemies (via `SoulfireManager.transferSoulfire`). |
| **FORM** | Experience. Displayed on the Minecraft XP bar. |

### Aspects (static modifiers)

| Name | Purpose |
|------|---------|
| **MIGHT** | Offensive power modifier. |
| **RESOLVE** | Defensive modifier. |
| **FINESSE** | Attack speed / timing modifier. Reduces attack cooldowns. |
| **PROWESS** | General combat effectiveness. |
| **ARMOR** | Damage reduction. |
| **FORTITUDE** | Grab cooldown reduction. |
| **CELERITY** | Dash cooldown reduction, movement enhancement. |
| **WILLPOWER** | Soulfire-related modifier. |

## Regeneration Mechanism

Each `Resource` runs a `TimeArbiter.TaskHandle` that periodically adds `effectiveRegenAmount()` to the current value. The regen period and amount can be dynamically modified via `effPeriodPercent` and `effAmountPercent` multipliers. Changing the period automatically restarts the regen task.

When toughness breaks, its `effAmountPercent` and `effPeriodPercent` are reduced (from `Config.Entity.HIT_TOUGH_BREAK_RECHARGE_*` values), making it regenerate slowly until it reaches a cutoff percentage, at which point normal regen resumes.

## Stat Calculations

`Combatant` provides helper methods that use aspects in formulas:

- `calcValueAdditive(stat, max, base, multiplier)` -- `min(max, base + multiplier * aspectVal)`
- `calcValueReductive(stat, min, base, multiplier)` -- `max(min, base - multiplier * aspectVal)`
- `calcCooldown(type, min, base, multiplier)` -- same as reductive, cast to int

## Damage Flow

1. `SwordEntity.hit()` is called with a `HitValuePacket` containing shard damage, toughness damage, soulfire loss, and reaping amounts.
2. Soulfire is transferred from the hit entity to the attacker via `SoulfireManager`.
3. Toughness is reduced first. If toughness reaches 0, it "breaks" -- triggering visual effects and modified regen.
4. While toughness is broken, shard damage is applied. If shards reach 0, the entity dies.
5. A toughness-break recovery mechanism tracks `shardsLostDuringToughnessBreak` and recharges toughness to a percentage when enough shards are lost.

## Dependencies

- **Config** -- Default stat values, combat constants
- **TimeArbiter** -- Regen tasks are time-bound (affected by global time scale)
- **SoulfireManager** -- Soulfire transfer particle effects and delivery

## Known Limitations

- All aspects default to `new AspectValue(1)` with no config-driven values beyond the 4 resources. The 8 modifier aspects are effectively hardcoded to 1.
- `CombatProfile` is constructed identically for all entity types with a TODO (#166) for dynamic loading.
- There is no buff/debuff stacking system; `effPercent` multipliers are set directly.
