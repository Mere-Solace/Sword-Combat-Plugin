package btm.sword.system.attack.visuals;

import java.util.UUID;

import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import btm.sword.system.attack.simulation.KeyframedSequence;

/**
 * Context passed to {@link ParticleDisplay#render} during effect dispatch.
 *
 * <p>The {@link EffectsContext#owningKeyframeIndex} refers to the keyframe whose effect
 * bundle is currently firing — a display with {@link OriginAnchor.OwningKeyframe} resolves
 * against this index.</p>
 *
 * @param worldTransform       local-to-world transform of the attack at this tick
 * @param world                world in which to spawn visuals
 * @param attackerId           attacker entity UUID (resolve via SwordEntityArbiter for body points)
 * @param trajectory           the keyframed trajectory — source of keyframe positions by index
 * @param lockedOrigin         origin captured when the attack was fired; {@code null} if unlocked
 * @param owningKeyframeIndex  zero-based index of the keyframe this display belongs to
 */
public record EffectsContext(
    Matrix4f worldTransform,
    World world,
    UUID attackerId,
    KeyframedSequence trajectory,
    @Nullable Vector3f lockedOrigin,
    int owningKeyframeIndex
) {}
