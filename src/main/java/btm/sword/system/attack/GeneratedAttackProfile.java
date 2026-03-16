package btm.sword.system.attack;

import java.util.function.Function;

import org.bukkit.util.Vector;

import btm.sword.system.attack.style.AttackProfile;
import btm.sword.system.attack.style.AttackShape;
import btm.sword.system.attack.style.shape.BezierShape;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.utility.math.ControlVectors;

/**
 * An {@link AttackProfile} backed by runtime-computed, world-space {@link ControlVectors}.
 *
 * <p>Used when sweep geometry is determined dynamically at attack time (e.g., a heavy swing
 * targeting a specific entity). Because the control vectors are already in world space, the
 * underlying {@link BezierShape} is created in world-space mode and skips basis transformation
 * at resolve time.
 */
public class GeneratedAttackProfile implements AttackProfile {

    private final AttackShape shape;
    private final Function<SwordEntity, Function<Attack, Vector>> knockBackFunction;

    /**
     * Creates a new {@code GeneratedAttackProfile} with pre-computed world-space control vectors.
     *
     * @param controlVectors  world-space cubic Bézier control points
     * @param knockBackFunction function that computes knockback direction at hit time
     */
    public GeneratedAttackProfile(ControlVectors controlVectors, Function<SwordEntity, Function<Attack, Vector>> knockBackFunction) {
        this.shape = BezierShape.worldSpace(controlVectors);
        this.knockBackFunction = knockBackFunction;
    }

    @Override
    public AttackShape shape() {
        return shape;
    }

    @Override
    public Function<SwordEntity, Function<Attack, Vector>> knockbackFunction() {
        return knockBackFunction;
    }

    @Override
    public Vector normalVector() {
        return null;
    }
}
