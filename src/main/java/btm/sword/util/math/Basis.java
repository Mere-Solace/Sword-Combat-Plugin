package btm.sword.util.math;

import org.bukkit.Location;
import org.bukkit.util.Vector;

@SuppressWarnings("all") // Wants to convert into record class but that is incorrect
public class Basis {
    private final Vector right;
    private final Vector up;
    private final Vector forward;

    public Basis(Vector right, Vector up, Vector forward) {
        this.right = right.normalize();
        this.up = up.normalize();
        this.forward = forward.normalize();
    }

    public Basis(Location origin, boolean orientWithPitch) {
        Basis created = orientWithPitch ?
            VectorUtil.getBasis(origin, origin.getDirection()) :
            VectorUtil.getBasisWithoutPitch(origin);

        this.right = created.right();
        this.up = created.up();
        this.forward = created.forward();
    }

    public Vector right() {
        return right.clone();
    }

    public Vector up() {
        return up.clone();
    }

    public Vector forward() {
        return forward.clone();
    }
}
