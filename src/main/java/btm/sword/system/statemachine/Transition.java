package btm.sword.system.statemachine;

import java.util.function.Consumer;
import java.util.function.Predicate;

public record Transition<T>(
    Class<? extends State<T>> from,
    Class<? extends State<T>> to,
    Predicate<T> condition,
    Consumer<T> onTransition
) {}
