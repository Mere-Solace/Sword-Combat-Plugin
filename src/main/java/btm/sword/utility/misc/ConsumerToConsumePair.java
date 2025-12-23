package btm.sword.utility.misc;

import java.util.function.Consumer;

public record ConsumerToConsumePair<T>(Consumer<T> consumer, T toAccept) {
    public void accept() {
        consumer.accept(toAccept);
    }
}
