package btm.sword.utility;

public final class SwordTimeUnit {
    public static final int TICKS_PER_SECOND = 20;
    public static final int MILLISECONDS_PER_TICK = 50;

    // Primary conversions - milliseconds as base unit
    public static int millisToTicks(long millis) {
        return (int) Math.round(millis / (double) MILLISECONDS_PER_TICK);
    }

    public static int ticksToMillis(int ticks) {
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
