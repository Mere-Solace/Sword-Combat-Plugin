# Entity Implementation Package

Package: `btm.sword.system.entity.impl`

## Overview

This package contains the concrete entity classes that sit atop the abstract `SwordEntity` base (in `entity.base`). The hierarchy has four layers:

```
SwordEntity          (entity.base)   — wraps LivingEntity; aspects, afflictions, status display
  └── Combatant      (entity.impl)   — adds combat: UmbralBlade, grab, dash, attack, throw, cast
        ├── SwordPlayer              — player-specific: input tree, inventory, HUD, scene modes
        ├── Hostile                  — mob combatant: AI state machine, simplified blade lifecycle
        ├── Passive                  — non-combat entity; no blade or input
        └── Dummy                   — training dummy; extends Combatant, owned by SwordPlayer
RigHostile           (entity.impl)   — non-combat scripted mob, extends SwordEntity directly
```

---

## Class Responsibilities

| Class | What it owns |
|---|---|
| `SwordEntity` | UUID, `LivingEntity` ref, `EntityAspects`, `Affliction` map, `Impalement` set, status `TextDisplay`, tick loop, hit invulnerability, impale slow, bounding box queries, basis vectors |
| `Combatant` | `UmbralBlade` lifecycle, grab/throw mechanics, `abilityCastTask`, air dash budget, `ActiveAttack` tracking, stat scaling helpers (`calcValueAdditive`, `calcCooldown`), `canPerform*` combat checks |
| `SwordPlayer` | `InputExecutionTree`, `InputGestureTracker`, inventory upkeep, scene overlay, creative dev mode, HUD display helpers, `ActivationContext`, `PlayerStorage`, skill container, `AbilitySlotManager`, `PlayerMenuManager` |
| `Hostile` | AI `StateMachine`, vanilla AI suppression, goal management via `MobGoalArbiter` |
| `Dummy` | Thin Combatant subclass; tracks owner, responds to `onGrabbed`/`onReleased` |
| `RigHostile` | Scripted non-combat NPC; does not extend `Combatant` |

---

## Combatant vs SwordPlayer — Boundary Rule

The intended rule is:

> `Combatant` owns everything that **any combat-capable entity** might need, regardless of whether it has a player behind it. `SwordPlayer` owns everything that **requires a `Player` object** or that is specific to the human-interaction layer.

### What currently lives in Combatant (correctly)

- `UmbralBlade` lifecycle (`setupUmbralBlade`, `handleUmbralBladeTick`, `requestUmbralBladeState`)
- Grab/throw state fields and callbacks (`onGrab`, `onGrabThrow`, `onGrabHit`, `onGrabLetGo`)
- `abilityCastTask` management (`setCastTask`, `canPerformAction`)
- Air dash budget (`canAirDash`, `resetAirDashesPerformed`, `increaseAirDashesPerformed`)
- Stat scaling (`calcValueAdditive`, `calcValueReductive`, `calcCooldown`, `applyAttackCooldown`)
- `canPerformHealAction`, `canPerformWieldAction`, `canPerformUmbralAction`, `canPerformUmbralLinkAttack`, `canPerformShadowBlink` — these encode blade-state + action-availability logic; they do not need a `Player`
- `holdingUmbralBlade`, `holdingSoulLink`, `holdingUmbralItemInMainHand`, `holdingNothing`, `isUmbralItem` — item-in-hand queries usable by Hostile AI too
- `launchAttackDef`, `onAttackEnd`, `currentAttack`, `isAttacking` — attack pipeline

### What lives in SwordPlayer and is correctly player-specific

- `InputExecutionTree` and `InputGestureTracker` — player-only input routing
- `ActivationContext` and `activationContext` — tracks player-specific FSM context (NORMAL, THROWING, INCAPACITATED, etc.)
- `inventoryUpkeep`, `SlotAnchoredItem` management — requires `Player.getInventory()`
- `AbilitySlotManager`, `PlayerMenuManager`, `PlayerStorage` — player data layer
- Scene overlay and creative dev mode — visibility flags, inventory save/restore, `GameMode` changes
- HUD display helpers (`displayCooldown`, `displayDisablingEffect`, `displayLackOfSoulfire`, `displaySoulfireConsumed`, `displayInputSequence`, `updateVisualStats`) — all require `Player.showTitle()` or `Player.setExp()`
- `parryWindowEnd`, `blockDrainTask`, `startBlockDrain`, `cancelBlockDrainTask` — shield/parry is player-only (no mob blocking)
- `expBarTick`, `targetEntityIndicatorTick` — visual-only, player-facing

