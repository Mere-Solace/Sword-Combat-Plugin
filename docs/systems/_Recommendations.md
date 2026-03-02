# Architectural Recommendations

Rolling recommendations based on a full codebase analysis. Updated as systems evolve.

---

## What is Working Well

1. **Self-registering Config pattern** -- `Config.ConfigEntry` with `static {}` registration blocks is clean and extensible. Adding a new config value is a single declaration.

2. **Generic State Machine framework** -- `StateMachine<T>`, `State<T>`, and `Transition<T>` in `utility/statemachine/` are well-abstracted and reusable. The `isAssignableFrom` transition matching enables elegant wildcard patterns.

3. **InputExecutionTree builder pattern** -- The `InputNodeBuilder` makes combo registration declarative and readable. The trie structure efficiently handles overlapping prefixes.

4. **Bezier curve attack paths** -- Using parametric curves for attack trajectories is a sophisticated and visually appealing approach that enables complex sweep patterns.

5. **Soulfire transfer particle system** -- `SoulfireManager` creates compelling visual feedback with lerped particle trajectories.

6. **TimeArbiter centralization** -- Having a single point of control for all timed game logic makes the time-scale feature feasible and consistent.

---

## Concrete Improvement Recommendations

### High Priority

#### 1. Fix InputAction cooldown tracking — #167, #180

In `InputExecutionTree`, actions are stored as `Supplier<InputAction>`, creating new instances on every `resolveAction()` call. This means `timeLastExecuted` is always 0 on a fresh resolve, making per-action cooldowns unreliable.

**Fix**: Cache resolved `InputAction` instances in the tree nodes, or move cooldown tracking to a separate per-player cooldown map keyed by action identity.

#### 2. Enable player data persistence — #39

Uncomment `PlayerDataManager.shutdown()` in `Sword.onDisable()` or implement a periodic auto-save. Without this, all player progress is lost on restart. Consider adding a versioning field to `PlayerData` for future migration support.

#### 3. Eliminate hit() duplication in Dummy — #185

`Dummy.hit()` duplicates most of `SwordEntity.hit()` with minor changes (no knockback, reset shards on death). Extract the differences into overridable hooks:

- `protected boolean shouldApplyKnockback()` (default true, Dummy returns false)
- `protected void onShardsDepleted()` (default: kill, Dummy: reset)

#### 4. Replace HashMap with LinkedHashMap in StateMachine transitions — #186

`StateMachine.transitions` uses `HashMap`, which provides no iteration order guarantee. Since the first matching transition fires, non-deterministic ordering could cause subtle bugs. Use `LinkedHashMap` to preserve registration order.

---

### Medium Priority

#### 5. Refactor SwordEntityArbiter.initializeNPC() — #187

The giant switch statement listing every entity type is brittle. Replace with:

- A `Set<EntityType>` for hostile types
- A `Set<EntityType>` for ignored types
- Default to Passive for everything else
- Consider making these sets configurable

#### 6. Extract hold detection constants — #170

The 162ms threshold in `SwordPlayer.startHoldingRight()` and `startSneaking()` should be a config value. The interaction between `RIGHT` -> `RIGHT_TAP`/`RIGHT_HOLD` and `SHIFT` -> `SHIFT_TAP`/`SHIFT_HOLD` detection should be documented.

#### 7. Add validation to Config values — #155

`ConfigManager.loadEntry()` silently accepts any value. Add range validation (e.g., `DASH_BASE_POWER > 0`, `GLOBAL_TIME_SCALE` in [0, 2]). Log warnings for out-of-range values.

#### 8. Reduce object allocation in hot paths — #188

`BezierUtil.cubicBezier3D()` creates new Vector objects via `multiply()` on every call. `HitboxUtil.secant()` creates many Location clones per step. For high-frequency calls during combat, consider pre-allocated mutable vectors or object pooling.

#### 9. Implement Hostile AI — #189

`Hostile` has extensive stub methods. Consider implementing a behavior tree or utility AI system. The existing `Pathfinder` integration is a good foundation.

---

### Low Priority / Technical Debt

#### 10. Unify PlayerMenuManager overloads — #125

Two `openNewMenu()` methods (Class-based and instance-based) have nearly identical logic. Extract the common history management into a private method.

#### 11. Add defensive nullability — #125

Several places assume non-null returns (e.g., `SwordEntityArbiter.get()` can return null but callers often skip null checks). Add `@Nullable` annotations and null guards.

#### 12. Replace direct Bukkit scheduler usage — #151

`SwordEntity.removeStatusDisplay()` uses a raw `BukkitRunnable` task. `SwordEntityArbiter.registerAllExistingEntities()` uses `Bukkit.getScheduler().runTaskLater()`. These should use `SwordScheduler` for consistency with the scheduling architecture.

#### 13. Break up Config.java — #155

The file is very large. Consider splitting inner classes into separate files (e.g., `Config.Combat` -> `CombatConfig.java`) while keeping the registration mechanism.

#### 14. Add unit tests for math utilities — #190

`BezierUtil`, `VectorUtil`, `HitboxUtil` are pure logic with no Bukkit dependencies beyond `Vector` and `Location`. These are prime candidates for unit testing.

---

## Scalability Concerns

- **File-based persistence** -- JSON file storage will not scale beyond a few hundred players. Plan for SQLite or a database backend. → #164
- **Per-entity ticking** -- Every `SwordEntity` runs its own timer task. With many entities, this creates many scheduled futures. Consider a single tick loop that iterates all entities. → #151
- **Display entity proliferation** -- Status displays, target indicators, sweep displays, and umbral blade displays create many entities. Server performance may degrade with many concurrent combatants. → #188
- **Static registries** -- `SkillRegistry`, `SwordEntityArbiter`, `InteractiveItemArbiter`, and `Config` are all static. This prevents multi-instance or testing scenarios. Consider dependency injection or at least instance-based registries.

---

*Last updated: 2026-02-22*
