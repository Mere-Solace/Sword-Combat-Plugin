package btm.sword.utility.display;

import java.util.Collection;
import java.util.function.Supplier;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

/**
 * Wrapper class for handling Bukkit {@link Particle} effects with supplier-based parameters.
 * <p>
 * All fields are backed by {@link Supplier} instances, allowing values to be resolved at runtime.
 * This enables config hot-reload, per-player variation, and any other runtime-resolved particle
 * properties without code changes. For static uses, pass simple {@code () -> value} suppliers.
 * </p>
 * <p>
 * Construct with a particle and optionals, then chain {@code with*()} methods for optional fields:
 * <pre>{@code
 * new ParticleWrapper(() -> Particle.CRIT, () -> 5, () -> 0.2, () -> 0.2, () -> 0.2)
 *     .withSpeed(() -> 0.1);
 * new ParticleWrapper(() -> Particle.BLOCK, () -> 25, () -> 0.1, () -> 0.1, () -> 0.1)
 *     .withBlockData(() -> Material.STONE.createBlockData());
 * }</pre>
 * </p>
 */
public class ParticleWrapper {
    /** Supplier for the Bukkit {@link Particle} type to display. */
    private final Supplier<Particle> particle;

    /** Supplier for the number of particles to spawn per display call. */
    private Supplier<Integer> count = () -> 1;

    /** Supplier for the X-axis offset around the spawn location. */
    private Supplier<Double> xOffset = () -> 0.0;

    /** Supplier for the Y-axis offset around the spawn location. */
    private Supplier<Double> yOffset = () -> 0.0;

    /** Supplier for the Z-axis offset around the spawn location. */
    private Supplier<Double> zOffset = () -> 0.0;

    /** Supplier for the speed parameter for particle motion. Default {@code -1} indicates no speed set. */
    private Supplier<Double> speed = () -> -1.0;

    /** Supplier for additional data used for some particle types (dust transitions). */
    private Supplier<Double> data = () -> 1.0;

    /** Supplier for dust options for colored dust particles, if applicable. */
    private Supplier<Particle.DustOptions> options = () -> null;

    /** Supplier for dust transition options for fading color dust particles, if applicable. */
    private Supplier<Particle.DustTransition> transition = () -> null;

    /** Supplier for block data used with block crack or block dust particle types, if applicable. */
    private Supplier<BlockData> blockData = () -> null;

    /** Supplier for a direct colour applied to {@link Particle#ENTITY_EFFECT}; {@code null} otherwise. */
    private Supplier<Color> entityColor = () -> null;

    /**
     * Constructs a ParticleWrapper with only a particle type supplier and default parameters.
     *
     * @param particle supplier for the particle type to wrap
     */
    public ParticleWrapper(Supplier<Particle> particle) {
        this.particle = particle;
    }

    /**
     * Constructs a ParticleWrapper with particle type, count, and per-axis offset suppliers.
     *
     * @param particle supplier for the particle type
     * @param count supplier for the number of particles to spawn
     * @param xOffset supplier for the X-axis offset around the spawn location
     * @param yOffset supplier for the Y-axis offset around the spawn location
     * @param zOffset supplier for the Z-axis offset around the spawn location
     */
    public ParticleWrapper(Supplier<Particle> particle, Supplier<Integer> count,
            Supplier<Double> xOffset, Supplier<Double> yOffset, Supplier<Double> zOffset) {
        this(particle);
        this.count = count;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }

    /**
     * Sets the speed supplier and returns {@code this} for chaining.
     *
     * @param speed supplier for the speed parameter
     * @return this instance
     */
    public ParticleWrapper withSpeed(Supplier<Double> speed) {
        this.speed = speed;
        return this;
    }

    /**
     * Sets the dust options supplier and returns {@code this} for chaining.
     *
     * @param options supplier for colored dust options
     * @return this instance
     */
    public ParticleWrapper withOptions(Supplier<Particle.DustOptions> options) {
        this.options = options;
        return this;
    }

