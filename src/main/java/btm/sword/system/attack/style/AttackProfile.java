package btm.sword.system.attack.style;

import java.util.function.Function;

import org.bukkit.util.Vector;

import btm.sword.system.attack.Attack;
import btm.sword.utility.math.ControlVectors;

public interface AttackProfile {
    ControlVectors controlVectors();
    Function<Attack, Vector> knockbackFunction();
    Vector normalVector();
}
