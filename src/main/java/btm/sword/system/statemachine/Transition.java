package btm.sword.system.statemachine;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a single state transition within a finite state machine (FSM).
 * <p>
 * A {@code Transition} defines:
 * <ul>
 *   <li><b>Source state</b> ({@code from}) — the state this transition is valid from.</li>
 *   <li><b>Target state</b> ({@code to}) — the state entered when this transition fires.</li>
 *   <li><b>Condition</b> ({@code condition}) — a predicate evaluated against the machine’s context
 *       that determines whether the transition should occur.</li>
 *   <li><b>Side-effect</b> ({@code onTransition}) — optional code executed when the transition
 *       successfully triggers (e.g., cleanup, animation cues, timers, counters, etc.).</li>
 * </ul>
 *
 * <h2>Type Parameters</h2>
 * <p>
 * {@code <T>} — the type of the <em>context object</em> driving the state machine.
 * This is usually the entity, controller, or domain object whose data determines
 * when transitions should fire.
 *
 * <h2>State Types</h2>
 * The {@code from} and {@code to} parameters accept any subtype of {@link State State&lt;T&gt;}.
 * This allows the FSM to match transitions using {@link Class#isAssignableFrom(Class)}
 * instead of checking exact class equality, enabling:
 * <ul>
 *   <li>hierarchical state patterns,</li>
 *   <li>wildcard “any subclass of X” transitions,</li>
 *   <li>clean separation between <em>state instances</em> and <em>state types</em>.</li>
 * </ul>
 *
 * <h2>How the FSM Uses This Record</h2>
 * <ol>
 *   <li>The FSM checks all transitions whose {@code from} type is assignable from the current state type.</li>
 *   <li>It evaluates each {@code condition} with the current context.</li>
 *   <li>When the first condition returns {@code true}, the transition fires.</li>
 *   <li>{@code onTransition.accept(context)} is invoked.</li>
 *   <li>The FSM constructs a new instance of the {@code to} state and activates it.</li>
 * </ol>
 *
 * <h2>Recommended Patterns</h2>
 * <ul>
 *   <li>Keep states stateless and reusable so they can be created on-demand per transition.</li>
 *   <li>Use descriptive, deterministic conditions — avoid hidden side effects.</li>
 *   <li>Use the {@code onTransition} hook for boundary-event logic only
 *       (e.g., stopping animations, resetting cooldowns).</li>
 * </ul>
 *
 * <h2>Related Reading</h2>
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Finite-state_machine">Finite State Machine (FSM) fundamentals</a></li>
 *   <li><a href="https://gameprogrammingpatterns.com/state.html">Game Programming Patterns — State Pattern</a></li>
 *   <li><a href="https://docs.oracle.com/javase/specs/">Java Language Specification (JLS)</a> — class literals & generics</li>
 *   <li>{@link Class#isAssignableFrom(Class)} — for flexible type-based transitions</li>
 *   <li>{@link Predicate}, {@link Consumer} — functional interfaces used for condition & side-effects</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * stateMachine.addTransition(new Transition<>(
 *     IdleState.class,
 *     AttackState.class,
 *     entity -> entity.attackRequested(),
 *     entity -> entity.resetComboTimer()
 * ));
 * }</pre>
 *
 * @param from        the state type this transition may originate from
 * @param to          the state type to enter when this transition triggers
 * @param condition   predicate determining whether this transition should occur
 * @param onTransition action executed when the transition fires
 * @param <T>         the context type feeding conditions and side-effects
 */
public record Transition<T>(
    Class<? extends State<T>> from,
    Class<? extends State<T>> to,
    Predicate<T> condition,
    Consumer<T> onTransition
) {}
