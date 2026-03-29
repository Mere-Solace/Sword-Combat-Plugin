package btm.sword.utility;

/**
 * Conversion utilities for the three time units used throughout Sword: milliseconds, ticks, and seconds.
 *
 * <p>Milliseconds are the canonical base unit. All config values that express durations use
 * milliseconds; ticks are the Bukkit scheduler unit (20 per second, 50 ms each).</p>
 */
public final class SwordTimeUnit {

    private SwordTimeUnit() {}

    /** Bukkit server ticks per second (20). */
    public static final int TICKS_PER_SECOND = 20;

    /** Duration of one server tick in milliseconds (50). */
    public static final int MILLISECONDS_PER_TICK = 50;

    /**
     * Converts milliseconds to the nearest whole number of ticks.
     *
     * @param millis duration in milliseconds
     * @return equivalent number of ticks (rounded)
     */
    public static int millisToTicks(long millis) {
        return (int) Math.round(millis / (double) MILLISECONDS_PER_TICK);
    }

    /**
     * Converts ticks to milliseconds.
     *
     * @param ticks number of server ticks
     * @return equivalent duration in milliseconds
     */
    public static int ticksToMillis(int ticks) {
        return ticks * MILLISECONDS_PER_TICK;
    }

    /**
     * Converts seconds to milliseconds.
     *
     * @param seconds duration in seconds (fractional values accepted)
     * @return equivalent duration in milliseconds
     */
    public static long secondsToMillis(double seconds) {
        return (long) (seconds * 1000);
    }

    /**
     * Converts milliseconds to seconds.
     *
     * @param millis duration in milliseconds
     * @return equivalent duration in seconds
     */
    public static double millisToSeconds(long millis) {
        return millis / 1000.0;
    }

    /**
     * Converts seconds to the nearest whole number of ticks.
     *
     * @param seconds duration in seconds
     * @return equivalent number of ticks (truncated)
     */
    public static int secondsToTicks(double seconds) {
        return (int) (seconds * TICKS_PER_SECOND);
    }
}
