package btm.sword.utility;

import java.util.List;

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

    /** Master toggle for {@link #debug} output. Controlled via the in-game dev menu. */
    public static boolean VERBOSE_ENABLED = false;

    /**
     * When {@code false}, {@link btm.sword.system.item.special.NonMovableItem#isNonMovable}
     * always returns {@code false}, letting devs freely move special items for testing.
     */
    public static boolean SPECIAL_ITEM_CHECKS_ENABLED = true;

    @SuppressWarnings("all")
    public static void debug(Class<?> clazz, int lineNum, String message) {
        if (VERBOSE_ENABLED && Config.Debug.LOGGING_VERBOSE_CONFIG) {
            String toSend = "> " + clazz + " line" + " [" + lineNum + "] :: " + message;
            Sword.print(toSend);

            Player me = Bukkit.getPlayer("BladeSworn");

            if (me != null && me.isValid()) {
                me.sendMessage(toSend);
            }
        }
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
