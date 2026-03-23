package btm.sword.system.entity.mob;

/**
 * Holds the resolved DEU animation slots for a mob type.
 * <p>
 * Each slot pairs a full animation tag (e.g. {@code "witha_walk"}) with its duration in ticks.
 * Tags are built at registry load time as {@code prefix_suffix}.
 * An empty-tag slot ({@link AnimationSlot#NONE}) means that state is not registered on the
 * {@link btm.sword.system.entity.display.DisplayRig}'s state machine.
 * </p>
 *
 * @param idle   IDLE looping state
 * @param walk   WALK looping state
 * @param fall   FALLING looping state
 * @param attack MELEE one-shot state (locked until animation completes)
 * @param die    DEATH one-shot state; {@link AnimationSlot#NONE} if no death animation
 */
public record AnimationSlots(
        AnimationSlot idle,
        AnimationSlot walk,
        AnimationSlot fall,
        AnimationSlot attack,
        AnimationSlot die) {

    /** Sentinel used when a mob type has no display rig. */
    public static final AnimationSlots EMPTY = new AnimationSlots(
        AnimationSlot.NONE,
        AnimationSlot.NONE,
        AnimationSlot.NONE,
        AnimationSlot.NONE,
        AnimationSlot.NONE
    );
}
