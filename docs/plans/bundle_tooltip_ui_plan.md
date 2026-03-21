# Bundle Tooltip UI System (Paper)

## Overview

This document outlines a system for leveraging Minecraft bundle tooltips
as a client-rendered, scrollable UI.

------------------------------------------------------------------------

## Core Concept

Bundles render their contents client-side based on NBT. By controlling
this NBT, we can simulate a UI.

------------------------------------------------------------------------

## System Architecture

### Components

1.  Bundle Controller
2.  Data Source (abilities/items)
3.  Input Handler
4.  Renderer (NBT updater)

------------------------------------------------------------------------

## Implementation Steps

### Step 1: Create Base Bundle

-   Create ItemStack(Material.BUNDLE)
-   Apply BundleMeta
-   Hide default visuals if needed

------------------------------------------------------------------------

### Step 2: Build Display Items

Each entry should: - Represent a UI element - Use display name + lore -
Optionally use CustomModelData

------------------------------------------------------------------------

### Step 3: Populate Bundle

-   Convert UI data into ItemStacks
-   Maintain ordered list
-   Assign with meta.setItems()

------------------------------------------------------------------------

### Step 4: Pagination System

-   Store full list
-   Slice into pages (max \~27 items)
-   Track page index

------------------------------------------------------------------------

### Step 5: Input Handling

Use: - PlayerItemHeldEvent (scroll) - Sneak toggle - Click actions

Map inputs to: - page change - category switch

------------------------------------------------------------------------

### Step 6: Rendering Loop

-   Update bundle contents when state changes
-   Replace item in inventory slot
-   Force refresh if needed

------------------------------------------------------------------------

### Step 7: Prevent Default Behavior

Cancel PlayerInteractEvent for bundles.

------------------------------------------------------------------------

## Advanced Features

### Scrolling Simulation

Shift window over list.

### Categories

Use separators (glass panes, etc.)

### Dynamic Updates

Reflect cooldowns, charges, etc.

------------------------------------------------------------------------

## Limitations

-   No click detection inside tooltip
-   Client caching requires forced refresh
-   Limited visual layout control

------------------------------------------------------------------------

## Performance Notes

-   Avoid updating every tick
-   Prefer event-driven updates

------------------------------------------------------------------------

## Use Cases

-   Ability selector
-   Weapon stats viewer
-   Skill tree preview

------------------------------------------------------------------------

## Summary

This system enables a lightweight, non-intrusive UI using bundle
tooltips without opening inventories.
