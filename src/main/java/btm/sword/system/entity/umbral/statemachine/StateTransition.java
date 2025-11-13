package btm.sword.system.entity.umbral.statemachine;

import java.util.function.Consumer;
import java.util.function.Predicate;

import btm.sword.system.entity.umbral.UmbralBlade;

public record StateTransition(
    UmbralState from,
    UmbralState to,
    Predicate<UmbralBlade> canTransition,
    Consumer<UmbralBlade> onTransition
) { }
