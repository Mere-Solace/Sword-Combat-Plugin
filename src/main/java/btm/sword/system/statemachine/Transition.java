package btm.sword.system.statemachine;

import java.util.function.Predicate;

public record Transition<T>(
    Class<? extends State<T>> target,
    Predicate<T> condition
) {}
