package btm.sword.system.attack.simulation;

/**
 * Specifies the interpolation mode for a {@link ControlPointTrajectory}.
 */
public enum ControlMode {

    /**
     * Linear interpolation between two control points.
     * {@link ControlPointTrajectory} requires exactly 2 {@link ControlPoint}s.
     */
    LINEAR,

    /**
     * Cubic Bézier interpolation across four control points.
     * {@link ControlPointTrajectory} requires exactly 4 {@link ControlPoint}s:
     * start, two interior tangent handles, and end.
     */
    BEZIER
}
