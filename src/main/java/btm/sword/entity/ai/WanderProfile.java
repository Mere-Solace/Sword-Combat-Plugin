package btm.sword.entity.ai;

/**
 * Defines the idle wander behavior for a {@link btm.sword.entity.mob.Hostile}.
 *
 * <p>Each profile controls how far from the mob's spawn origin it wanders, how long it
 * waits between walks, and how long it spends in the look-around phase. Assign a profile
 * to a {@code Hostile} at construction time to produce different behavioral archetypes.
 *
 * <ul>
 *   <li>{@link #SENTINEL} — guards a fixed point; barely moves, long idle periods.
 *   <li>{@link #ROAMER} — explores freely over a wide area with short idle pauses.
 *   <li>{@link #GRAZER} — stays close to origin; mostly idle with occasional short walks.
 * </ul>
 */
public enum WanderProfile {

    /**
     * Stays within 2 blocks of its origin. Long idle and look periods; almost never walks.
     * Suitable for stationary guards posted at a specific location.
     */
    SENTINEL(2.0, 100, 160, 30, 60),

    /**
     * Explores up to 14 blocks from its origin. Short idle pauses and brisk walks.
     * Suitable for patrolling enemies or wildlife that actively move around.
     */
    ROAMER(14.0, 40, 80, 15, 30),

    /**
     * Stays within 5 blocks of its origin. Long idle periods with brief, slow ambles.
     * Suitable for passive animals, NPCs that loiter, or ambient creatures.
     */
    GRAZER(5.0, 80, 140, 30, 60);

    /** Maximum distance in blocks the mob will walk from its spawn origin. */
    public final double wanderRadius;

    /** Minimum ticks the mob stands still before looking around (at 20 TPS). */
    public final int minIdleTicks;

    /** Maximum ticks the mob stands still before looking around (at 20 TPS). */
    public final int maxIdleTicks;

    /** Minimum ticks spent in the look-around phase before choosing a walk target. */
    public final int minLookTicks;

    /** Maximum ticks spent in the look-around phase before choosing a walk target. */
    public final int maxLookTicks;

    WanderProfile(
        double wanderRadius,
        int minIdleTicks,
        int maxIdleTicks,
        int minLookTicks,
        int maxLookTicks
    ) {
        this.wanderRadius = wanderRadius;
        this.minIdleTicks = minIdleTicks;
        this.maxIdleTicks = maxIdleTicks;
        this.minLookTicks = minLookTicks;
        this.maxLookTicks = maxLookTicks;
    }
}
