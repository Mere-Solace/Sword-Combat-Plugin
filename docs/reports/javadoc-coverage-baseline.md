# Javadoc Coverage Baseline Report

**Generated:** 2025-10-30 19:20:52

## Summary

| Metric | Count | Percentage |
|--------|-------|------------|
| **Total Classes** | 53 | - |
| **Documented Classes** | 20 | 37.7% |
| **Total Public Methods** | 232 | - |
| **Documented Methods** | 83 | 35.8% |

## Classes Needing Documentation (Sorted by Priority)

| Class | Path | Class Doc | Methods | Documented | Coverage |
|-------|------|-----------|---------|------------|----------|
| EntityListener.java | btm\sword\listeners\EntityListener.java | ✓ | 4 | 0 | 0% |
| InputListener.java | btm\sword\listeners\InputListener.java | ✓ | 11 | 0 | 0% |
| PlayerListener.java | btm\sword\listeners\PlayerListener.java | ✓ | 9 | 0 | 0% |
| AttackAction.java | btm\sword\system\action\AttackAction.java | ✓ | 2 | 0 | 0% |
| MovementAction.java | btm\sword\system\action\MovementAction.java | ✓ | 7 | 0 | 0% |
| TestAction.java | btm\sword\system\action\TestAction.java | ✗ | 6 | 0 | 0% |
| GrabAction.java | btm\sword\system\action\utility\GrabAction.java | ✓ | 2 | 0 | 0% |
| ThrowAction.java | btm\sword\system\action\utility\thrown\ThrowAction.java | ✓ | 1 | 0 | 0% |
| UtilityAction.java | btm\sword\system\action\utility\UtilityAction.java | ✓ | 4 | 0 | 0% |
| GroundedAffliction.java | btm\sword\system\combat\GroundedAffliction.java | ✓ | 2 | 0 | 0% |
| DisplayWrapper.java | btm\sword\system\entity\display\DisplayWrapper.java | ✗ | 1 | 0 | 0% |
| EntityAspects.java | btm\sword\system\entity\EntityAspects.java | ✗ | 32 | 0 | 0% |
| Hostile.java | btm\sword\system\entity\Hostile.java | ✗ | 16 | 0 | 0% |
| Passive.java | btm\sword\system\entity\Passive.java | ✗ | 2 | 0 | 0% |
| Item.java | btm\sword\system\item\Item.java | ✗ | 2 | 0 | 0% |
| ItemStackBuilder.java | btm\sword\system\item\ItemStackBuilder.java | ✗ | 10 | 0 | 0% |
| CombatProfile.java | btm\sword\system\playerdata\CombatProfile.java | ✗ | 6 | 0 | 0% |
| PlayerData.java | btm\sword\system\playerdata\PlayerData.java | ✗ | 3 | 0 | 0% |
| EntityUtil.java | btm\sword\util\EntityUtil.java | ✓ | 1 | 0 | 0% |
| RuntimeTypeAdapterFactory.java | btm\sword\util\gson\RuntimeTypeAdapterFactory.java | ✗ | 4 | 0 | 0% |

*... and 33 more classes*

## Well-Documented Classes (100% Coverage)

| Class | Path | Methods Documented |
|-------|------|--------------------|
| InteractiveItemArbiter.java | btm\sword\system\action\utility\thrown\InteractiveItemArbiter.java | 0 |
| AspectValue.java | btm\sword\system\entity\aspect\value\AspectValue.java | 2 |
| ResourceValue.java | btm\sword\system\entity\aspect\value\ResourceValue.java | 4 |
| SwordEntityArbiter.java | btm\sword\system\entity\SwordEntityArbiter.java | 0 |
| InputAction.java | btm\sword\system\input\InputAction.java | 4 |
| BezierUtil.java | btm\sword\util\BezierUtil.java | 0 |
| DisplayUtil.java | btm\sword\util\DisplayUtil.java | 0 |
| HitboxUtil.java | btm\sword\util\HitboxUtil.java | 0 |
| ParticleWrapper.java | btm\sword\util\ParticleWrapper.java | 1 |

## Recommendations

1. **Start with high-priority classes**: Focus on classes with most methods and 0% coverage
2. **Target classes without class-level Javadoc**: Add class documentation first
3. **Document public APIs**: All public methods should have Javadoc
4. **Track progress**: Re-run this task periodically to measure improvement

## How to Improve

Run: `./gradlew javadocCoverage` to regenerate this report

See [docs/standards/documentation-standards.md](../standards/documentation-standards.md) for Javadoc guidelines.
