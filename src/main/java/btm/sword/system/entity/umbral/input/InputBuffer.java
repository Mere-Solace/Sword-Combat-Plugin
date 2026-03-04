package btm.sword.system.entity.umbral.input;

import java.util.ArrayDeque;
import java.util.Deque;

public class InputBuffer {
    private static final long DEFAULT_TIMEOUT_MS = 70L; // 2 ticks before input is invalid; 1/10th of a second

    private final Deque<TimestampedInput> queue = new ArrayDeque<>();

    public record TimestampedInput(BladeRequest request, long timestampMs) {}

    public void push(BladeRequest request) {
        long now = System.currentTimeMillis();
        queue.addLast(new TimestampedInput(request, now));
    }

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
