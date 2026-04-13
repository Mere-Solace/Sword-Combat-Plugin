package btm.sword.utility.math;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/** An orthonormal right-up-forward coordinate basis used to orient attacks and trajectories in world space. */
@SuppressWarnings("all") // Wants to convert into record class but that is incorrect
public class Basis {
    private final Vector right;
    private final Vector up;
    private final Vector forward;

    /** Constructs a basis from the three given axis vectors, normalising each on construction. */
    public Basis(Vector right, Vector up, Vector forward) {
        this.right = right.normalize();
        this.up = up.normalize();
        this.forward = forward.normalize();
    }

    /** Constructs a basis from a world location, optionally including pitch in the orientation. */
    public Basis(Location origin, boolean orientWithPitch) {
        Basis created = orientWithPitch ?
            VectorUtil.getBasis(origin, origin.getDirection()) :
            VectorUtil.getBasisWithoutPitch(origin);

        this.right = created.right();
        this.up = created.up();
        this.forward = created.forward();
    }

    /** Returns a clone of the right-axis vector. */
    public Vector right() {
        return right.clone();
    }

    /** Returns a clone of the up-axis vector. */
    public Vector up() {
        return up.clone();
    }

    /** Returns a clone of the forward-axis vector. */
    public Vector forward() {
        return forward.clone();
    }
}
