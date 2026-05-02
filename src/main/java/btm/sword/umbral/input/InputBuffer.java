package btm.sword.umbral.input;

import java.util.ArrayDeque;
import java.util.Deque;

/** A FIFO queue of {@link BladeRequest} values that expire after 70 ms if not consumed. */
public class InputBuffer {
    private static final long DEFAULT_TIMEOUT_MS = 70L; // 2 ticks before input is invalid; 1/10th of a second

    private final Deque<TimestampedInput> queue = new ArrayDeque<>();

    /** Associates a {@link BladeRequest} with the wall-clock time it was enqueued. */
    public record TimestampedInput(BladeRequest request, long timestampMs) {}

    /** Appends the given request to the back of the buffer, timestamped with the current time. */
    public void push(BladeRequest request) {
        long now = System.currentTimeMillis();
        queue.addLast(new TimestampedInput(request, now));
    }

    /** Scans the buffer for the given request, removes expired entries, and returns {@code true} if found and consumed. */
    public boolean consumeIfPresent(BladeRequest request) {
        long now = System.currentTimeMillis();
        queue.removeIf(entry -> now - entry.timestampMs() > DEFAULT_TIMEOUT_MS);
        var it = queue.iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.request() == request) {
                it.remove();
                return true;
            }
        }
        return false;
    }
}
