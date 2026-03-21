package btm.sword.system.inventory;

// barrier is good material for cancel
// Remember the other types of windows!

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.AnimationBrowserMenu;
import btm.sword.system.inventory.menu.ArtifactPouchMenu;
import btm.sword.system.inventory.menu.CharacterMenu;
import btm.sword.system.inventory.menu.ConfigMenu;
import btm.sword.system.inventory.menu.CreativeInventoryMenu;
import btm.sword.system.inventory.menu.CurrencyMenu;
import btm.sword.system.inventory.menu.DEUBDEMenu;
import btm.sword.system.inventory.menu.DeuGroupBrowserMenu;
import btm.sword.system.inventory.menu.DevMenu;
import btm.sword.system.inventory.menu.DevStatEditorMenu;
import btm.sword.system.inventory.menu.MainMenu;
import btm.sword.system.inventory.menu.MaterialPouchMenu;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.inventory.menu.MovesetMenu;

public class InventoryMenuManager {
    private static final Map<Class<? extends Menu>, Function<SwordPlayer, ? extends Menu>> MENU_REGISTRY = new ConcurrentHashMap<>();

    public static <T extends Menu> void register(Class<T> menuType, Function<SwordPlayer, T> creator) {
        MENU_REGISTRY.put(menuType, creator);
    }

    public static void registerAll() {
        register(MainMenu.class, MainMenu::new);
        register(CharacterMenu.class, CharacterMenu::new);
        register(MovesetMenu.class, MovesetMenu::new);
        register(DevMenu.class, DevMenu::new);
        register(DevStatEditorMenu.class, DevStatEditorMenu::new);
        register(ConfigMenu.class, ConfigMenu::new);
        register(CreativeInventoryMenu.class, CreativeInventoryMenu::new);
        register(AnimationBrowserMenu.class, AnimationBrowserMenu::new);
        register(DEUBDEMenu.class, DEUBDEMenu::new);
        register(DeuGroupBrowserMenu.class, DeuGroupBrowserMenu::new);
        register(MaterialPouchMenu.class, MaterialPouchMenu::new);
        register(CurrencyMenu.class, CurrencyMenu::new);
        register(ArtifactPouchMenu.class, ArtifactPouchMenu::new);
        // SkillSelectionMenu is constructed with a slot index and is opened directly
    }

    @SuppressWarnings("unchecked")
    public static <T extends Menu> T create(Class<T> menuType, SwordPlayer player) {
        Function<SwordPlayer, ? extends Menu> menuCreationFunc = MENU_REGISTRY.get(menuType);
        if (menuCreationFunc == null) throw new IllegalArgumentException("No menu registered: " + menuType);
        return (T) menuCreationFunc.apply(player);
    }

    public static void openMenu(Class<? extends Menu> menuType, SwordPlayer swordPlayer) {
        swordPlayer.getPlayerMenuManager().openNewMenu(menuType);
    }
}
