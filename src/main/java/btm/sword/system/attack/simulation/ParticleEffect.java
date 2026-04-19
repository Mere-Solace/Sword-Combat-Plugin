package btm.sword.system.attack.simulation;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Particle;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import btm.sword.system.attack.visuals.OriginAnchor;
import btm.sword.system.attack.visuals.PointDisplay;
import btm.sword.utility.display.ParticleWrapper;

/**
 * Specification for a single particle burst emitted by a
 * {@link btm.sword.system.attack.visuals.ParticleDisplay}.
 *
 * <p>Three distinct offsets participate in particle placement and must not be
 * confused:</p>
 * <ol>
 *   <li><b>Origin offset</b> — a fixed local-space displacement from the
 *       display's resolved anchor. Lives on {@link btm.sword.system.attack.visuals.ParticleDisplay#getOriginOffset()}.</li>
 *   <li><b>Random offset range</b> — per-emission jitter applied on top of
 *       the origin to pick the actual spawn point for this emission. Lives
 *       on {@link btm.sword.system.attack.visuals.ParticleDisplay#getRandomOffsetRange()}.</li>
 *   <li><b>Spawn offset</b> — per-particle Bukkit {@code xOffset/yOffset/zOffset}
 *       used by {@link org.bukkit.World#spawnParticle} to cluster individual
 *       particles around the chosen spawn point. This is {@link #spreadOffset()}.</li>
 * </ol>
 *
 * @param type         Minecraft particle type
 * @param count        number of particles to spawn
 * @param spreadOffset per-particle Bukkit spawn offset (x/y/z) — see class
 *                     javadoc
 * @param speed        particle speed argument; when {@code < 0} the wrapper
 *                     omits the speed parameter from the Bukkit call
 * @param dustOptions  colour/size options for {@link Particle#DUST} or
 *                     {@link Particle#DUST_COLOR_TRANSITION}; {@code null} otherwise
 */
public record ParticleEffect(
    Particle type,
    int count,
    Vector3f spreadOffset,
    double speed,
    @Nullable Particle.DustOptions dustOptions
) {

    /**
     * Builds a {@link ParticleWrapper} that reproduces this effect's Bukkit call.
     * Used by the display hierarchy to pass through {@code DrawUtil} shape methods.
     *
     * @return a fresh {@link ParticleWrapper} configured from this effect's fields
     */
    public ParticleWrapper toWrapper() {
        Vector3f off = spreadOffset;
        ParticleWrapper wrapper = new ParticleWrapper(
            () -> type,
            () -> count,
            () -> (double) off.x,
            () -> (double) off.y,
            () -> (double) off.z);
        if (speed >= 0.0) {
            double s = speed;
            wrapper.withSpeed(() -> s);
        }
        if (dustOptions instanceof Particle.DustTransition dt) {
            wrapper.withTransition(() -> 1.0, () -> dt);
        } else if (dustOptions != null) {
            Particle.DustOptions opts = dustOptions;
            wrapper.withOptions(() -> opts);
        }
        return wrapper;
    }

    /**
     * Wraps this effect in a {@link PointDisplay} anchored to its owning keyframe,
     * with an optional origin offset applied at the display level.
     *
     * <p>Used by the YAML loader when it encounters a pre-hierarchy keyframe that
     * stored particles flat under {@code particles:} with an inline {@code offset}
     * field. That field was always the display's origin offset — it just lived on
     * the wrong object — so the loader lifts it into the new display's
     * {@link btm.sword.system.attack.visuals.ParticleDisplay#getOriginOffset()}.
     * The inner particle spec keeps its {@link #spreadOffset} and {@link #speed}
     * untouched.</p>
     *
     * @param originOffset fixed local-space offset to apply at the display level;
     *                     {@code null} uses zero
     * @return a {@link PointDisplay} wrapping this effect
     */
    public PointDisplay toPointDisplay(@Nullable Vector3f originOffset) {
        List<ParticleEffect> list = new ArrayList<>();
        list.add(this);
        Vector3f origin = originOffset != null ? new Vector3f(originOffset) : new Vector3f();
        return new PointDisplay(OriginAnchor.owning(), origin, new Vector3f(), 1, 0, list);
    }

    /**
     * Snapshots a {@link ParticleWrapper}'s current supplier values into a {@link ParticleEffect}.
     * Config-driven suppliers are evaluated once at call time.
     *
     * @param w the wrapper to convert
     * @return a new {@link ParticleEffect} capturing the wrapper's current values
     */
    public static ParticleEffect fromWrapper(ParticleWrapper w) {
        Particle.DustOptions dust = w.getDustTransition() != null ? w.getDustTransition() : w.getDustOptions();
        return new ParticleEffect(
            w.getParticle(),
            w.getCount(),
            new Vector3f((float) w.getXOffset(), (float) w.getYOffset(), (float) w.getZOffset()),
            w.getSpeed(),
            dust);
    }
}
