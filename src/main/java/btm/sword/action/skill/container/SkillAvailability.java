package btm.sword.action.skill.container;

/**
 * Tracks whether a discovered skill can be equipped by the player.
 *
 * <p>A skill enters a player's discovered pool the first time they encounter it.
 * Once discovered, its availability state governs whether it can be placed in a
 * {@link SkillSlot}:
 * <ul>
 *   <li>{@link #AVAILABLE} — discovered and equippable.</li>
 *   <li>{@link #DEPLETED} — all uses have been spent; visible in the menu but
 *       cannot be re-equipped until restored.</li>
 *   <li>{@link #RELINQUISHED} — a quest item that was given away permanently;
 *       visible as a record but permanently locked.</li>
 * </ul>
 */
public enum SkillAvailability {
    AVAILABLE,
    DEPLETED,
    RELINQUISHED
}
