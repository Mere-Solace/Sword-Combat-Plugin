# Skill & Ability System

## Overview

The skill system manages the player's equipped abilities — combat skills tied to the UmbralBlade, throwable and consumable active abilities, and passive modifiers. It covers four concerns:

1. **Skill identity and registration** — what skills exist (`SkillId`, `SkillIds`, `SkillRegistry`)
2. **Skill behavior contracts** — what a skill does and how it activates (`Skill`, `ActiveSkill`, `AbilitySkill`, and their subclasses)
3. **Per-player state** — which skills a player has unlocked and equipped (`PlayerSkillContainer`, `SkillSlot`)
4. **Input resolution** — bridging equipped skills into the `InputExecutionTree` (`SkillSlotActionFactory`)

Skills live in the player's `CombatProfile` via `PlayerSkillContainer`. The `InputRegistrar` reads the container through `SkillSlotActionFactory` to wire each slot dynamically into the trie-based input system at skill-node resolution time.

---

## Class Hierarchy

```
Skill (interface)
│   id(), type(), icon(), name(), description()
│
├── ActiveSkill (abstract)
│   │   execute(Combatant), calculateCooldown(Combatant), canPerform(Combatant)
│   │   requiresHold() → false by default
│   │   getCachedInputAction() / setCachedInputAction() — cooldown cache
│   │
│   ├── ShadowSlashSkill          — UMBRAL slot; stub execute (placeholder)
│   ├── VoidLungeSkill            — UMBRAL slot; blinks behind targeted entity, reclaims blade
│   │
│   └── AbilitySkill (interface, also extends Skill)
│       │   abilityType(), buildWorldItem(), consumesOnUse(), requiresPhysicalItemToUse()
│       │   useTypes(), maxUses(), maxDurability(), cooldownTicks(), isWeapon()
│       │
│       ├── ActivatableAbility (abstract extends ActiveSkill implements AbilitySkill)
│       │   │   Press to activate; cooldown only; item never consumed
│       │   │
│       │   ├── ConsumableAbility (abstract)
│       │   │       One-shot activation; item consumed on use; STACK use type by default
│       │   │
│       │   ├── ThrowableAbility (abstract)
│       │   │   │   Hold-activated throw; item consumed per throw; STACK use type by default
│       │   │   │   requiresHold() → true
│       │   │   │
│       │   │   └── KnifeThrowAbility  — ACTIVE slot; STACK + COOLDOWN; isWeapon = true
│       │   │
│       │   └── QuestAbility (abstract)
│       │           Activatable like normal but also surrenderable as a quest turn-in
│       │           onSurrender(Combatant) — item removal handled externally by AbilitySurrenderUtil
│       │
│       └── PassiveAbilitySkill (abstract extends PassiveSkill implements AbilitySkill)
│               Always active while equipped; never consumed
│
├── PassiveSkill (abstract)
│       Default AMETHYST_SHARD icon; no activation logic (passive effect only)
│
└── ConsumableActive (abstract extends ActiveSkill)
        Stub; intended for active skills that track uses/repair (no concrete implementations yet)
```

### Key distinction: `ActiveSkill` vs `AbilitySkill`

- `ActiveSkill` — any skill that has an executable `execute()` method and a cooldown. Covers both UmbralBlade skills (slot-bound, no physical item) and ability skills (physical world item).
- `AbilitySkill` — a marker interface that adds the world-item contract (`buildWorldItem()`, consumption rules, use-type tracking). **Not all `ActiveSkill`s are `AbilitySkill`s.** UmbralBlade skills like `ShadowSlashSkill` and `VoidLungeSkill` extend `ActiveSkill` directly — they have no physical item.

---

## Enums

### `SkillType`

```
UMBRAL   — Tied to the UmbralBlade; equipped in UMBRAL_1/2/3 slots
ACTIVE   — Physical-item-backed ability; equipped in ACTIVE_1/2 slots
PASSIVE  — Always-on modifier; equipped in PASSIVE_CORE/1/2/3 slots
```

### `AbilityType`

Classifies the interaction pattern of an `AbilitySkill`:

| Value | Meaning |
|---|---|
| `ACTIVATABLE` | Press to activate; cooldown only; item never consumed |
| `THROWABLE` | Held right-click to throw; physical item consumed per throw |
| `CONSUMABLE` | One-shot activation; physical item consumed on use |
| `PASSIVE` | Always active while equipped; no input required |
| `QUEST` | Activatable + surrenderable as a quest turn-in |

**Important: `AbilityType` is currently never consumed by any system.** Every concrete `AbilitySkill` subclass declares it via `abilityType()`, but no caller ever reads the return value — no switch statements, no serialization, no UI dispatch. The type information is structurally encoded in the class hierarchy instead (e.g., `instanceof ThrowableAbility`). `AbilityType` is either planned for future serialization/UI use or is genuinely dead code.

### `AbilityUseType`

Controls how an ability slot's resource is depleted. Multiple types may be combined on a single ability (they are non-exclusive):

