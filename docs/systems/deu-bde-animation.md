# DEU + BDE Animation Guide

This document covers how to author, store, and play display entity animations using
**BDEngine (BDE)** as the creation tool and **DisplayEntityUtils (DEU)** as the runtime
engine. It is written specifically for use inside Sword.

---

## Overview

| Tool | Role |
|---|---|
| **BDEngine** (`bdengine.app`) | Browser-based model + animation editor. Exports a Minecraft datapack `.zip`. |
| **DisplayEntityUtils (DEU)** | Paper plugin runtime. Imports BDE exports, stores groups/animations, and provides a Java API for playback. |

The pipeline is always: **author in BDE → export datapack → convert in DEU → consume via DEU Java API**.

---

## Part 1: Creating a Model and Animation in BDEngine

### 1.1 Open the Editor

Go to `bdengine.app` in any WebGL-capable browser. No account needed.

### 1.2 Build the Model

Use the **Editor** module to place and arrange block display entities. Each entity is a
"part" with its own block type, position, rotation, and scale. Name each part
descriptively — these names become the **part tags** that DEU uses to filter which parts
an animation targets.

Key editor concepts:
- Parts are positioned relative to the model's **origin point** (the spawn anchor).
- Use the **Structure** module to group parts into logical layers (e.g. `body`, `left_arm`).
- The **Asset Library** has preset shapes (spheres, cylinders, etc.) to accelerate model building.

### 1.3 Build the Animation

Switch to the **Animator** module. The workflow mirrors any keyframe timeline editor:

1. Move the playhead to a time position.
2. Pose the model (drag parts in the editor).
3. Insert a keyframe — BDE captures the current transformation of every modified part.
4. Advance the playhead, re-pose, insert another keyframe.
5. BDE interpolates between keyframes automatically.

Each animation has a **name** (e.g. `idle`, `walk`, `attack`). You can have multiple
named animations in a single BDE project. Use the **Sound** module to attach sounds to
specific keyframe events.

### 1.4 Export

Click **Export to Minecraft** → download the `.zip` datapack. This is the file you give to DEU.

---

## Part 2: Importing Into DEU

### 2.1 Drop the File on the Server

Place the exported `.zip` inside the DEU plugin folder:
```
plugins/DisplayEntityUtils/bdenginedatapacks/<your-file-name>.zip
```

### 2.2 Run the Conversion Command

Stand in-world where you want the model temporarily spawned during conversion. Then run:

```
/deu bdengine convertdp <zip-name> <group-tag> <anim-tag-prefix>
```

- `<zip-name>` — filename without `.zip` (e.g. `main_menu_sword`)
- `<group-tag>` — the identifier your code will use to spawn the model (e.g. `menu_sword`)
- `<anim-tag-prefix>` — prefix prepended to each animation name from BDE (e.g. `menu_sword_`)

**Example:**
```
/deu bdengine convertdp main_menu_sword menu_sword menu_sword_
```
If BDE had animations named `idle` and `slash`, DEU will produce:
- Group: `menu_sword` (saved as `menu_sword.deg`)
- Animations: `menu_sword_idle`, `menu_sword_slash` (saved as `.deanim` files)

For **legacy** BDE exports (projects created before December 8, 2024, single-animation only):
```
/deu bdengine convertdpleg <zip-name> <group-tag> <anim-tag>
```

### 2.3 Verify the Result

```
/deu listgroups local
/deu listanims local
```

The converted assets are now in DEU's local storage — no further file management needed.

---

## Part 3: DEU In-Game Command Reference

All commands use `/deu`.

### Group Commands (selection and management)

| Command | What it does |
|---|---|
| `/deu group spawn <tag> local` | Spawn a saved group at your location |
| `/deu group spawn <tag> local -packet` | Spawn as packet-based (client-side only) |
| `/deu group selectnearest <radius>` | Select the nearest spawned group |
| `/deu group deselect` | Clear group selection |
| `/deu group info` | Show tag, part count, and active animations |
| `/deu group settag <tag>` | Rename the selected group |
| `/deu group save local` | Save the selected group to local storage |
| `/deu group savejson` | Save as DEU JSON (slower but human-readable) |
| `/deu group clone` | Clone the selected group in place |
| `/deu group clonehere` | Clone and move clone to your position |
| `/deu group despawn` | Remove selected group from the world |
| `/deu group delete <tag> local` | Delete group from storage |
| `/deu group topacket` | Convert selected group to packet-based entities |
| `/deu group setspawnanim <anim-tag> local <linear\|loop>` | Set animation that auto-plays on spawn |
| `/deu group yaw <yaw>` | Rotate group on Y axis |
| `/deu group scale <multiplier> <ticks>` | Scale group over N ticks |
| `/deu group move <dir> <dist> [ticks]` | Move group; optional smooth duration |
| `/deu group movehere` | Teleport group to your position |
| `/deu listgroups local` | List all saved group tags |

