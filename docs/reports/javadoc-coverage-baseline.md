# Javadoc Coverage Baseline Report

**Generated:** 2025-10-30 20:21:13

## Summary

| Metric                   | Count | Percentage |
| ------------------------ | ----- | ---------- |
| **Total Classes**        | 53    | -          |
| **Documented Classes**   | 26    | 49.1%      |
| **Total Public Methods** | 292   | -          |
| **Documented Methods**   | 188   | 64.4%      |

## Classes Needing Documentation (Sorted by Priority)

| Class                          | Path                                                   | Class Doc | Methods | Documented | Coverage |
| ------------------------------ | ------------------------------------------------------ | --------- | ------- | ---------- | -------- |
| CommandManager.java            | btm\sword\commands\CommandManager.java                 | ✗         | 1       | 0          | 0%       |
| EntityListener.java            | btm\sword\listeners\EntityListener.java                | ✓         | 4       | 0          | 0%       |
| InputListener.java             | btm\sword\listeners\InputListener.java                 | ✓         | 11      | 0          | 0%       |
| PlayerListener.java            | btm\sword\listeners\PlayerListener.java                | ✓         | 9       | 0          | 0%       |
| GroundedAffliction.java        | btm\sword\system\combat\GroundedAffliction.java        | ✓         | 2       | 0          | 0%       |
| DisplayWrapper.java            | btm\sword\system\entity\display\DisplayWrapper.java    | ✗         | 1       | 0          | 0%       |
| Passive.java                   | btm\sword\system\entity\Passive.java                   | ✓         | 2       | 0          | 0%       |
| PlayerDataManager.java         | btm\sword\system\playerdata\PlayerDataManager.java     | ✗         | 6       | 0          | 0%       |
| SwordScheduler.java            | btm\sword\system\SwordScheduler.java                   | ✗         | 1       | 0          | 0%       |
| RuntimeTypeAdapterFactory.java | btm\sword\util\gson\RuntimeTypeAdapterFactory.java     | ✗         | 4       | 0          | 0%       |
| InputUtil.java                 | btm\sword\util\InputUtil.java                          | ✗         | 1       | 0          | 0%       |
| SoundUtil.java                 | btm\sword\util\SoundUtil.java                          | ✗         | 1       | 0          | 0%       |
| VectorUtil.java                | btm\sword\util\VectorUtil.java                         | ✗         | 7       | 0          | 0%       |
| MovementAction.java            | btm\sword\system\action\MovementAction.java            | ✓         | 9       | 2          | 22%      |
| AttackAction.java              | btm\sword\system\action\AttackAction.java              | ✓         | 6       | 2          | 33%      |
| TestAction.java                | btm\sword\system\action\TestAction.java                | ✓         | 9       | 3          | 33%      |
| GrabAction.java                | btm\sword\system\action\utility\GrabAction.java        | ✓         | 3       | 1          | 33%      |
| UtilityAction.java             | btm\sword\system\action\utility\UtilityAction.java     | ✓         | 8       | 4          | 50%      |
| ThrownItem.java                | btm\sword\system\action\utility\thrown\ThrownItem.java | ✗         | 22      | 13         | 59%      |
| EntityUtil.java                | btm\sword\util\EntityUtil.java                         | ✓         | 3       | 2          | 67%      |

_... and 33 more classes_

## Well-Documented Classes (100% Coverage)

| Class                       | Path                                                               | Methods Documented |
| --------------------------- | ------------------------------------------------------------------ | ------------------ |
| InteractiveItemArbiter.java | btm\sword\system\action\utility\thrown\InteractiveItemArbiter.java | 4                  |
| AspectValue.java            | btm\sword\system\entity\aspect\value\AspectValue.java              | 2                  |
| ResourceValue.java          | btm\sword\system\entity\aspect\value\ResourceValue.java            | 4                  |
| EntityAspects.java          | btm\sword\system\entity\EntityAspects.java                         | 32                 |
| SwordEntityArbiter.java     | btm\sword\system\entity\SwordEntityArbiter.java                    | 5                  |
| InputAction.java            | btm\sword\system\input\InputAction.java                            | 4                  |
| ItemStackBuilder.java       | btm\sword\system\item\ItemStackBuilder.java                        | 10                 |
| CombatProfile.java          | btm\sword\system\playerdata\CombatProfile.java                     | 6                  |
| PlayerData.java             | btm\sword\system\playerdata\PlayerData.java                        | 3                  |
| BezierUtil.java             | btm\sword\util\BezierUtil.java                                     | 3                  |
| DisplayUtil.java            | btm\sword\util\DisplayUtil.java                                    | 5                  |
| HitboxUtil.java             | btm\sword\util\HitboxUtil.java                                     | 7                  |
| ParticleWrapper.java        | btm\sword\util\ParticleWrapper.java                                | 1                  |

## Recommendations

1. **Start with high-priority classes**: Focus on classes with most methods and 0% coverage
2. **Target classes without class-level Javadoc**: Add class documentation first
3. **Document public APIs**: All public methods should have Javadoc
4. **Track progress**: Re-run this task periodically to measure improvement

## How to Improve

Run: `./gradlew javadocCoverage` to regenerate this report

See [docs/standards/documentation-standards.md](../standards/documentation-standards.md) for Javadoc
guidelines.
