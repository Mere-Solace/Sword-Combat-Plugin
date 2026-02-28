# Menu System

## Overview

The menu system provides inventory-based GUIs using the InvUI library (resolved at runtime via `SwordPluginLoader`). Menus are registered in a central `InventoryMenuManager` and tracked per-player with browser-like back/forward history in `PlayerMenuManager`.

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `InventoryMenuManager` | `system/inventory/InventoryMenuManager.java` | Static registry mapping `Class<? extends Menu>` to `Function<SwordPlayer, Menu>` factory functions. Provides `register()`, `create()`, and `openMenu()`. |
| `PlayerMenuManager` | `system/inventory/PlayerMenuManager.java` | Per-player menu history manager using a `LinkedList<Menu>`. Supports `openNewMenu()`, `openPreviousMenu()`, `openForwardPreviousMenu()`, and `reopenCurrentMenu()`. Handles deduplication and history truncation. |
| `Menu` | `system/inventory/menu/Menu.java` | Abstract base for all menus. |
| `MainMenu` | `system/inventory/menu/MainMenu.java` | Primary menu opened via the echo shard button (slot 8). |
| `CharacterMenu` | `system/inventory/menu/CharacterMenu.java` | Character stats/skills view. |
| `SkillSelectionMenu` | `system/inventory/menu/SkillSelectionMenu.java` | Skill equip menu (not registered in `InventoryMenuManager`; opened directly with slot parameter). |
| `ForwardItem` / `PreviousItem` | `system/inventory/item/` | Navigation items for menu forward/back. |

## Menu Registration

```java
public static void registerAll() {
    register(MainMenu.class, MainMenu::new);
    register(CharacterMenu.class, CharacterMenu::new);
}
```

Called in `Sword.onEnable()`.

## Menu History

`PlayerMenuManager` maintains a `LinkedList<Menu>` acting as a browser-like history stack:

- Opening a new menu truncates forward history and appends.
- If the same menu class is already current, it refreshes instead.
- If the next menu in history matches, it steps forward without creating a new instance.
- `openPreviousMenu()` and `openForwardPreviousMenu()` navigate the history.
- `refreshMenu(Class)` re-opens all instances of a menu class in history (useful after data changes).

## Menu Button

Players always have an echo shard in slot 8 tagged with `MAIN_MENU_BUTTON_KEY`. Right-clicking it opens the `MainMenu` via `InventoryMenuManager.openMenu()`.

## Dependencies

- **InvUI library** (1.47) -- Loaded at runtime via `SwordPluginLoader` using Maven dependency resolution
- **SwordPlayer** -- Menu context and player reference
- **PlayerSkillContainer** -- For skill selection menus
- **KeyRegistry** -- Item tagging for the menu button

## Known Limitations

- `SkillSelectionMenu` is not registered in `InventoryMenuManager` and uses a different construction pattern (requires slot index), breaking the uniform factory pattern.
- Two overloads of `openNewMenu()` exist (one taking `Class`, one taking instance), and the `openNewMenu(Menu instance)` path is separate code that mostly duplicates the class-based path.
