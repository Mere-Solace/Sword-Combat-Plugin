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
        currentMenuIndex = -1;
    }

    private <T extends Menu> void addAndOpenMenu(Class<T> menuClass) {
        T menu = InventoryMenuManager.create(menuClass, swordPlayer);
        menuHistory.add(menu);
        menu.open();
    }

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
                menuHistory.get(currentMenuIndex).open();
                return;
        }

        // truncate forward (redo) history
        while (menuHistory.size() - 1 > currentMenuIndex) {
            menuHistory.removeLast();
        }

        addAndOpenMenu(menuClass);
        currentMenuIndex = menuHistory.size() - 1;
    }

    public void openPreviousMenu() {
        if (noPreviousMenu()) return;
        menuHistory.get(--currentMenuIndex).open();
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