### Animation Commands (authoring workflow)

| Command | What it does |
|---|---|
| `/deu anim new` | Create a blank animation (for manual authoring without BDE) |
| `/deu anim settag <tag>` | Name the current animation |
| `/deu anim addframe <delay> <duration>` | Capture current group state as next frame |
| `/deu anim addframeafter <id> <delay> <duration>` | Insert frame after specific frame |
| `/deu anim overwriteframe <id>` | Replace frame data with current group state |
| `/deu anim editframe <ids> <delay> <duration>` | Change timing of existing frame(s) |
| `/deu anim editallframes <delay> <duration>` | Batch-update timing of all frames |
| `/deu anim removeframe <id>` | Delete a frame |
| `/deu anim showframe <id>` | Apply frame to group (destructive preview) |
| `/deu anim previewframe <id>` | Apply frame via packets (non-destructive) |
| `/deu anim play` | Play animation once on selected group |
| `/deu anim play -loop` | Play animation looping |
| `/deu anim play -packet` | Play via packets (no server entity state changes) |
| `/deu anim previewplay` | Packet-based preview; does not commit state |
| `/deu anim stop` | Stop all animations on selected group |
| `/deu anim restore` | Revert group to pre-preview state |
| `/deu anim info` | Show frame count, tags, filter |
| `/deu anim listanims` | List animations currently playing on selected group |
| `/deu anim select <tag> local` | Load animation by tag |
| `/deu anim selectjson <file>` | Load animation from local JSON file |
| `/deu anim save local` | Save current animation to local storage |
| `/deu anim savejson` | Save as DEU JSON format |
| `/deu anim delete <tag> local` | Delete animation from storage |
| `/deu anim usefilter` | Restrict animation to tagged parts only |
| `/deu anim reverse` | Reverse the frame order |
| `/deu listanims local` | List all saved animation tags |

### Frame Parameters: `delay` and `duration`

These are the two numbers you supply to `addframe` and `editframe`:

- **`delay`** (ticks) — how long to wait after the previous frame ends before starting this one.
  Use `0` for immediate sequencing.
- **`duration`** (ticks) — Minecraft's native `interpolationDuration`. Display entities
  smoothly interpolate from their previous transformation to this frame's transformation
  over this many ticks. `0` = snap. `10` = 0.5 second smooth interpolation.

**Example:** `/deu anim addframe 0 10` — no delay, 10-tick interpolation.

---

## Part 4: Using DEU Animations from Java

### 4.1 Loading Assets

Bundle `.deanim` and `.deg` files in your plugin's resources folder (e.g. `src/main/resources/animations/`).
Load them at runtime using the plugin instance:

```java
// Load group from plugin resources
DisplayEntityGroup savedGroup = DisplayGroupManager.getGroup(plugin, "animations/menu_sword.deg");

// Load animation from plugin resources
DisplayAnimation animation = DisplayAnimationManager.getAnimation(plugin, "animations/menu_sword_idle.deanim");
```

Or load from DEU's local storage (files in `plugins/DisplayEntityUtils/`):

```java
DisplayEntityGroup savedGroup = DisplayGroupManager.getGroup(LoadMethod.LOCAL, "menu_sword");
DisplayAnimation animation = DisplayAnimationManager.getAnimation(LoadMethod.LOCAL, "menu_sword_idle");
```

### 4.2 Spawning a Group

```java
// Real server entity
SpawnedDisplayEntityGroup live = savedGroup.spawn(location, GroupSpawnedEvent.SpawnReason.PLUGIN);

// Packet-based (client-side only, shown per player)
PacketDisplayEntityGroup packet = savedGroup.createPacketGroup(location, false); // false = no spawn animation
```

Use `PacketDisplayEntityGroup` when the model should only be visible to specific players
(e.g. a main menu scene model) and when you don't want the entities to be persistent
server-side. This is the correct choice for Sword's scene system.

Show/hide packet groups per player:
```java
packet.showToPlayer(player);
packet.hideFromPlayer(player);
```

### 4.3 Playing Animations

```java
SpawnedDisplayAnimation anim = animation.toSpawnedDisplayAnimation();

// One-shot
DisplayAnimator animator = group.animate(anim);

// Looping
DisplayAnimator loopAnimator = group.animateLooping(anim);

// Per-player via packets (best for scene system)
DisplayAnimator.play(player, group, anim, DisplayAnimator.AnimationType.LINEAR);
DisplayAnimator.play(player, group, anim, DisplayAnimator.AnimationType.LOOP);
```

### 4.4 Stopping and Cleaning Up

```java
// Stop a specific animator
animator.stop(group);

// Stop all animations on a group
group.stopAnimations(true);

// Despawn the group
group.remove();
```

### 4.5 Jump to a Specific Frame (No Playback)

