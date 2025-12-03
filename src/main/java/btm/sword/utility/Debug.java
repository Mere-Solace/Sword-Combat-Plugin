package btm.sword.utility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import btm.sword.Sword;
import btm.sword.config.Config;

public class Debug {

    @SuppressWarnings("all")
    public static void debug(Class<?> clazz, int lineNum, String message) {
        if (true && Config.Debug.LOGGING_VERBOSE_CONFIG) { // for me to change
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
}
