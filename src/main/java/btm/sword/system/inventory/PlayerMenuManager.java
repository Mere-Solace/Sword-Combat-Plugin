package btm.sword.system.inventory;

import java.util.LinkedList;

import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.inventory.menu.Menu;

public class PlayerMenuManager {
    private final SwordPlayer swordPlayer;
    private final LinkedList<Menu> menuHistory = new LinkedList<>();
    private int currentMenuIndex;

    public PlayerMenuManager(SwordPlayer swordPlayer) {
        this.swordPlayer = swordPlayer;
        currentMenuIndex = 0;
    }

    public <T extends Menu> void openNewMenu(Class<T> menuClass) {
        if (currentMenuIndex == menuHistory.size() - 1) {
            currentMenuIndex++;
            T menu = InventoryMenuManager.create(menuClass, swordPlayer);
            menuHistory.add(menu);
            menu.open();
        } // if the new menu was the menu forward in history
        else if (menuHistory.size() - 2 >= currentMenuIndex &&
            menuClass.equals(menuHistory.get(currentMenuIndex + 1).getClass())) {
            currentMenuIndex++;
            menuHistory.get(currentMenuIndex).open();
        }
        else {
            // remove the last elements up to the current menu
            int toRemove = menuHistory.size() - 1 - currentMenuIndex;
            for (int i = 0; i < toRemove; i++) {
                menuHistory.removeLast();
            }
            currentMenuIndex++;
            T menu = InventoryMenuManager.create(menuClass, swordPlayer);
            menuHistory.add(menu);
            menu.open();
        }
    }

    public void openPreviousMenu() {
        if (noPreviousMenu()) return;

        int numberOfMenus = menuHistory.size();
        currentMenuIndex = numberOfMenus - 2; // e.g. if cur index is 1 (size will be 2), go to 0
        menuHistory.get(currentMenuIndex).open();
    }

    public void reopenCurrentMenu() {
        menuHistory.get(currentMenuIndex).open();
    }

    public void openForwardPreviousMenu() {
        if (noForwardPreviousMenu()) return;

        menuHistory.get(++currentMenuIndex).open();
    }

    public boolean noPreviousMenu() {
        return currentMenuIndex < 1;
    }

    public boolean noForwardPreviousMenu() {
        return currentMenuIndex == menuHistory.size() - 1;
    }
}
