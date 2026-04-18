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
 * <p>Retained as a record (not a hierarchy) because the data is flat: particle type, count,
 * Bukkit-level per-particle spread, and optional dust options. Position is handled by the
 * containing {@code ParticleDisplay}; {@link #offset} is kept on the record for legacy
 * keyframe-effect migration but is not consulted at runtime by the new display hierarchy.</p>
 *
 * @param type        Minecraft particle type
 * @param count       number of particles to spawn
 * @param offset      legacy local-space offset from the keyframe's world-centre; preserved for
 *                    migration only — display-level {@code originOffset} supersedes it
 * @param spread      Bukkit per-particle spread radius passed to
 *                    {@link org.bukkit.World#spawnParticle(Particle, org.bukkit.Location, int, double, double, double, double)}
 * @param dustOptions colour/size options for {@link Particle#DUST} or
 *                    {@link Particle#DUST_COLOR_TRANSITION}; {@code null} otherwise
 */
public record ParticleEffect(
    Particle type,
    int count,
    Vector3f offset,
    float spread,
    @Nullable Particle.DustOptions dustOptions
) {

    /**
     * Builds a {@link ParticleWrapper} that reproduces this effect's Bukkit call.
     * Used by the display hierarchy to pass through {@code DrawUtil} shape methods.
     *
     * @return a fresh {@link ParticleWrapper} configured from this effect's fields
     */
    public ParticleWrapper toWrapper() {
        float s = spread;
        ParticleWrapper wrapper = new ParticleWrapper(
            () -> type,
            () -> count,
            () -> (double) s,
            () -> (double) s,
            () -> (double) s);
        if (dustOptions != null) {
            Particle.DustOptions opts = dustOptions;
            wrapper.withOptions(() -> opts);
        }
        return wrapper;
    }

    /**
     * Migrates a legacy keyframe-level {@link ParticleEffect} to a {@link PointDisplay} anchored
     * to its owning keyframe. The legacy {@link #offset} becomes the display's
     * {@code originOffset}; the inner particle spec is stripped of offset so it is not applied
     * twice.
     *
     * @return a {@link PointDisplay} equivalent in behaviour to this legacy effect
     */
    public PointDisplay toPointDisplay() {
        ParticleEffect inner = new ParticleEffect(type, count, new Vector3f(), spread, dustOptions);
        List<ParticleEffect> list = new ArrayList<>();
        list.add(inner);
        return new PointDisplay(OriginAnchor.owning(), new Vector3f(offset), new Vector3f(),
            1, 0, list);
    }
}
