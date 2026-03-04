package btm.sword.system.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import btm.sword.system.action.constraint.ActionConstraint;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;
import lombok.Getter;

/**
 * Represents an executable action triggered by player input within the Sword plugin system.
 * Encapsulates the logic for execution, cooldown handling, ability casting checks,
 * and optional display of cooldown or disabled states to the player.
 * <p>
 * Uses {@link Combatant} as the executor and may display messages via {@link SwordPlayer} methods.
 * {@link ActionConstraint}s provide composable precondition checks that replace scattered
 * {@code canPerform*()} calls.
 * </p>
 */
public class InputAction {
    /** The action to perform when executed, defined as a consumer of a Combatant. */
    private final Consumer<Combatant> action;

    /** Function to calculate the cooldown duration in milliseconds for this action, based on the executor. */
    private final Function<Combatant, Integer> cooldownCalculation;

    /** Predicate that tests whether the executor is allowed to cast this ability at a given time. */
    private final Predicate<Combatant> canCastAbility;

    /** Composable constraints evaluated before execution; all must pass for the action to fire.
     * -- GETTER --
     *  Returns the constraints attached to this action. Constraints are evaluated in
     *
     *  and in readiness display checks.
     *
     * @return unmodifiable list of constraints
     */
    @Getter
    private final List<ActionConstraint> constraints;

    private final Function<Combatant, Float> requiredSoulfire;

    /** Convenience accessor to compute required soulfire for the provided executor. */
    public float getRequiredSoulfire(Combatant executor) {
        return Math.max(0f, requiredSoulfire.apply(executor));
    }

    /** Whether to display the remaining cooldown time to the player if action is on cooldown. */
    private final boolean displayCooldown;

    /** Whether to display a "disabled" effect/message if the action cannot be cast. */
    private final boolean displayDisabled;

    private final boolean resetIfCannotPerform;
    private final Function<Combatant, Integer> castDuration; // function to compute cast duration per executor

    /** Timestamp in milliseconds when this action was last successfully executed. */
    @lombok.Getter
    private long timeLastExecuted = 0;

    /**
     * Convenience accessor to compute cast duration for the provided executor.
     */
    public int getCastDurationMillis(Combatant executor) {
        return Math.max(0, castDuration.apply(executor));
    }

    /**
     * Constructs an InputAction.
     *
     * @param action the action to execute on {@link Combatant}
     * @param cooldownCalculation function that computes cooldown based on executor; returns milliseconds
     * @param canCastAbility predicate to test if executor can cast the action
     * @param constraints composable preconditions that must all pass before execution
     * @param requiredSoulfire function returning the soulfire cost for the executor
     * @param displayCooldown whether to visually show cooldown progress on failure
     * @param displayDisabled whether to visually indicate the ability is disabled on failure
     * @param resetIfCannotPerform whether to reset the sequence if the action cannot be performed
     * @param castDuration the function that calculates the duration (ms) to block other abilities while casting
     */
    public InputAction(
            Consumer<Combatant> action,
            Function<Combatant, Integer> cooldownCalculation,
            Predicate<Combatant> canCastAbility,
            List<ActionConstraint> constraints,
            Function<Combatant, Float> requiredSoulfire,
            boolean displayCooldown,
            boolean displayDisabled,
            boolean resetIfCannotPerform,
            Function<Combatant, Integer> castDuration) {
        this.action = action;
        this.cooldownCalculation = cooldownCalculation;
        this.canCastAbility = canCastAbility;
        this.constraints = constraints != null ? List.copyOf(constraints) : List.of();
        this.requiredSoulfire = requiredSoulfire != null ? requiredSoulfire : c -> 0f;
        this.displayCooldown = displayCooldown;
        this.displayDisabled = displayDisabled;
        this.resetIfCannotPerform = resetIfCannotPerform;
        this.castDuration = castDuration != null ? castDuration : c -> 0;
    }

    /**
     * Checks whether the action is ready to execute (cooldown and ability checks).
     * This does not actually run the action; use {@link #perform(Combatant)} to execute.
     *
     * @param executor the Combatant attempting to execute the action
     * @return whether the action can be performed or not
     */
    public boolean handlePerformAttempt(Combatant executor) {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - getTimeLastExecuted();
        long cooldown = calcCooldown(executor);

        if (deltaTime <= cooldown) {
            if (displayCooldown)
                ((SwordPlayer) executor).displayCooldown(Math.max(0, cooldown - (currentTime - getTimeLastExecuted())));
            handleExecutionFailure(executor);
            return false;
        }

        // Check composable constraints — all must pass
        for (ActionConstraint constraint : constraints) {
            if (!constraint.test(executor)) {
                if (displayDisabled) {
                    ((SwordPlayer) executor).displayDisablingEffect();
                }
                if (resetIfCannotPerform) {
                    handleExecutionFailure(executor);
                }
                return false;
            }
        }

        if (unableToCast(executor)) {
            if (resetIfCannotPerform) {
                if (displayDisabled) {
                    ((SwordPlayer) executor).displayDisablingEffect();
                }
                handleExecutionFailure(executor);
                return false;
            }
        }
        handleSoulfireConsumption(executor);
        return true;
    }

