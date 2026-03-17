package btm.sword.system.attack.style;

import java.util.function.Function;

import org.bukkit.util.Vector;

import btm.sword.system.attack.Attack;
import btm.sword.system.entity.base.SwordEntity;

/**
 * Defines the geometric and kinematic properties of a single attack sweep.
 *
 * <p>An {@code AttackProfile} supplies three pieces of information to {@link Attack}:
 * <ul>
 *   <li>The {@link AttackShape} that resolves into a parametric path function.</li>
 *   <li>A knockback function evaluated at hit time to determine knockback direction.</li>
 *   <li>An optional normal vector used by certain attack types for orientation math.</li>
 * </ul>
 *
 * <p>Implementations include the {@link AttackType} enum for named, static attack curves
 * and {@link btm.sword.system.attack.GeneratedAttackProfile} for runtime-computed sweep geometry.
 */
public interface AttackProfile {

    /**
     * Returns the {@link AttackShape} that defines this attack's sweep path.
     *
     * @return the shape backing this profile
     */
    AttackShape shape();

    /**
     * Returns a function that computes the knockback direction vector at hit time.
     *
     * @return knockback direction resolver
     */
    Function<SwordEntity, Function<Attack, Vector>> knockbackFunction();

    /**
     * Returns the optional normal vector for this attack, or {@code null} if not set.
     *
     * @return normal vector, or {@code null}
     */
    Vector normalVector();
}
