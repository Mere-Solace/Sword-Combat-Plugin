package btm.sword.menu;

// barrier is good material for cancel
// Remember the other types of windows!

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import btm.sword.entity.player.SwordPlayer;
import btm.sword.menu.dev.AllKeyframeEffectsMenu;
import btm.sword.menu.dev.AnimationBrowserMenu;
import btm.sword.menu.dev.ConfigMenu;
import btm.sword.menu.dev.CreativeInventoryMenu;
import btm.sword.menu.dev.DEUBDEMenu;
import btm.sword.menu.dev.DeuGroupBrowserMenu;
import btm.sword.menu.dev.DevMenu;
import btm.sword.menu.dev.KeyframeVisualsMenu;
import btm.sword.menu.dev.TestingMenu;
import btm.sword.menu.dev.WeaponDisplayEditorMenu;

/** Registry and factory for all InvUI-backed menus; maps menu types to their player-scoped constructors. */
public final class InventoryMenuManager {

    private InventoryMenuManager() {}

    private static final Map<Class<? extends Menu>, Function<SwordPlayer, ? extends Menu>> MENU_REGISTRY = new ConcurrentHashMap<>();

    /** Registers a menu type with its player-scoped factory function. */
    public static <T extends Menu> void register(Class<T> menuType, Function<SwordPlayer, T> creator) {
        MENU_REGISTRY.put(menuType, creator);
    }

    /** Registers all known menu types with their default factory functions. */
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
        register(ItemLibraryMenu.class, ItemLibraryMenu::new);
        register(WeaponDisplayEditorMenu.class, WeaponDisplayEditorMenu::new);
        register(KeyframeVisualsMenu.class, KeyframeVisualsMenu::new);
        register(AllKeyframeEffectsMenu.class, AllKeyframeEffectsMenu::new);
        register(TestingMenu.class, TestingMenu::new);
        // SkillSelectionMenu is constructed with a slot index and is opened directly
    }

    /** Creates and returns a menu instance of the given type for the player. */
    @SuppressWarnings("unchecked")
    public static <T extends Menu> T create(Class<T> menuType, SwordPlayer player) {
        Function<SwordPlayer, ? extends Menu> menuCreationFunc = MENU_REGISTRY.get(menuType);
        if (menuCreationFunc == null) throw new IllegalArgumentException("No menu registered: " + menuType);
        return (T) menuCreationFunc.apply(player);
    }

    /** Delegates to the player's {@link PlayerMenuManager} to open a new instance of the given menu type. */
    public static void openMenu(Class<? extends Menu> menuType, SwordPlayer swordPlayer) {
        swordPlayer.getPlayerMenuManager().openNewMenu(menuType);
    }
}
