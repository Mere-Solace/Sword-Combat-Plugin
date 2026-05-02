package btm.sword.control;

import java.util.function.Supplier;

/** Pairs a boolean condition supplier with a runnable that executes when the condition is true. */
public record PredicateRunnablePair(Supplier<Boolean> predicate, Runnable runnable) {
    /** Runs the runnable if the predicate returns {@code true}, and reports whether it fired. */
    public boolean testAndAccept() {
        if (predicate.get()) {
            if (runnable != null) runnable.run();
            return true;
        }
        return false;
    }
}