Useful for setting an initial pose or a state transition without interpolating through
all frames:

```java
// Server-side (affects real entities)
group.setToFrame(animation, 0);

// Packet-based per player
group.setToFrame(player, animation, 0);
```

### 4.6 Setting a Spawn Animation

If a group should always start playing an animation the moment it's spawned:

```java
// In-game via command:
// /deu group setspawnanim <anim-tag> local linear

// Via API (before spawning):
savedGroup.setSpawnAnimation(savedAnimation, DisplayAnimator.AnimationType.LOOP);
SpawnedDisplayEntityGroup live = savedGroup.spawn(location, reason);
// The spawn animation starts automatically.
```

---

## Part 5: Storage and File Locations

| Asset Type | DEU Local Storage Path | Extension |
|---|---|---|
| Groups | `plugins/DisplayEntityUtils/groups/` | `.deg` |
| Animations | `plugins/DisplayEntityUtils/animations/` | `.deanim` |
| DEU JSON groups | same folder, alternate format | `.json` |
| DEU JSON animations | same folder, alternate format | `.json` |
| BDE datapack zips | `plugins/DisplayEntityUtils/bdenginedatapacks/` | `.zip` |

**Bundling assets with the plugin:** Copy `.deg` and `.deanim` files into
`src/main/resources/` and load via `DisplayGroupManager.getGroup(plugin, path)` and
`DisplayAnimationManager.getAnimation(plugin, path)`. This ensures assets travel with
the jar and don't depend on server-side file placement.

---

## Part 6: Part Tags and Filtering

**Part tags** are string identifiers assigned to individual display entities within a
group. They let you target a subset of parts during an animation, so you can play an
"arm swing" animation without touching the legs, for instance.

Assign part tags in-game:
```
/deu parts settag <tag>    — tag the part you're looking at
```

Or in BDE, name each part in the Editor's part panel — those names become part tags
after conversion.

Apply a filter to an animation so it only affects tagged parts:
```
/deu anim usefilter         — restricts animation to parts matching the active filter
/deu anim unfilter          — remove filter
```

In Java:
```java
PartFilter filter = new PartFilter(Set.of("left_arm", "right_arm"));
animation.setPartFilter(filter);
```

---

## Part 7: Packet vs. Real Entity Groups — When to Use Which

| | Real Group (`SpawnedDisplayEntityGroup`) | Packet Group (`PacketDisplayEntityGroup`) |
|---|---|---|
| Persists server-side | Yes | No |
| Visible to all players | Yes (by default) | Only explicitly shown players |
| Performance (many players) | One set of entities | Per-player packet overhead |
| Can be selected in-game | Yes | No |
| Correct choice for | Persistent world decoration | Per-player scenes, menus, cutscenes |

For Sword's scene system, **packet groups are correct** — the main menu is per-player
and should not leave persistent entities in the world.

---

## Part 8: Complete Authoring Checklist

```
[ ] Build model in BDE Editor, name all parts
[ ] Build animations in BDE Animator, name each animation
[ ] Export datapack .zip from BDE
[ ] Place .zip in plugins/DisplayEntityUtils/bdenginedatapacks/
[ ] /deu bdengine convertdp <file> <group-tag> <anim-prefix>
[ ] /deu listgroups local  — verify group saved
[ ] /deu listanims local   — verify animations saved
[ ] /deu group spawn <group-tag> local  — sanity-check spawn
[ ] /deu anim select <anim-tag> local && /deu anim play  — sanity-check animation
[ ] Copy .deg and .deanim files into src/main/resources/animations/
[ ] Load via DisplayGroupManager.getGroup(plugin, ...) and DisplayAnimationManager.getAnimation(plugin, ...)
[ ] Spawn as PacketDisplayEntityGroup for per-player use
[ ] Play with DisplayAnimator.play(player, group, anim, AnimationType.LOOP)
```

---

## Part 9: Common Pitfalls

**Conversion spawns the model at your feet.** Stand in a clear, open area with no
obstructions. The conversion process temporarily spawns all entities to read their data
— it despawns them after saving.

**Animation frame IDs are 0-indexed.** `addframe` appends, `addframeafter 0` inserts
after the first frame.

**`delay` vs `duration` confusion.** `delay` is wait-time between frames. `duration` is
interpolation ticks within that frame. A 20-tick smooth motion between poses = `delay 0 duration 20`.

**Packet groups do not survive server restart.** If you need persistence, use real
groups with `togglepersist`. For scene system use, recreate packet groups on player join.

**DEU's `LoadMethod.LOCAL` looks in `plugins/DisplayEntityUtils/`.** Files bundled in
your plugin jar must use the `(JavaPlugin, String)` overload of `getGroup` and `getAnimation`.

**BDE export version mismatch.** If `convertdp` produces errors or empty output, try
`convertdpleg`. The modern format was introduced December 8, 2024.
