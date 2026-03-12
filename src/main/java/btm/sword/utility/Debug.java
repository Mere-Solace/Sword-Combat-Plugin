package btm.sword.utility;

import java.util.List;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.VectorUtil;

public class Debug {

    /**
     * Master toggle for {@link #debug} output. Reads from {@link Config.Debug#LOGGING_VERBOSE_DEBUG}
     * so the value persists across hot-reloads and is configurable from config.yaml.
     */
    public static final Supplier<Boolean> VERBOSE_ENABLED = () -> Config.Debug.LOGGING_VERBOSE_DEBUG;

    /**
     * When {@code false}, {@link btm.sword.system.item.special.NonMovableItem#isNonMovable}
     * always returns {@code false}, letting devs freely move special items for testing.
     */
    public static boolean SPECIAL_ITEM_CHECKS_ENABLED = true;

    @SuppressWarnings("all")
    public static void debug(Class<?> clazz, int lineNum, String message) {
        if (VERBOSE_ENABLED.get() && Config.Debug.LOGGING_VERBOSE_CONFIG) {
            String toSend = "> " + clazz + " line" + " [" + lineNum + "] :: " + message;
            Sword.print(toSend);

            Player me = Bukkit.getPlayer("BladeSworn");

            if (me != null && me.isValid()) {
                me.sendMessage(toSend);
            }
        }
    }

    public static void combatDebug(Class<?> clazz, int lineNum, String message) {
        if (!Config.Debug.LOGGING_VERBOSE_COMBAT) return;
        debug(clazz, lineNum, "[Combat] " + message);
    }

    public static void debugBlob(Location debugLocation) {
        Prefab.Particles.DEBUG_BLOB.display(debugLocation);
    }

    public static void debugBasis(Location location, Vector direction, double length) {
        Basis testBasis = VectorUtil.getBasis(location, direction);
        DrawUtil.line(List.of(Prefab.Particles.TEST_SWORD_BLUE),
            location, testBasis.right(), length, 0.25);
        DrawUtil.line(List.of(Prefab.Particles.TEST_SWORD_BLUE),
            location, testBasis.up(), length, 0.25);
        DrawUtil.line(List.of(Prefab.Particles.TEST_SWORD_BLUE),
            location, testBasis.forward(), length, 0.25);
    }
}
