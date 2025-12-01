package btm.sword.utility;

import btm.sword.Sword;
import btm.sword.config.Config;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Debug {

    public static void debug(Class<?> clazz, int lineNum, String message) {
        if (Config.Debug.LOGGING_VERBOSE_CONFIG) {
            String toSend = "{ " + clazz + " } " + " -[" + lineNum + "]- :: " + message;
            Sword.print(toSend);

            Player me = Bukkit.getPlayer("BladeSworn");

            if (me != null && me.isValid()) {
                me.sendMessage(toSend);
            }
        }
    }
}
