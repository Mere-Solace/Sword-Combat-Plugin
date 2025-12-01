package btm.sword.utility;

public final class SwordTimeUnit {
    public static final int TICKS_PER_SECOND = 20;
    public static final long MILLISECONDS_PER_TICK = 50L;

    // Primary conversions - milliseconds as base unit
    public static int millisToTicks(long millis) {
        return (int) Math.round(millis / (double) MILLISECONDS_PER_TICK);
    }

    public static long ticksToMillis(int ticks) {
        return ticks * MILLISECONDS_PER_TICK;
    }

    // Secondary conversions for legacy compatibility
    public static long secondsToMillis(double seconds) {
        return (long) (seconds * 1000);
    }

    public static double millisToSeconds(long millis) {
        return millis / 1000.0;
    }

    // Direct tick/second for config migration
    public static int secondsToTicks(double seconds) {
        return (int) (seconds * TICKS_PER_SECOND);
    }
}
