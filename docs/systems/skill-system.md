# Skill System

## Overview

The skill system allows players to equip and activate skills in designated slots. Skills are registered in a static `SkillRegistry`, tracked per-player in a `PlayerSkillContainer`, and resolved at runtime into `InputAction`s via `SkillSlotActionFactory`.

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `Skill` | `system/action/skill/Skill.java` | Interface: `id()`, `type()`, `icon()`, `name()`, `description()`. |
| `SkillId` | `system/action/skill/SkillId.java` | Stable identifier for a skill. |
| `SkillIds` | `system/action/skill/SkillIds.java` | Constants for known skill IDs (SHADOW_SLASH, VOID_LUNGE, KNIFE_THROW, NONE, LOCKED). |
| `SkillType` | `system/action/skill/SkillType.java` | Enum: `UMBRAL`, `ACTIVE`, `PASSIVE`. |
| `SkillRegistry` | `system/action/skill/SkillRegistry.java` | Static `HashMap<SkillId, Skill>`. Registers `VoidLungeSkill` and `ShadowSlashSkill` in a static initializer. |
| `ActiveSkill` | `system/action/skill/type/ActiveSkill.java` | Abstract class implementing `Skill`. Adds `execute()`, `calculateCooldown()`, `canPerform()`. |
| `PassiveSkill` | `system/action/skill/type/PassiveSkill.java` | Abstract passive skill base. |
| `ConsumableActive` | `system/action/skill/type/ConsumableActive.java` | Consumable active skill variant. |
| `ShadowSlashSkill` | `system/action/skill/type/impl/umbral/ShadowSlashSkill.java` | Concrete umbral skill. |
| `VoidLungeSkill` | `system/action/skill/type/impl/umbral/VoidLungeSkill.java` | Concrete umbral skill. |
| `SkillSlot` | `system/action/skill/container/SkillSlot.java` | Enum: UMBRAL_1/2/3, ACTIVE_1/2, PASSIVE_1/2/3/CORE. Each has a `SkillType`. |
| `PlayerSkillContainer` | `system/action/skill/container/PlayerSkillContainer.java` | Manages available and equipped skills per player. Uses `EnumMap<SkillSlot, SkillId>` for equipment and `EnumMap<SkillType, Set<SkillId>>` for availability. |
| `SkillSlotActionFactory` | `system/action/skill/container/SkillSlotActionFactory.java` | Resolves a `SkillSlot` into an `InputAction` by looking up the equipped skill and building an `InputAction` from its `execute`, `calculateCooldown`, and `canPerform` methods. |
| `SkillSlotRules` | `system/action/skill/container/SkillSlotRules.java` | Validation rules for slot compatibility. |

## Skill Resolution Flow

1. Player inputs a skill combo (e.g., F, L, L for UMBRAL_1).
2. The `InputExecutionTree` node for that combo has `action(() -> SkillSlotActionFactory.create(owner, SkillSlot.UMBRAL_1))`.
3. `SkillSlotActionFactory.create()`:
   a. Gets `PlayerSkillContainer` from the player's `CombatProfile`.
   b. Looks up the `SkillId` equipped in the slot.
   c. Fetches the `Skill` from `SkillRegistry`.
   d. If it is an `ActiveSkill`, builds an `InputAction` delegating to the skill's methods.
4. The `InputAction` is then executed normally via `InputActionExecutor`.

## Equip/Unequip

`PlayerSkillContainer` enforces:

- Locked slots cannot be modified.
- Equipping a skill already in a different slot unequips it from the old slot (no duplicates).
- `freeSkillIds(type)` returns available skills of a type not currently equipped.
- The default constructor equips `SHADOW_SLASH` in UMBRAL_1 and `VOID_LUNGE` in UMBRAL_2 for testing.

## Dependencies

- **CombatProfile** -- Owns the `PlayerSkillContainer`
- **InputExecutionTree** -- Skill combos registered as input sequences
- **SkillSlotActionFactory** -- Bridge between skill system and input system
- **Menu System** -- `SkillSelectionMenu` for equipping skills via GUI

## Known Limitations

- Only 2 skills are implemented (ShadowSlash, VoidLunge). KNIFE_THROW is referenced in default equips but not registered.
- Passive skills have no activation or effect system; the `PassiveSkill` class exists but has no concrete implementations or tick-based application.
- `SkillSlotActionFactory.create()` has a hardcoded check `if (resolvedSkill instanceof VoidLungeSkill)` for cast duration, which does not scale.
- The skill system has no leveling, upgrade paths, or unlock conditions.
