package btm.sword.system.attack.style;

import java.util.function.Function;

import org.bukkit.util.Vector;

import btm.sword.utility.math.Basis;

/**
 * Defines a parametric sweep path for an attack.
 *
 * <p>Implementations resolve an attacker's {@link Basis} and a range multiplier into a
 * function mapping {@code t ∈ [0, 1]} to world-space offset vectors. The resulting
 * function is consumed by {@link btm.sword.system.attack.Attack} to drive hit detection
 * and visual effects along the weapon's path each iteration tick.
 *
 * <p>Implementations are free to derive the path from any geometry: Bézier curves, arcs,
 * straight lines, polygons, or procedurally generated curves. The only contract is that
 * the returned function accepts a {@code double} parameter in {@code [0, 1]} and returns
 * a world-space {@link Vector} offset from the attack origin.
 */
public interface AttackShape {

    /**
     * Resolves this shape into a parametric path function.
     *
     * <p>The returned function maps {@code t ∈ [0, 1]} to a world-space {@link Vector}
     * offset from the attack origin. The {@code basis} describes the attacker's local
     * coordinate frame at the moment the attack begins; shapes that are defined in local
     * space must transform their geometry into world space using this basis.
     *
     * @param basis           the attacker's local coordinate frame at attack start
     * @param rangeMultiplier a scalar applied to the shape's effective reach
     * @return a function mapping {@code t} to world-space offset vectors
     */
    Function<Double, Vector> resolve(Basis basis, double rangeMultiplier);
}
