package btm.sword.system.attack.dev;

/**
 * Point placement modes available during a wand recording session.
 *
 * <p>Cycled by pressing DROP+SWAP while in {@link DevMode#RECORDING}. The current mode
 * determines how right-click places points and how many are required before a trajectory
 * segment is considered complete for saving.</p>
 *
 * <ul>
 *   <li>{@link #SINGLE_KEYFRAME} — each right-click places one independent keyframe.
 *       Any number of points may be accumulated before stopping.</li>
 *   <li>{@link #RAYCAST} — each right-click raycasts from the player's eye to the first
 *       solid block and places a keyframe at the hit position. Falls back to tip distance
 *       if nothing is hit.</li>
 *   <li>{@link #LINE_SEGMENT} — two right-clicks define the start and end of a linear
 *       segment; the saved keyframes use linear interpolation between them.</li>
 *   <li>{@link #BEZIER_CURVE} — four right-clicks define the four control points of a
 *       cubic Bézier trajectory.</li>
 * </ul>
 */
public enum PlacementMode {

    SINGLE_KEYFRAME("Single Keyframe", 1),
    RAYCAST("Raycast", 1),
    LINE_SEGMENT("Line Segment", 2),
    BEZIER_CURVE("Bezier Curve", 4);

    private final String label;
    private final int requiredPoints;

    PlacementMode(String label, int requiredPoints) {
        this.label = label;
        this.requiredPoints = requiredPoints;
    }

    /** Display label shown in the recording boss bar. */
    public String label() {
        return label;
    }

    /**
     * Minimum number of placed points required to produce a valid trajectory.
     * For {@link #SINGLE_KEYFRAME} this is {@code 1}; any additional placed points
     * are all included in the saved result.
     */
    public int requiredPoints() {
        return requiredPoints;
    }

    /** Returns the next mode in cycle order, wrapping around after the last entry. */
    public PlacementMode next() {
        PlacementMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
