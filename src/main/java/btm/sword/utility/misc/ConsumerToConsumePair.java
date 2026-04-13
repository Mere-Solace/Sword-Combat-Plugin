package btm.sword.utility.misc;

import java.util.function.Consumer;

/** Pairs a {@link Consumer} with a pre-bound argument so the pair can be stored and invoked later without parameters. */
public record ConsumerToConsumePair<T>(Consumer<T> consumer, T toAccept) {
    /** Invokes the consumer with the bound argument. */
    public void accept() {
        consumer.accept(toAccept);
    }
}
