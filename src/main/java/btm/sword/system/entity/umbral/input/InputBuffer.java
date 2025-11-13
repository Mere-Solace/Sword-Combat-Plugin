package btm.sword.system.entity.umbral.input;

import java.util.ArrayDeque;
import java.util.Deque;

public class InputBuffer {
    private static final long DEFAULT_TIMEOUT_MS = 60L;

    private final Deque<TimestampedInput> queue = new ArrayDeque<>();

    public record TimestampedInput(BladeRequest request, long timestampMs) {}

    public void push(BladeRequest request) {
        long now = System.currentTimeMillis();
        queue.addLast(new TimestampedInput(request, now));
    }

    public boolean consumeIfPresent(BladeRequest request) {
        long now = System.currentTimeMillis();
        while (!queue.isEmpty()) {
            var head = queue.peekFirst();
            if (now - head.timestampMs > DEFAULT_TIMEOUT_MS) {
                queue.pollFirst();
                continue;
            }
            if (head.request == request) {
                queue.pollFirst();
                return true;
            }
            return false;
        }
        return false;
    }
}