| Value | Effect |
|---|---|
| `STACK` | Each activation decrements item stack count; slot depletes at zero |
| `DURABILITY` | Each activation degrades the item's durability bar; slot depletes when broken |
| `COOLDOWN` | Applies a Minecraft item-cooldown overlay; never depletes the slot |

`AbilitySlotManager` (in `item/special/`) is the actual executor of use-type logic — it reads `useTypes()`, `maxUses()`, `maxDurability()`, and `cooldownTicks()` from the ability and applies them to the player's hotbar item each time `consumeUse()` is called.

---

## `SkillId` and `SkillIds`

`SkillId` is a thin record wrapper over a Bukkit `NamespacedKey`. It provides:
- `SkillId.of(namespace, value)` — primary factory
- `SkillId.parse(serialized)` — for deserialization from `"namespace:value"` strings
- `asString()` — returns `"namespace:value"` for storage

`SkillIds` is a non-instantiable constants class holding every registered skill ID:

```
sword:none                       — sentinel for an empty slot
sword:locked                     — sentinel for a locked (unavailable) slot
sword:umbral_blade.shadow_slash  — UmbralBlade skill slot 1
sword:umbral_blade.void_lunge    — UmbralBlade skill slot 2
sword:active.knife_throw         — Active ability slot
sword:passive.bleed_mastery      — Passive skill (registered as ID only; no impl yet)
```

`SkillIds.getAll()` returns the full `ALL` list. **When adding a new skill, you must add its `SkillId` constant here AND add it to `ALL`.** The class has two reminder comments for this; missing the `ALL` entry will cause the skill to never appear in player skill pools.

---

## `SkillRegistry`

A static `HashMap<SkillId, Skill>` populated via a `static {}` block. No Bukkit API is needed at registration time, so initialization is safe at class-load.

```java
// Registration
register(new VoidLungeSkill());
register(new ShadowSlashSkill());
register(new KnifeThrowAbility());

// Lookup
Skill skill = SkillRegistry.get(SkillIds.VOID_LUNGE);
```

`get()` returns `null` for unregistered IDs — callers must null-check. Note that `SkillIds.NONE` and `SkillIds.LOCKED` are never registered; they are sentinel values only and will always return `null` from `get()`.

---

## `SkillSlot` and `PlayerSkillContainer`

### `SkillSlot`

Nine slots partitioned by type:

| Slot | Type |
|---|---|
| `UMBRAL_1`, `UMBRAL_2`, `UMBRAL_3` | `UMBRAL` |
| `ACTIVE_1`, `ACTIVE_2` | `ACTIVE` |
| `PASSIVE_CORE`, `PASSIVE_1`, `PASSIVE_2`, `PASSIVE_3` | `PASSIVE` |

`SkillSlotRules.canEquip(skill, slot)` enforces type compatibility — a skill can only be equipped in a slot whose type matches `skill.type()`.

### `PlayerSkillContainer`

Owned by `CombatProfile`, which is owned by each `SwordPlayer`. Tracks two things:

1. **Available skills** — skills the player has unlocked, stored as `EnumMap<SkillType, Set<SkillId>>`. Populated from `PlayerDataStore` on login; new players get all skills from `SkillIds.getAll()` (the no-arg constructor for testing).
2. **Equipped skills** — `EnumMap<SkillSlot, SkillId>` mapping each slot to an equipped skill (or `NONE`/`LOCKED`).

Key behaviors:
- `equip(slot, id)` — puts the skill in the slot; if the same skill was already in another slot, that other slot is cleared to `NONE`. Skills cannot be equipped in two slots simultaneously.
- `unlock(slot)` — transitions a slot from `LOCKED` to `NONE` (unlocks it for equipping).
- `freeSkillIds(type)` — returns skills of the given type that are available but not currently equipped in any slot. Used by the skill selection menu to populate options.
- `allAvailableSkillIds()` — flat list of every unlocked skill; used by the persistence layer on save.

---

## `SkillSlotActionFactory`

Bridges `PlayerSkillContainer` into the `InputExecutionTree`. Called by `InputRegistrar` when building the trie node for each skill slot.

```java
// tap-activated skill (UMBRAL or non-throwable ACTIVE)
InputAction action = SkillSlotActionFactory.create(player, SkillSlot.UMBRAL_1);

// hold-activated skill (ACTIVE with requiresHold() == true)
InputAction action = SkillSlotActionFactory.create(player, SkillSlot.ACTIVE_1, true);
```

Behavior:
- Returns `null` if no skill is equipped, if the equipped skill is not an `ActiveSkill`, or if the hold-variant flag mismatches `skill.requiresHold()`. A `null` return makes the input branch silently inert.
- Caches the built `InputAction` on the `ActiveSkill` instance via `getCachedInputAction()` / `setCachedInputAction()`. This preserves the cooldown state (`timeLastExecuted`) across dynamic re-resolves, since skill-slot trie nodes re-invoke the supplier on every step rather than holding a static reference.
- Hardcodes two skill-specific overrides for `VoidLungeSkill` — cast duration (250 ms) and soulfire cost (40). This coupling is a known smell: the factory contains skill-specific knowledge that belongs on the skill itself.