### Ambiguous / tension points

**`normalActState()`, `throwingState()`, `soulLinkState()`, `umbralBladeState()`, `nonUmbralState()`, `canBeginThrow()` etc.**

These are composite predicates that combine `activationContext` (player-only field) with `holdingSoulLink()` / `holdingUmbralBlade()` (Combatant methods). They live in `SwordPlayer` because they depend on `activationContext`. This is correct — they should not move to `Combatant`.

However, these methods are used almost exclusively as `ActionContextPair` context predicates in `InputRegistrar`, which only receives `SwordPlayer` references anyway. Their placement in `SwordPlayer` is appropriate.

**`Combatant.onGrab()` references `SwordPlayer` directly** via an `instanceof` check to set `ActivationContext.INCAPACITATED`. This is a narrow but real coupling: `Combatant` reaches into `SwordPlayer`'s `activationContext` field. The coupling is intentional (grabbed players should be incapacitated) but worth noting. If `ActivationContext` ever needs to be on `Combatant`, this can be straightforwardly moved. For now, the `instanceof` is acceptable since the dependency is narrow and explicit.

**`InputAction.handlePerformAttempt()` casts executor to `SwordPlayer`** to call `displayCooldown()`, `displayDisablingEffect()`, and `resetTree()`. This is a coupling violation — `InputAction` is typed `Consumer<Combatant>` but internally depends on `SwordPlayer` API. See the Input section below for the recommended fix.

---

## Known Coupling Issues

### 1. `InputAction` typed `Combatant` but assumes `SwordPlayer`

`InputAction.action` is a `Consumer<Combatant>`. At every call site in `InputRegistrar`, the consumer is written against `SwordPlayer` methods accessed via `instanceof` casts or direct `(SwordPlayer) c` casts. `InputAction.handlePerformAttempt()` also hard-casts `executor` to `SwordPlayer` for HUD display calls.

The type claim (`Combatant`) is inaccurate. The true execution domain is always `SwordPlayer` — `InputExecutionTree` is owned by `SwordPlayer`, `InputRegistrar.initializeInputTree()` receives a `SwordPlayer`, and `InputActionExecutor.execute()` is called from `SwordPlayer.act()`.

**Recommended fix**: change `Consumer<Combatant>` → `Consumer<SwordPlayer>` throughout `InputAction` and its Builder. All `canCast`, `cooldown`, `castDuration`, `requiredSoulfire` functions are currently `Function<Combatant, T>` — these should similarly become `Function<SwordPlayer, T>`. This eliminates all the unsafe casts in `InputAction.handlePerformAttempt()` and in `InputRegistrar`. See the Input README for the full impact analysis.

### 2. `Combatant.onGrab()` and `onGrabLetGo()` reach into `SwordPlayer.activationContext`

Narrow coupling via `instanceof SwordPlayer`. Acceptable for now; would be resolved if `activationContext` were lifted to `Combatant`.

### 3. `InputRegistrar` canCast predicates mix Combatant and SwordPlayer

Several canCast predicates in `InputRegistrar` write `c instanceof SwordPlayer sp && sp.somePlayerMethod()` because the lambda parameter is typed `Combatant` but the actual value is always a `SwordPlayer`. This is boilerplate that would disappear with the fix in point 1.

---

## Extension Points

- New playable entity type: extend `Combatant`, override `setupUmbralBlade()` if blade behavior differs, implement `onDeath()`.
- New non-playable combat entity: extend `Combatant`. Override `setupUmbralBlade()` → `deactivateUmbralBlade()` (as `Hostile` does) to suppress the visual.
- New `canPerform*` predicate: add to `Combatant` if the check requires only blade state + action availability; add to `SwordPlayer` if it requires `ActivationContext` or inventory state.
