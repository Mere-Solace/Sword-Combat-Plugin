package btm.sword.scene.animation;

/**
 * Animation IDs for {@link FakePlayerManager#animateFake}.
 * Maps to the raw protocol animation byte in {@code ClientboundAnimatePacket}.
 */
public enum FakeAnimation {

    /** Swing the entity's main hand. */
    SWING_MAIN_HAND(0),

    /** Play the hurt/take-damage flinch. */
    TAKE_DAMAGE(1),

    /** Swing the entity's offhand. */
    SWING_OFF_HAND(3);

    private final int id;

    FakeAnimation(int id) {
        this.id = id;
    }

    /**
     * Returns the raw protocol animation ID sent in the ANIMATION packet.
     *
     * @return protocol animation byte value
     */
    public int getId() {
        return id;
    }
}
