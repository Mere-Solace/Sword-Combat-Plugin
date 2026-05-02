package btm.sword.combat.simulation;

import org.joml.Vector3f;

/**
 * A single control point for a {@link ControlPointSequence}.
 *
 * <p>Stores a local-space position and the half-extents of the OBB at this point.
 * Positions are in the attacker's local coordinate system; they are transformed to
 * world space at sample time by {@link ControlPointSequence#sample}.</p>
 *
 * @param position    local-space position of this control point
 * @param halfExtents half-extents of the OBB at this control point
 */
public record ControlPoint(Vector3f position, Vector3f halfExtents) {}
