package btm.sword.config.section;

import org.bukkit.util.Vector;

/**
 *
 */
public final class DirectionConfig {

    private DirectionConfig() {}

    private static final Vector UP = new Vector(0, 1, 0);
    /** @return a fresh clone of the world-up unit vector {@code (0, 1, 0)}. */
    public static Vector up() { return UP.clone(); }

    private static final Vector DOWN = new Vector(0, -1, 0);
    /** @return a fresh clone of the world-down unit vector {@code (0, -1, 0)}. */
    public static Vector down() { return DOWN.clone(); }

    private static final Vector NORTH = new Vector(0, 0, -1);
    /** @return a fresh clone of the north unit vector {@code (0, 0, -1)}. */
    public static Vector north() { return NORTH.clone(); }

    private static final Vector SOUTH = new Vector(0, 0, 1);
    /** @return a fresh clone of the south unit vector {@code (0, 0, 1)}. */
    public static Vector south() { return SOUTH.clone(); }

    private static final Vector OUT_UP = new Vector(0, 1, 1);
    /** @return a fresh clone of the out-and-up diagonal vector {@code (0, 1, 1)}. */
    public static Vector outUp() { return OUT_UP.clone(); }

    private static final Vector OUT_DOWN = new Vector(0, -1, 1);
    /** @return a fresh clone of the out-and-down diagonal vector {@code (0, -1, 1)}. */
    public static Vector outDown() { return OUT_DOWN.clone(); }
}