    private void handleSoulfireConsumption(Combatant combatant) {
        combatant.consumeSoulfire(requiredSoulfire.apply(combatant)); // begin the depletion of soulfire
    }

    private void handleExecutionFailure(Combatant combatant) {
        if (combatant instanceof SwordPlayer swordPlayer) {
            swordPlayer.resetTree();
        }
    }

    /**
     * Actually performs the action on the executor. Should be invoked once validation
     * passes (i.e., execute(...) returned true) and will set the last-executed time.
     */
    public void perform(Combatant executor) {
        action.accept(executor);
        setTimeLastExecuted();
    }


    /**
     * Calculates the cooldown duration in milliseconds for this action,
     * defaulting to zero if no cooldown function is set.
     *
     * @param executor the Combatant executing the action
     * @return the cooldown duration in milliseconds
     */
    public long calcCooldown(Combatant executor) {
        return cooldownCalculation != null ? cooldownCalculation.apply(executor) : 0;
    }

    /**
     * Tests whether the executor meets the conditions to cast this ability.
     * Defaults to always true if no predicate is provided.
     *
     * @param executor the Combatant attempting to cast
     * @return true if casting is allowed, false otherwise
     */
    public boolean unableToCast(Combatant executor) {
        return canCastAbility != null && !canCastAbility.test(executor);
    }

    /**
     * Sets the timestamp when this action was last executed to the current time.
     */
    public void setTimeLastExecuted() {
        timeLastExecuted = System.currentTimeMillis();
    }

    /**
     * Builder for InputAction to make construction more readable and maintainable.
     */
    public static Builder builder() {
        return new Builder();
    }

    /** */
    public static class Builder {
        private Consumer<Combatant> action;
        private Function<Combatant, Integer> cooldownCalculation;
        private Predicate<Combatant> canCastAbility;
        private final List<ActionConstraint> constraints = new ArrayList<>();
        private Function<Combatant, Float> requiredSoulfire = c -> 0f;
        private boolean displayCooldown = false;
        private boolean displayDisabled = false;
        private boolean resetIfCannotPerform = false;
        private Function<Combatant, Integer> castDuration = c -> 0;

        public Builder action(Consumer<Combatant> action) {
            this.action = action;
            return this;
        }

        public Builder cooldown(Function<Combatant, Integer> cooldownCalculation) {
            this.cooldownCalculation = cooldownCalculation;
            return this;
        }

        public Builder canCast(Predicate<Combatant> canCastAbility) {
            this.canCastAbility = canCastAbility;
            return this;
        }

        /**
         * Adds composable {@link ActionConstraint}s that must all pass before the action can fire.
         * Constraints are evaluated before the legacy {@code canCast} predicate.
         *
         * @param actionConstraints one or more constraints to add
         * @return this builder
         */
        public Builder constraints(ActionConstraint... actionConstraints) {
            this.constraints.addAll(Arrays.asList(actionConstraints));
            return this;
        }

        public Builder requiredSoulfire(Supplier<Float> requiredSoulfire) {
            this.requiredSoulfire = requiredSoulfire != null ? c -> requiredSoulfire.get() : c -> 0f;
            return this;
        }

        public Builder requiredSoulfire(Function<Combatant, Float> requiredSoulfire) {
            this.requiredSoulfire = requiredSoulfire != null ? requiredSoulfire : c -> 0f;
            return this;
        }

        public Builder displayCooldown(boolean displayCooldown) {
            this.displayCooldown = displayCooldown;
            return this;
        }

        public Builder displayDisabled(boolean displayDisabled) {
            this.displayDisabled = displayDisabled;
            return this;
        }

        public Builder resetIfCannotPerform(boolean resetIfCannotPerform) {
            this.resetIfCannotPerform = resetIfCannotPerform;
            return this;
        }

        /** Use a supplier to provide a (possibly dynamic) constant cast duration in milliseconds. The supplier is evaluated at execution time. */
        public Builder castDuration(Supplier<Integer> castDurationSupplier) {
            this.castDuration = castDurationSupplier != null ? c -> castDurationSupplier.get() : c -> 0;
            return this;
        }

        public Builder castDuration(Function<Combatant, Integer> castDurationFunction) {
            this.castDuration = castDurationFunction != null ? castDurationFunction : c -> 0;
            return this;
        }

        public InputAction build() {
            if (action == null) throw new IllegalStateException("InputAction requires an action consumer");
            Function<Combatant, Integer> cooldown = cooldownCalculation != null ? cooldownCalculation : (c -> 0);
            Predicate<Combatant> canCast = canCastAbility != null ? canCastAbility : (c -> true);
            return new InputAction(action, cooldown, canCast, constraints, requiredSoulfire,
                displayCooldown, displayDisabled, resetIfCannotPerform, castDuration);
        }
    }
}
