TODO AUDIT — 2026-04-07
========================

TODOs WITH EXISTING ISSUE REFS
-------------------------------

File                             | Line | Issue | Description
---------------------------------|------|-------|---------------------------------------------------
RoguelikeRun.java                | 155  | #285  | Distribute MiscItems (Soulfire Flask, Skill Scroll) once item system exists
PlayerListener.java              | 399  | #233  | Find a better way to handle display entity orphaning on game mode change
Attack.java                      | 382  | #128  | Make particle effects more dynamic (low prio)
UmbralBlade.java                 | 258  | #240  | Method for calculating correct blade orientation for swing plane alignment
UmbralBlade.java                 | 535  | #241  | Add rejection logic for non-thrower grabs
UmbralStateMachine.java          | 341  | #241  | Test the Lunge transition
GrabAction.java                  | 47   | #147  | Clean up nest
DashAttackAction.java            | 57   | #137  | Fix and move into AttackingQuick state
SwordEntity.java                 | 221  | #160  | Remake AI toggle after pin
CombatProfile.java               | 98   | #166  | Allow dynamic loading of CombatProfile info
CombatProfile.java               | 130  | #166  | Related to dynamic CombatProfile loading
InteractiveItemArbiter.java      | 174  | #81   | Cleanup considerations for thrown items
Combatant.java                   | 135  | #122  | On-death UmbralBlade logic
VisualProjectile.java            | 215  | #119  | Make particle type dynamic
VisualProjectile.java            | 216  | #119  | Make particle period dynamic
SwordPlayer.java                 | 1415 | #279  | Apply stat more often and give more meaning


TODOs NEEDING ISSUES
--------------------

#  | File                        | Line     | Description                                                          | Type
---|-----------------------------|----------|----------------------------------------------------------------------|----------
1  | MovementListener.java       | 57       | Fix FieldAccessException — Field index 0 OOB for some packet variants | fix
2  | SwordPlayer.java            | 715      | Find out why soul link sometimes stays in inventory after removal    | fix
3  | GrabAction.java             | 55       | Find out why input tree doesn't reset on its own                     | fix
4  | TestingMenu.java            | 223      | openSmithingTable is deprecated — find replacement                   | fix
5  | MovementAction.java         | 61       | Tree reset needed in movement — root issue unknown                   | fix
6  | SwordEntity.java            | 889      | Convert lodged-block item into a StuckItem                           | feat
7  | UmbralBladeAction.java      | 97       | Implement forward rush + trigger mechanism                           | feat
8  | WieldState.java             | 29       | Add gameplay functionality for the Wield state                       | feat
9  | CharacterMenu.java          | 148-149  | Class change: add confirmation screen + consumption requirement       | feat
10 | SimulatedDisplay.java       | 36       | Config-wrap magic numbers (display scale, etc.)                      | chore
   | VisualProjectile.java       | 487,491  | Config-wrap axe scale and display magic numbers                      | chore
   | Dash.java                   | 299      | Config-wrap dash distance threshold                                  | chore
11 | ItemDisplayAttack.java      | 111      | Pass HitValues instance instead of inline construction               | refactor
12 | Attack.java                 | 114      | Caller-defined consumer for time-step + current vector               | refactor
13 | UmbralBladeAction.java      | 105      | Store ItemStack reference instead of constructing inline             | refactor
14 | SwordPlayer.java            | 1334     | Cache ItemStack + send equipment change delta instead of full refresh | refactor
15 | CharacterMenu.java          | 114      | Remove dev shortcut for replenishing ability slots                   | chore
16 | SkillRegistry.java          | 33       | Remove test skill stubs once real items exist                        | chore
   | SwordPlayer.java            | 317      | Remove test skill stubs once real items exist                        | chore
17 | HudOverrideManager.java     | 24       | Audit null-check patterns on attribute access                        | chore
18 | MenuSlotGrid.java           | 115      | Evaluate building slot grid on world load instead of on-demand       | chore


JAVADOC VIOLATIONS (checkstyle)
--------------------------------
Total violations: 290 across 93 files.
Run: ./gradlew checkstyleMain --rerun-tasks 2>&1 | grep MissingJavadoc

