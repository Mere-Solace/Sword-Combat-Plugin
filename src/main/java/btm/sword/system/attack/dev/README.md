# system/attack/dev

This package implements the in-game developer tooling for authoring and testing volume-based attacks.
It is **entirely dev-only** — no class in this package is involved in live gameplay. The entry points
are the test wand (bound in `InputRegistrar`) and the `AttackBrowserMenu` / `AttackEditorMenu` UI.

## Class inventory

| Class | Role |
|-------|------|
| `AttackDevSession` | Per-player session state. Owns all mutable editing data: mode, keyframes, duration, hit value, orientation flags, multi-selection set, and the wand-loaded `AttackDef`. Also the static `SESSIONS` map and its lifecycle methods. |
| `DevMode` | Enum: `IDLE`, `RECORDING`, `EDITING`, `VIEWING`. Tracks what a session is currently doing. |
| `AnimationMode` | Manages the lifecycle of the in-world hotbar-tool editing mode. `enter()` snapshots the creative-dev inventory and populates the animation hotbar; `exit()` restores inventory and opens `AttackBrowserMenu`. |
| `AnimationModeInputHandler` | Routes `InputType` events to the correct hotbar-tool slot action while `AnimationMode` is active on a `DevSwordPlayer`. Handles keyframe navigation, position nudging, extent nudging, shape cycling, preview playback, and editor open. |
| `VolumeEditorMode` | Renders OBB/sphere wireframes for all active sessions via a 100 ms main-thread repeating task. Also renders a live playback hitbox (cyan) for wand test fires. Manages per-session `TextDisplay` label entities. |
| `WandActions` | Static helpers bound to wand input combos in `InputRegistrar`. `exitSession` stops the current session; `openEditor` transitions to `EDITING` and opens `AttackEditorMenu`. |
| `SweepRecordingAction` | Records a sweep path by sampling the player's look direction at 10 Hz while blocking. Converts world-space samples to local-space `VolumeKeyframe` list and saves to `attacks/<name>.yml`. Also handles `holdingRight` speed boost during recording. |
| `SaveConfirmDialog` | Non-escapable InvUI dialog shown when the player presses the BARRIER exit button in AnimationMode. Offers "Save & Exit" (writes YAML, registers in `AttackRegistry`) and "Discard & Exit" (reloads from disk). |
| `RecordedSample` | Record: world-space tip position + absolute timestamp. Accumulated during a `RECORDING` session. |

## Session lifecycle

```
Player equips test wand
        │
        ├─ SHIFT+LEFT → SweepRecordingAction.toggleRecording()
        │       IDLE → RECORDING: launchSamplingLoop (100 ms, blocking=sample)
        │       RECORDING → IDLE: saveDraft() → AttackDef → registry + YAML → startEditing()
        │
        ├─ SHIFT+SWAP → WandActions.openEditor()
        │       EDITING: open AttackEditorMenu
        │       has loadedAttackDef: startEditingFromDef() → EDITING → AttackEditorMenu
        │       otherwise: AttackBrowserMenu
        │
        └─ DROP+DROP → WandActions.exitSession()
                → stopCurrentSession() → IDLE → AttackBrowserMenu

EDITING session:
  AttackBrowserMenu → select attack → startEditing()
                                             │
                                     startForSession() → VolumeEditorMode loop (100 ms)
                                             │
                               AttackEditorMenu (InvUI menus for:
                                  keyframe add/remove/reorder,
                                  t-value, jump flag, effect editor,
                                  duration, orientation flags,
                                  sweep generator, load-into-wand)
                                             │
                               AnimationMode.enter() (slot 35 in menu)
                                   ├─ Saves creative-dev inventory
                                   ├─ Populates hotbar (slots 0–8) + BARRIER (slot 35)
                                   └─ AnimationModeInputHandler.handle() per input
                                          (nudge XYZ, extents, cycle shape, play/pause)
                                             │
                               AnimationMode.exit() via SaveConfirmDialog
                                   ├─ Save → buildCurrentAttack() → AttackDefSerializer.save() → AttackRegistry.register()
                                   └─ Discard → reload from YAML → session.getEditKeyframes() restored
```

## Extension points

- **New hotbar tool in AnimationMode**: Add a slot in `AnimationMode.populateAnimationHotbar()`, add
  the matching case in `AnimationModeInputHandler.handle()`.
- **New session state**: Add a value to `DevMode`, add `start*`/`stop*` methods to `AttackDevSession`,
  and add a `startLoop` variant in `VolumeEditorMode` if visualization is needed.
- **Save to a directory other than `attacks/`**: Change the `File` construction in `SaveConfirmDialog`
  and `SweepRecordingAction.saveDraft()` — both hardcode `new File(dataFolder, "attacks")`.

## Interactions

| Dependency | Direction | How |
|------------|-----------|-----|
| `def/` | outward | `AttackDef` creation, `AttackRegistry.register`, `AttackDefSerializer.save/load` |
| `simulation/` | outward | `VolumeKeyframe`, `VolumeShape`, `KeyframedTrajectory`, `VolumeEditorMode.startPlaybackVisualization` uses `VolumeTrajectory.sample` |
| `Combatant.launchAttackDef` | outward | `AttackAction.fireWandDef` calls it for wand test fires |
| `InputRegistrar` | inward | Wand input combos call `WandActions` and `SweepRecordingAction` |
| `PlayerListener` | inward | Locks recording player position on `PlayerMoveEvent` |
| `DevSwordPlayer` | inward | `AnimationMode.enter/exit` mutate the player wrapper |
| InvUI menus (`dev/menu/`) | inward | `AttackBrowserMenu`, `AttackEditorMenu`, `SweepGeneratorMenu`, `KeyframeEffectsMenu` all access `AttackDevSession` directly |

## Known issues

**`VolumeEditorMode` logs verbosely every 4 seconds (every 40 ticks × 100 ms).**
`Sword.print` calls in the render loop are unconditional — they fire in any prod-accessible
dev session. These should be gated behind `Debug.attackVolume()` or removed.
(`VolumeEditorMode:133, 179, 197, 237–240, 265–268, 275`)

**`SweepRecordingAction.toggleRecording` only starts from `IDLE`.**
If a session is in `EDITING` or `VIEWING` when `SHIFT+LEFT` fires, nothing happens — no error
message is shown. A guard or explicit check would improve UX. (`SweepRecordingAction:108`)

**`SweepRecordingAction.saveDraft` hardcodes the attack name `"sweep_draft"`.**
Every recording session produces `sweep_draft`, `sweep_draft_1`, etc. regardless of context.
There is no way to name the attack before starting the recording. (`SweepRecordingAction:109`)

**`AnimationModeInputHandler` applies nudge to all selected keyframes only for `handleNudgePos` and
`handleNudgeExtents`, but the multi-selection set (`selectedKeyframeIndices`) is only wired through
`VolumeEditorMode.updateLabels` for display — the actual nudge methods read only `currentKeyframeIndex`
and ignore `selectedKeyframeIndices`.**
(`AnimationModeInputHandler:101–113, 121–135`)

**`SaveConfirmDialog.discardAndExit` mutates `session.getEditKeyframes()` directly.**
It calls `session.getEditKeyframes().clear()` and `addAll(...)` on the list returned by the getter.
`AttackDevSession.editKeyframes` is mutable, so this works, but it bypasses `setEditKeyframes()`
which also resets `selectedKeyframeIndices` and `currentKeyframeIndex`. After a discard, the
selection state may be stale. (`SaveConfirmDialog:149–150`)