---

## History System

`PlayerAbilityHistory` is a session-scoped, chronologically-ordered log of ability events per player. It tracks two event types via `AbilityHistoryAction`:
- `USED` — a consumable or throwable ability's physical item was consumed
- `SURRENDERED` — a quest ability was surrendered as a turn-in

`AbilityHistoryEntry` records: the skill ID, the display name at event time (captured as plain text via `PlainTextComponentSerializer`), the action type, and an epoch-millisecond timestamp.

The history is loaded from the database on login and saved on quit via `AbilityHistoryRepository`. Item removal and `recordSurrender()` are triggered externally by `AbilitySurrenderUtil` (not in this package).

---

## How to Add a New Skill

### 1. Choose the right base class

| Goal | Extend |
|---|---|
| UmbralBlade-slot skill (no physical item) | `ActiveSkill` |
| Press-to-activate item ability | `ActivatableAbility` |
| Throw-on-hold item ability | `ThrowableAbility` |
| One-shot consumable item ability | `ConsumableAbility` |
| Quest turn-in item ability | `QuestAbility` |
| Always-on passive item ability | `PassiveAbilitySkill` |

### 2. Implement required methods

For `ActiveSkill`-based skills:
- `id()` — return your `SkillIds` constant
- `type()` — return `SkillType.UMBRAL`, `ACTIVE`, or `PASSIVE`
- `execute(Combatant)` — the effect
- `calculateCooldown(Combatant)` — cooldown in milliseconds
- `canPerform(Combatant)` — pre-cast guard
- `icon()` — menu display item
- `name()` — Adventure `Component`
- `description()` — list of `Component`s

For `AbilitySkill`-based skills, also implement:
- `buildWorldItem()` — build the base `ItemStack`, then call `AbilityItemBuilder.tag(item, id())` to stamp the PDC identity key
- Override `useTypes()`, `maxUses()`, `maxDurability()`, `cooldownTicks()`, and `isWeapon()` as needed

For `ThrowableAbility`, `execute()` should call `ThrowAction.throwDirect(combatant, projectile, scale, velocity)`.

### 3. Register the skill ID

In `SkillIds.java`:
```java
public static final SkillId MY_SKILL = SkillId.of("sword", "active.my_skill");
```

Then add it to the `ALL` list — **both steps are required.**

### 4. Register the skill implementation

In `SkillRegistry.java`, inside the `static {}` block:
```java
register(new MySkillImpl());
```

### 5. Wire to input (optional — UMBRAL and ACTIVE slots only)

If your skill goes in a slot that is already handled by `InputRegistrar`, no additional wiring is needed. `SkillSlotActionFactory.create()` resolves it at runtime from `PlayerSkillContainer`.

If your skill requires a hold-activated input path, ensure `requiresHold()` returns `true`. The factory will then only return a non-null action on the `holdVariant = true` code path in `InputRegistrar`.

---

## Interactions with Other Systems

| System | Interaction |
|---|---|
| `InputExecutionTree` | `SkillSlotActionFactory` produces `InputAction` instances consumed by the trie at skill-slot nodes |
| `CombatProfile` | Owns `PlayerSkillContainer`; retrieved via `player.getCombatProfile().getPlayerSkillContainer()` |
| `AbilitySlotManager` | Reads `AbilitySkill` methods (`useTypes`, `buildWorldItem`, etc.) to manage hotbar slot items |
| `PlayerDataStore` / `SqlitePlayerDataStore` | Persists and restores `PlayerSkillContainer` state (equipped + available skill IDs) |
| `AbilityHistoryRepository` | Persists and loads `PlayerAbilityHistory` entries |
| `SkillSelectionMenu` / `CharacterMenu` | Read `PlayerSkillContainer` to display and modify equipped skills |
| `UmbralBlade` / `UmbralStateMachine` | UmbralBlade skills trigger blade FSM transitions indirectly via `UmbralBladeAction` calls |

---

## Known Issues and Limitations

- **`AbilityType` is never consumed.** All five base classes declare it but no caller reads the value. It is safe to ignore for now but represents either dead code or unfinished infrastructure.
- **`ConsumableActive` is a stub.** It extends `ActiveSkill` with no added behavior and has no concrete implementations.
- **`SkillIds.BLEED_MASTERY` has no implementation.** The ID is declared and included in `ALL` but no class is registered in `SkillRegistry`. `SkillRegistry.get(SkillIds.BLEED_MASTERY)` returns `null`.
- **`SkillSlotActionFactory` hardcodes `VoidLungeSkill` specifics.** Cast duration and soulfire cost are set conditionally by `instanceof` check. New skills with non-default cast durations or soulfire costs must add another branch here until the factory is refactored.
- **`SkillSlotActionFactory.create()` silently returns `null` on hold/tap mismatch.** A misconfigured input tree registration (e.g., registering a tap node for a throwable skill) produces no error — the node just never fires.
- **`ShadowSlashSkill.name()` returns `null`.** The `execute()` body is a debug message. This is a placeholder; the skill is not yet implemented.