Files affected (93 total):
  btm/sword/Sword.java
  btm/sword/listeners/CustomInteractionListener.java
  btm/sword/listeners/InputListener.java
  btm/sword/listeners/PlayerListener.java
  btm/sword/listeners/SystemListener.java
  btm/sword/system/action/attack/AttackAction.java
  btm/sword/system/action/attack/PunchAction.java
  btm/sword/system/action/movement/Dash.java
  btm/sword/system/action/skill/type/impl/umbral/ShadowSlashSkill.java
  btm/sword/system/action/skill/type/impl/umbral/VoidLungeSkill.java
  btm/sword/system/action/SwordAction.java
  btm/sword/system/action/throwing/impale/Impalement.java
  btm/sword/system/action/throwing/InteractiveItem.java
  btm/sword/system/action/throwing/InteractiveItemArbiter.java
  btm/sword/system/action/throwing/ItemThrowStyle.java
  btm/sword/system/action/throwing/types/DroppedItem.java
  btm/sword/system/action/throwing/types/VisualProjectile.java
  btm/sword/system/action/UmbralBladeAction.java
  btm/sword/system/action/utility/UtilityAction.java
  btm/sword/system/attack/Attack.java
  btm/sword/system/attack/ItemDisplayAttack.java
  btm/sword/system/attack/style/WeaponAttackStyle.java
  btm/sword/system/attack/SweepAttack.java
  btm/sword/system/attack/UmbralBladeAttack.java
  btm/sword/system/combat/Affliction.java
  btm/sword/system/combat/GroundedAffliction.java
  btm/sword/system/control/EntityController.java
  btm/sword/system/control/EntityLifeCycleManager.java
  btm/sword/system/control/PredicateRunnablePair.java
  btm/sword/system/control/SwordScheduler.java
  btm/sword/system/entity/ai/MobGoalArbiter.java
  btm/sword/system/entity/aspect/Resource.java
  btm/sword/system/entity/aspect/value/AspectValue.java
  btm/sword/system/entity/aspect/value/ResourceValue.java
  btm/sword/system/entity/base/SoulfireManager.java
  btm/sword/system/entity/impl/Dummy.java
  btm/sword/system/entity/impl/Hostile.java
  btm/sword/system/entity/impl/Passive.java
  btm/sword/system/entity/impl/SwordPlayer.java
  btm/sword/system/entity/umbral/input/BladeRequest.java
  btm/sword/system/entity/umbral/input/InputBuffer.java
  btm/sword/system/entity/umbral/statemachine/state/FinisherState.java
  btm/sword/system/entity/umbral/statemachine/state/GrabImpaleState.java
  btm/sword/system/entity/umbral/statemachine/state/InactiveState.java
  btm/sword/system/entity/umbral/statemachine/state/LungingState.java
  btm/sword/system/entity/umbral/statemachine/state/PreviousState.java
  btm/sword/system/entity/umbral/statemachine/state/RecoverState.java
  btm/sword/system/entity/umbral/statemachine/state/SheathedState.java
  btm/sword/system/entity/umbral/statemachine/state/StandbyState.java
  btm/sword/system/entity/umbral/statemachine/state/WieldState.java
  btm/sword/system/entity/umbral/statemachine/UmbralState.java
  btm/sword/system/entity/umbral/statemachine/UmbralStateFacade.java
  btm/sword/system/entity/umbral/statemachine/UmbralStateMachine.java
  btm/sword/system/entity/umbral/UmbralBlade.java
  btm/sword/system/entity/umbral/UmbralSkill.java
  btm/sword/system/hud/HudOverrideManager.java
  btm/sword/system/input/InputAction.java
  btm/sword/system/input/InputExecutionTree.java
  btm/sword/system/input/InputType.java
  btm/sword/system/interaction/CustomInteractionContext.java
  btm/sword/system/interaction/CustomInteractionManager.java
  btm/sword/system/inventory/InventoryMenuManager.java
  btm/sword/system/inventory/item/ForwardItem.java
  btm/sword/system/inventory/item/PreviousItem.java
  btm/sword/system/inventory/menu/AbilityHistoryMenu.java
  btm/sword/system/inventory/menu/ArtifactPouchMenu.java
  btm/sword/system/inventory/menu/CharacterMenu.java
  btm/sword/system/inventory/menu/CurrencyMenu.java
  btm/sword/system/inventory/menu/dev/TestingMenu.java
  btm/sword/system/inventory/menu/DevStatEditorMenu.java
  btm/sword/system/inventory/menu/MainMenu.java
  btm/sword/system/inventory/menu/MaterialPouchMenu.java
  btm/sword/system/inventory/menu/MovesetMenu.java
  btm/sword/system/inventory/menu/SkillSelectionMenu.java
  btm/sword/system/inventory/PlayerMenuManager.java
  btm/sword/system/item/ItemStackBuilder.java
  btm/sword/system/item/ItemUsageManager.java
  btm/sword/system/item/KeyRegistry.java
  btm/sword/system/item/SwordItemType.java
  btm/sword/system/playerdata/SwordClassType.java
  btm/sword/utility/data/RuntimeTypeAdapterFactory.java
  btm/sword/utility/display/ColorUtil.java
  btm/sword/utility/display/DisplayUtil.java
  btm/sword/utility/display/DrawUtil.java
  btm/sword/utility/entity/HitboxUtil.java
  btm/sword/utility/entity/InputUtil.java
  btm/sword/utility/entity/PotionEffectWrapper.java
  btm/sword/utility/math/Basis.java
  btm/sword/utility/math/VectorUtil.java
  btm/sword/utility/Misc.java
  btm/sword/utility/misc/ConsumerToConsumePair.java
  btm/sword/utility/Prefab.java
