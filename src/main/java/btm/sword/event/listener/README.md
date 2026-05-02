# Listeners

Bukkit event listeners and packet interceptors for the Sword plugin.

## Overview

| Class | Purpose |
|---|---|
| `InputListener` | Translates raw Bukkit input events into `InputKey` actions and feeds the `InputExecutionTree` |
| `PlayerListener` | Player lifecycle hooks (join, quit, respawn, etc.) |
| `EntityListener` | Entity damage, death, and combat event hooks |
| `WorldListener` | Block placement and item consumption hooks |
| `SystemListener` | Internal plugin lifecycle and reload events |

---

## WorldListener — Block Placement

Block placement is currently cancelled via `BlockPlaceEvent` when `Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING` is `false`.

**Why not ProtocolLib packet interception?**

A `BlockPlacePacketListener` (intercepting `USE_ITEM_ON`) was implemented and reverted. The goal was to cancel placement *before* the server processes it, eliminating the client-side ghost-block flicker that `BlockPlaceEvent` cancellation causes (the client renders the block optimistically before receiving the server resync).

The approach works, but was shelved because **in production, players will not hold placeable items** — the flicker is a non-issue when block items are gated to a building world or creative-only context.

**When revisiting**, the implementation notes are:

- `PacketAdapter` on `PacketType.Play.Client.USE_ITEM_ON`, `ListenerPriority.HIGH`
- `getHands().read(0)` returns `EnumWrappers.Hand` (not Bukkit `EquipmentSlot`)
- `getDirections().read(0)` throws `FieldAccessException` — the face is inside the `BlockHitResult` and ProtocolLib does not extract it separately for this packet
- **Resync fix:** after `event.setCancelled(true)`, send `player.sendBlockChange()` for all 6 cardinal neighbors of the target block position — this corrects the client prediction without needing the face
- Once the packet listener is active, the `if (!BLOCK_INTERACTION_ALLOW_BLOCK_PLACING)` early-return in `WorldListener.onBlockPlace` can be removed (the packet listener fires first and the event never reaches Bukkit)
- ProtocolLib 5.3.0 is already a hard dependency in `build.gradle` and `paper-plugin.yml`
