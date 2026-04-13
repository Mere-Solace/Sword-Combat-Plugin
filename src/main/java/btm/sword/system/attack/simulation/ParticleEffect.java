package btm.sword.system.attack.simulation;

import org.bukkit.Particle;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A single particle burst fired when the attack's normalized time crosses a keyframe's
 * {@code t} value.
 *
 * <p>Particles are spawned at the keyframe's world-space centre offset by
 * {@link #offset()} (also transformed to world space). {@link #spread()} controls the
 * random position spread radius passed to the Bukkit particle API.</p>
 *
 * @param type        Minecraft particle type
 * @param count       number of particles to spawn
 * @param offset      local-space offset from the keyframe's world-centre
 * @param spread      random position spread radius
 * @param dustOptions colour/size options for {@link Particle#DUST} or
 *                    {@link Particle#DUST_COLOR_TRANSITION}; {@code null} otherwise
 */
public record ParticleEffect(
    Particle type,
    int count,
    Vector3f offset,
    float spread,
    @Nullable Particle.DustOptions dustOptions
) {}
