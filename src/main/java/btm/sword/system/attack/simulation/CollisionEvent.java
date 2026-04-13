package btm.sword.system.attack.simulation;

import java.util.UUID;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import btm.sword.system.attack.HitValuePacket;
import btm.sword.system.entity.base.SwordEntity;

/**
 * Immutable record of a single collision detected by the off-thread {@code VolumeSimulation}.
 * <p>
 * Produced on the simulation thread and enqueued into {@link CollisionEventBridge} for
 * delivery to the main thread each tick.
 * </p>
 *
 * @param attackerUuid      UUID of the entity whose attack volume caused the hit
 * @param victimUuid        UUID of the entity that was struck
 * @param contactPoint      world-space point of contact between the volume and the victim AABB
 * @param hitValue          damage values to apply on the main thread
 * @param knockbackFunction optional function evaluated on the main thread to produce the
 *                          knockback velocity; receives the contact point and the attacking
 *                          entity; {@code null} means no knockback
 */
public record CollisionEvent(
    UUID attackerUuid,
    UUID victimUuid,
    Vector3f contactPoint,
    HitValuePacket hitValue,
    @Nullable BiFunction<Vector3f, SwordEntity, Vector3f> knockbackFunction
) {}
