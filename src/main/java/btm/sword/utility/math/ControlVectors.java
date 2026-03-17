package btm.sword.utility.math;

import java.util.function.Function;
import java.util.function.Supplier;

import org.bukkit.util.Vector;

/**
 * Holds the four cubic Bézier control points (start, end, c1, c2) for an attack sweep path.
 *
 * <p>Each point is stored as a {@link Supplier}{@code <Vector>} so the value can be resolved
 * lazily at attack-execution time. This allows control-point values driven by
 * {@link btm.sword.config.Config} entries to reflect live config changes without requiring
 * a server restart.
 *
 * <p>Two construction paths are supported:
 * <ul>
 *   <li>Plain {@link Vector} constructor — vectors are captured eagerly and wrapped in a
 *       supplier that always returns a clone of the captured value. Use this when the
 *       geometry is computed at runtime (e.g., procedural world-space sweeps).</li>
 *   <li>{@link Supplier}{@code <Vector>} constructor — suppliers are invoked each time a
 *       point is read. Use this for config-driven attack types so that hot-reloaded
 *       values are picked up automatically.</li>
 * </ul>
 *
 * <p>The static {@link #of} factory methods mirror these two paths.
 */
public class ControlVectors {

    private final Supplier<Vector> start;
    private final Supplier<Vector> end;
    private final Supplier<Vector> c1;
    private final Supplier<Vector> c2;

    /**
     * Creates a {@code ControlVectors} whose points are baked-in from the given vectors.
     *
     * <p>Each vector is cloned at construction time, so subsequent mutations to the
     * originals have no effect.
     *
     * @param start the start control point
     * @param end   the end control point
     * @param c1    the first interior control point
     * @param c2    the second interior control point
     */
    public ControlVectors(Vector start, Vector end, Vector c1, Vector c2) {
        Vector s = start.clone();
        Vector e = end.clone();
        Vector c1c = c1.clone();
        Vector c2c = c2.clone();
        this.start = s::clone;
        this.end = e::clone;
        this.c1 = c1c::clone;
        this.c2 = c2c::clone;
    }

    /**
     * Creates a {@code ControlVectors} whose points are resolved lazily from the given suppliers.
     *
     * <p>Each call to {@link #start()}, {@link #end()}, {@link #c1()}, or {@link #c2()} will
     * invoke the corresponding supplier, making this suitable for config-backed entries that
     * may change between attacks.
     *
     * @param start supplier for the start control point
     * @param end   supplier for the end control point
     * @param c1    supplier for the first interior control point
     * @param c2    supplier for the second interior control point
     */
    public ControlVectors(Supplier<Vector> start, Supplier<Vector> end,
                          Supplier<Vector> c1, Supplier<Vector> c2) {
        this.start = start;
        this.end = end;
        this.c1 = c1;
        this.c2 = c2;
    }

    /**
     * Creates a baked-in {@code ControlVectors} from four plain vectors.
     *
     * @param start the start control point
     * @param end   the end control point
     * @param c1    the first interior control point
     * @param c2    the second interior control point
     * @return a new {@code ControlVectors} with eagerly captured values
     */
    public static ControlVectors of(Vector start, Vector end, Vector c1, Vector c2) {
        return new ControlVectors(start, end, c1, c2);
    }

    /**
     * Creates a lazily-resolved {@code ControlVectors} from four suppliers.
     *
     * @param start supplier for the start control point
     * @param end   supplier for the end control point
     * @param c1    supplier for the first interior control point
     * @param c2    supplier for the second interior control point
     * @return a new {@code ControlVectors} backed by the given suppliers
     */
    public static ControlVectors of(Supplier<Vector> start, Supplier<Vector> end,
                                    Supplier<Vector> c1, Supplier<Vector> c2) {
        return new ControlVectors(start, end, c1, c2);
    }

    /**
     * Returns the start control point.
     *
     * @return the start vector (fresh value from the backing supplier)
     */
    public Vector start() {
        return start.get();
    }

    /**
     * Returns the end control point.
     *
     * @return the end vector (fresh value from the backing supplier)
     */
    public Vector end() {
        return end.get();
    }

    /**
     * Returns the first interior control point.
     *
     * @return the c1 vector (fresh value from the backing supplier)
     */
    public Vector c1() {
        return c1.get();
    }

    /**
     * Returns the second interior control point.
     *
     * @return the c2 vector (fresh value from the backing supplier)
     */
    public Vector c2() {
        return c2.get();
    }

    /**
     * Returns a new {@code ControlVectors} with each point transformed into the given basis
     * and scaled by {@code multiplier}.
     *
     * <p>All four suppliers are resolved once during this call; the returned instance is
     * baked-in (not supplier-backed) because the transformation has already been applied.
     *
     * @param basis      the target coordinate frame
     * @param multiplier scale factor applied after basis transformation
     * @return a new baked-in {@code ControlVectors} in the given basis
     */
    public ControlVectors adjustToBasis(Basis basis, double multiplier) {
        Function<Vector, Vector> adjust = v -> VectorUtil.transformWithNewBasis(basis, v).multiply(multiplier);
        return of(
            adjust.apply(start()),
            adjust.apply(end()),
            adjust.apply(c1()),
            adjust.apply(c2())
        );
    }
}
