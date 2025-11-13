package btm.sword.system.entity.umbral.statemachine;

import btm.sword.system.entity.umbral.UmbralBlade;

import java.util.function.Consumer;
import java.util.function.Predicate;

public record StateTransition(
    UmbralState from,
    UmbralState to,
    Predicate<UmbralBlade> canTransition,
    Consumer<UmbralBlade> onTransition
) { }
