package btm.sword.system.control;

import java.util.function.Supplier;

public record PredicateRunnablePair(Supplier<Boolean> predicate, Runnable runnable) {
    public boolean testAndAccept() {
        if (predicate.get()) {
            if (runnable != null) runnable.run();
            return true;
        }
        return false;
    }
}
