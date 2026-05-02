package btm.sword.action.skill.container;

/**
 * Persisted per-slot resource state for an equipped ability.
 *
 * <p>{@code -1} for uses/durability means "not tracked" (ability has no STACK/DURABILITY use type).
 * {@code cooldownExpiresAt} is epoch-millis; {@code 0} means no active cooldown.</p>
 *
 * @param remainingUses       remaining stack-use count, or {@code -1} if not tracked
 * @param remainingDurability remaining durability, or {@code -1} if not tracked
 * @param cooldownExpiresAt   epoch-millis when the cooldown expires; {@code 0} if no active cooldown
 */
public record SkillSlotState(int remainingUses, int remainingDurability, long cooldownExpiresAt) {

    /** Sentinel — no tracking active for any dimension. */
    public static final SkillSlotState EMPTY = new SkillSlotState(-1, -1, 0L);
}
