package btm.sword.menu;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.KeyRegistry;
import btm.sword.runtime.scheduler.SwordScheduler;

/** Manages a player's menu navigation history, supporting forward and backward navigation between menus. */
public class PlayerMenuManager {
    private final SwordPlayer swordPlayer;
    private final LinkedList<Menu> menuHistory = new LinkedList<>();
    private int currentMenuIndex;

    /** Constructs a menu manager for the given player with an empty history. */
    public PlayerMenuManager(SwordPlayer swordPlayer) {
        this.swordPlayer = swordPlayer;
        currentMenuIndex = -1;
    }

    private void performOpen(Menu menu) {
        Player player = swordPlayer.player();

        // #221: Return any held cursor item to inventory rather than destroying it.
        // If the inventory is full, the item is dropped in the world instead.
        ItemStack cursor = player.getItemOnCursor();
        if (!cursor.isEmpty()) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(cursor.clone());
            leftover.values().forEach(swordPlayer::spawnInventoryDrop);
            player.setItemOnCursor(ItemStack.of(Material.AIR));
        }

        menu.open();

        // Deferred cursor fix: opening an InvUI window during InventoryClickEvent handling can
        // desync the client cursor state, causing the clicked item (e.g. the menu button) to
        // reappear on the cursor after the window opens. Non-movable items should never be on
        // cursor, so clear them on the next tick to correct the desync.
        SwordScheduler.runBukkitTaskLater(() -> {
            ItemStack afterCursor = player.getItemOnCursor();
            if (!afterCursor.isEmpty() && KeyRegistry.hasKey(afterCursor, KeyRegistry.NON_MOVABLE_KEY)) {
                player.setItemOnCursor(ItemStack.of(Material.AIR));
            }
        }, 50, TimeUnit.MILLISECONDS);
    }

    private <T extends Menu> void addAndOpenMenu(Class<T> menuClass) {
        T menu = InventoryMenuManager.create(menuClass, swordPlayer);
        menuHistory.add(menu);
        performOpen(menu);
    }

    /** Add an already-constructed menu instance to the history and open it. */
    public void openNewMenu(Menu menuInstance) {
        // if current menu exists and is the same class as the potential new menu, simply refresh
        if (currentMenuIndex >= 0
            && currentMenuIndex < menuHistory.size() &&
            menuInstance.getClass().equals(menuHistory.get(currentMenuIndex).getClass())) {
                reopenCurrentMenu();
                return;
        }

        // if next in history matches the new menu class, just step forward
        if (currentMenuIndex + 1 < menuHistory.size() &&
            menuInstance.getClass().equals(menuHistory.get(currentMenuIndex + 1).getClass())) {
                currentMenuIndex++;
                performOpen(menuHistory.get(currentMenuIndex));
                return;
        }

        // truncate forward (redo) history
        while (menuHistory.size() - 1 > currentMenuIndex) {
            menuHistory.removeLast();
        }

        menuHistory.add(menuInstance);
        currentMenuIndex = menuHistory.size() - 1;
        performOpen(menuInstance);
    }

    /**
     * Re-open every instance of the supplied menu class in the history without changing the current index.
     * Useful to refresh background menus (e.g. CharacterMenu) after changes from a child menu.
     */
    public void refreshMenu(Class<? extends Menu> menuClass) {
        for (Menu m : menuHistory) {
            if (m.getClass().equals(menuClass)) m.open();
        }
    }

    /** Opens the given menu type, reusing history entries where possible before creating a new instance. */
    public <T extends Menu> void openNewMenu(Class<T> menuClass) {
        // if current menu exists and is the same as the potential new menu, simply refresh the menu
        if (currentMenuIndex >= 0
            && currentMenuIndex < menuHistory.size() &&
            menuClass.equals(menuHistory.get(currentMenuIndex).getClass())) {
                reopenCurrentMenu();
                return;
        }

        // if next in history matches the new menu, just step forward
        if (currentMenuIndex + 1 < menuHistory.size() &&
            menuClass.equals(menuHistory.get(currentMenuIndex + 1).getClass())) {
                currentMenuIndex++;
                performOpen(menuHistory.get(currentMenuIndex));
                return;
        }

        // truncate forward (redo) history
        while (menuHistory.size() - 1 > currentMenuIndex) {
            menuHistory.removeLast();
        }

        addAndOpenMenu(menuClass);
        currentMenuIndex = menuHistory.size() - 1;
    }

    /** Navigates back in menu history, opening the previous menu. Does nothing if at the start. */
    public void openPreviousMenu() {
        if (noPreviousMenu()) return;
        performOpen(menuHistory.get(--currentMenuIndex));
    }

    /** Re-opens the currently active menu, refreshing its contents. */
    public void reopenCurrentMenu() {
        performOpen(menuHistory.get(currentMenuIndex));
    }

    /** Navigates forward in menu history, opening the next menu. Does nothing if at the end. */
    public void openForwardPreviousMenu() {
        if (noForwardPreviousMenu()) return;
        performOpen(menuHistory.get(++currentMenuIndex));
    }

    /** Returns {@code true} if there is no earlier menu in the navigation history. */
    public boolean noPreviousMenu() {
        return currentMenuIndex < 1;
    }

    /** Returns {@code true} if there is no later menu in the navigation history. */
    public boolean noForwardPreviousMenu() {
        return currentMenuIndex == menuHistory.size() - 1;
    }
}
