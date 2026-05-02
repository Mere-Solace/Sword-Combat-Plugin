package btm.sword.action.skill.type.impl.charge;

import java.util.Set;

import org.bukkit.entity.ItemDisplay;

import btm.sword.action.skill.AbilityType;
import btm.sword.action.skill.AbilityUseType;
import btm.sword.action.skill.type.ActivatableAbility;

/**
 * An ability that charges while the player holds right-click, then fires on release.
 *
 * <p>The charge spawns an {@link ItemDisplay} in front of the player. Each tick,
 * {@link #onChargeTick(ChargeSession, long)} is called so the ability can apply its own
 * visual effects (scaling, rotation, particles, etc.). The player may release once
 * {@link ChargeSession#canRelease()} returns {@code true}.</p>
 *
 * <p>Chargeable abilities use the input execution tree ({@code RIGHT} to start charging,
 * {@code RIGHT_HOLD} to release) rather than the simple left-click path used by
 * {@link ActivatableAbility}.</p>
 */
public abstract class ChargeableAbility extends ActivatableAbility {

    @Override
    public AbilityType abilityType() {
        return AbilityType.CHARGEABLE;
    }

    /** Chargeable abilities are always activated via hold, not tap. */
    @Override
    public boolean requiresHold() {
        return true;
    }

    @Override
    public boolean consumesOnUse() {
        return true;
    }

    /** Chargeable abilities default to stack-based consumption. */
    @Override
    public Set<AbilityUseType> useTypes() {
        return Set.of(AbilityUseType.STACK, AbilityUseType.COOLDOWN);
    }

    /**
     * Called every tick while the charge is active.
     * Implementations apply visual effects (scale, rotation, particles, etc.)
     * to the session's display entity.
     *
     * @param session   the active charge session
     * @param elapsedMs milliseconds since the charge started
     */
    public abstract void onChargeTick(ChargeSession session, long elapsedMs);

    /**
     * Whether the charge can be released at this moment. Called when the player
     * releases right-click. If {@code false}, the charge is cancelled instead.
     *
     * @param session the active charge session
     * @return {@code true} if the charge is ready to fire
     */
    public abstract boolean canRelease(ChargeSession session);

    /**
     * The velocity magnitude of the projectile when released.
     *
     * @return projectile speed
     */
    public abstract double projectileVelocity();

    /**
     * The display scale to use for the thrown projectile.
     * Defaults to {@code 1.0f}; override to use the charge session's tracked scale.
     *
     * @param session the charge session at time of release
     * @return the scale for the thrown ThrownItem display
     */
    public float releaseDisplayScale(ChargeSession session) {
        return 1.0f;
    }
}
