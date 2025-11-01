package btm.sword.util.display;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

/**
 * Wrapper class for handling Bukkit {@link Particle} effects with additional parameters
 * such as count, offset, speed, and special particle options like dust colors or block data.
 * <p>
 * Provides methods to configure and display particle effects with flexible configurations
 * in various locations and worlds.
 * </p>
 */
public class ParticleWrapper {
    /** The Bukkit {@link Particle} type to display. */
    private final Particle particle;

    /** Number of particles to spawn per display call. */
    private int count = 1;

    /** Offsets for particle spawning around the location on the X axis. */
    private double xOffset = 0;

    /** Offsets for particle spawning around the location on the Y axis. */
    private double yOffset = 0;

    /** Offsets for particle spawning around the location on the Z axis. */
    private double zOffset = 0;

    /** Speed parameter for particle motion effects. Default -1 indicates no speed set. */
    private double speed = -1;

    /** Additional data used for some particle types (default 1). */
    private double data = 1;

    /** Dust options for colored dust particles, if applicable. */
    private Particle.DustOptions options = null;

    /** Dust transition options for fading color dust particles, if applicable. */
    private Particle.DustTransition transition = null;

    /** Block data used with block crack or block dust particle types, if applicable. */
    private BlockData blockData = null;

    /**
     * Constructs a ParticleWrapper with the specified particle type and default parameters.
     *
     * @param particle the particle type to wrap
     */
    public ParticleWrapper(Particle particle) {
        this.particle = particle;
    }

    /**
     * Constructs a ParticleWrapper with particle type and specific count and offsets for each axis.
     *
     * @param particle the particle type
     * @param count number of particles to spawn
     * @param xOffset X-axis offset around the spawn location
     * @param yOffset Y-axis offset around the spawn location
     * @param zOffset Z-axis offset around the spawn location
     */
    public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset) {
        this(particle);
        this.count = count;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }

    /**
     * Constructs a ParticleWrapper with specified particle, count, offsets, and speed.
     *
     * @param particle the particle type
     * @param count number of particles to spawn
     * @param xOffset X offset
     * @param yOffset Y offset
     * @param zOffset Z offset
     * @param speed speed parameter for particle motion
     */
    public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset, double speed) {
        this(particle, count, xOffset, yOffset, zOffset);
        this.speed = speed;
    }

    /**
     * Constructs a ParticleWrapper with colored dust options.
     *
     * @param particle particle type
     * @param count number of particles
     * @param xOffset X offset
     * @param yOffset Y offset
     * @param zOffset Z offset
     * @param options dust color options for particles
     */
    public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset, Particle.DustOptions options) {
        this(particle, count, xOffset, yOffset, zOffset);
        this.options = options;
    }

    /**
     * Constructs a ParticleWrapper with dust transition options.
     *
     * @param particle particle type
     * @param count number of particles
     * @param xOffset X offset
     * @param yOffset Y offset
     * @param zOffset Z offset
     * @param data additional data affecting particles
     * @param transition dust transition options for fading color effects
     */
    public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset, double data, Particle.DustTransition transition) {
        this(particle, count, xOffset, yOffset, zOffset);
        this.data = data;
        this.transition = transition;
    }

    /**
     * Constructs a ParticleWrapper for block crack or dust particle types with block data.
     *
     * @param particle particle type
     * @param count number of particles
     * @param xOffset X offset
     * @param yOffset Y offset
     * @param zOffset Z offset
     * @param blockData block data type for particle behavior
     */
    public ParticleWrapper(Particle particle, int count, double xOffset, double yOffset, double zOffset, BlockData blockData) {
        this(particle, count, xOffset, yOffset, zOffset);
        this.blockData = blockData;
    }

    /**
     * Displays the particle effect at the specified location in the world.
     * Chooses the appropriate Bukkit spawnParticle method based on set optional parameters.
     *
     * @param location location to display the particle effect
     */
    public void display(Location location) {
        World world = location.getWorld();
        if (transition == null && options == null && blockData == null)
            if (speed == -1)
                world.spawnParticle(particle, location, count, xOffset, yOffset, zOffset);
            else
                world.spawnParticle(particle, location, count, xOffset, yOffset, zOffset, speed);

        else if (options == null && blockData == null)
            world.spawnParticle(particle, location, count, xOffset, yOffset, zOffset, data, transition);

        else if (blockData == null)
            world.spawnParticle(particle, location, count, options);

        else
            world.spawnParticle(particle, location, count, xOffset, yOffset, zOffset, blockData);
    }
}