    /**
     * Sets the dust transition data and transition suppliers and returns {@code this} for chaining.
     *
     * @param data supplier for the extra data value affecting the transition
     * @param transition supplier for dust transition options for fading color effects
     * @return this instance
     */
    public ParticleWrapper withTransition(Supplier<Double> data, Supplier<Particle.DustTransition> transition) {
        this.data = data;
        this.transition = transition;
        return this;
    }

    /**
     * Sets the block data supplier and returns {@code this} for chaining.
     *
     * @param blockData supplier for block data type for particle behavior
     * @return this instance
     */
    public ParticleWrapper withBlockData(Supplier<BlockData> blockData) {
        this.blockData = blockData;
        return this;
    }

    /**
     * Sets the entity colour supplier for {@link Particle#ENTITY_EFFECT} and returns {@code this}.
     *
     * @param entityColor supplier for the colour to pass as extra particle data
     * @return this instance
     */
    public ParticleWrapper withEntityColor(Supplier<Color> entityColor) {
        this.entityColor = entityColor;
        return this;
    }

    // ── Snapshot getters (call suppliers once, used for serialization) ──────

    /** Returns the current particle type by calling the supplier. */
    public Particle getParticle() { return particle.get(); }

    /** Returns the current particle count by calling the supplier. */
    public int getCount() { return count.get(); }

    /** Returns the current X-axis spawn offset by calling the supplier. */
    public double getXOffset() { return xOffset.get(); }

    /** Returns the current Y-axis spawn offset by calling the supplier. */
    public double getYOffset() { return yOffset.get(); }

    /** Returns the current Z-axis spawn offset by calling the supplier. */
    public double getZOffset() { return zOffset.get(); }

    /** Returns the current speed by calling the supplier. */
    public double getSpeed() { return speed.get(); }

    /** Returns the current dust options by calling the supplier, or {@code null} if none. */
    public Particle.DustOptions getDustOptions() { return options.get(); }

    /** Returns the current dust transition by calling the supplier, or {@code null} if none. */
    public Particle.DustTransition getDustTransition() { return transition.get(); }

    /** Returns the current entity colour by calling the supplier, or {@code null} if none. */
    public Color getEntityColor() { return entityColor.get(); }

    /**
     * Displays the particle effect at the specified location in the world.
     * Suppliers are evaluated at display time, enabling runtime-resolved values.
     *
     * @param location location to display the particle effect
     */
    public void display(Location location) {
        World world = location.getWorld();
        Particle p = particle.get();
        int c = count.get();
        double x = xOffset.get();
        double y = yOffset.get();
        double z = zOffset.get();
        double s = speed.get();
        Particle.DustTransition t = transition.get();
        Particle.DustOptions o = options.get();
        BlockData bd = blockData.get();
        Color ec = entityColor.get();

        if (ec != null)
            world.spawnParticle(p, location, c, x, y, z, s < 0 ? 1.0 : s, ec);
        else if (t == null && o == null && bd == null)
            if (s == -1.0)
                world.spawnParticle(p, location, c, x, y, z);
            else
                world.spawnParticle(p, location, c, x, y, z, s);

        else if (t != null && bd == null)
            world.spawnParticle(p, location, c, x, y, z, data.get(), t);

        else if (o instanceof Particle.DustTransition dt && bd == null)
            world.spawnParticle(p, location, c, x, y, z, data.get(), dt);

        else if (o != null && bd == null)
            world.spawnParticle(p, location, c, o);

        else
            world.spawnParticle(p, location, c, x, y, z, bd);
    }

    /**
     * Displays all particle effects in the collection at the given location.
     *
     * @param particles the collection of particle wrappers to display
     * @param location the location at which to display the particles
     */
    public static void displayAll(Collection<ParticleWrapper> particles, Location location) {
        for (ParticleWrapper pw : particles) {
            pw.display(location);
        }
    }
}
