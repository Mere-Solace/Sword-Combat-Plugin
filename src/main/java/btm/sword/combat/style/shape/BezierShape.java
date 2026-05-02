package btm.sword.combat.style.shape;

import java.util.function.Function;

import org.bukkit.util.Vector;

import btm.sword.combat.style.AttackShape;
import btm.sword.util.math.Basis;
import btm.sword.util.math.BezierUtil;
import btm.sword.util.math.ControlVectors;

/**
 * An {@link AttackShape} backed by a cubic Bézier curve.
 *
 * <p>Two modes are supported:
 * <ul>
 *   <li><b>Local-space</b> (default): control vectors are defined relative to the attacker's
 *       orientation. At resolve time, {@link ControlVectors#adjustToBasis} transforms them into
 *       world space before sampling. Use {@link #of} to create a local-space shape.</li>
 *   <li><b>World-space</b>: control vectors are already in world space and require no
 *       transformation. Used when geometry is computed procedurally at attack time (e.g.,
 *       sweeps targeting a specific entity). Use {@link #worldSpace} to create a world-space shape.</li>
 * </ul>
 *
 * @param controlVectors the cubic Bézier control points (local or world space)
 * @param isWorldSpace   {@code true} if {@code controlVectors} are in world space;
 *                       {@code false} if they are local to the attacker's orientation
 */
public record BezierShape(ControlVectors controlVectors,
                          boolean isWorldSpace) implements AttackShape {

    /**
     * Creates a local-space Bézier shape.
     *
     * <p>Control vectors are defined relative to the attacker's orientation.
     * {@link ControlVectors#adjustToBasis} is applied at resolve time.
     *
     * @param controlVectors local-space cubic Bézier control points
     * @return a new local-space {@code BezierShape}
     */
    public static BezierShape of(ControlVectors controlVectors) {
        return new BezierShape(controlVectors, false);
    }

    /**
     * Creates a world-space Bézier shape.
     *
     * <p>Control vectors are already in world space. No basis transformation is applied
     * at resolve time. Intended for procedurally generated attacks whose geometry is
     * computed at runtime from entity positions.
     *
     * @param controlVectors world-space cubic Bézier control points
     * @return a new world-space {@code BezierShape}
     */
    public static BezierShape worldSpace(ControlVectors controlVectors) {
        return new BezierShape(controlVectors, true);
    }

    /**
     * Returns the raw {@link ControlVectors} backing this shape.
     *
     * <p>For local-space shapes these are in canonical (local) space; for world-space shapes
     * they are in world space. Callers outside the attack system that need the raw control
     * points (e.g., for blade lunge movement) should use this method directly.
     *
     * @return the control vectors for this shape
     */
    @Override
    public ControlVectors controlVectors() {
        return controlVectors;
    }

    @Override
    public Function<Double, Vector> resolve(Basis basis, double rangeMultiplier) {
        ControlVectors adjusted = isWorldSpace
            ? controlVectors
            : controlVectors.adjustToBasis(basis, rangeMultiplier);
        return BezierUtil.cubicBezier3D(adjusted);
    }
}
