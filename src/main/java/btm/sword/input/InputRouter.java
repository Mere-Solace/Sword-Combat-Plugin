package btm.sword.input;

import org.bukkit.inventory.ItemStack;

import btm.sword.action.throwing.ThrowAction;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.entity.player.ThrowPhase;
import btm.sword.input.ItemInputBinding.MatchContext;
import btm.sword.input.ItemInputBinding.Phase;
import btm.sword.item.core.ItemClassifier;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.util.entity.InputUtil;

/**
 * Single dispatcher that translates an {@link InputIntent} into player-side effects
 * and an {@link InputDecision} for the listener.
 *
 * <h2>Responsibility</h2>
 * The router owns every gameplay-level routing decision that used to live in
 * {@code InputListener}: typed-item handling, inventory-mode suppression, usable-item
 * gating, interactible-block fall-through, block-availability checks, throw cancellation
 * on hotbar changes, and the final dispatch into {@link SwordPlayer#act(InputType)}.
 *
 * <h2>Layering</h2>
 * <pre>
 *   Bukkit event   --(InputListener)-->   InputIntent
 *   InputIntent    --(InputRouter)--->    SwordPlayer.act + InputDecision
 *   InputDecision  --(InputListener)-->   event.setCancelled
 * </pre>
 *
 * <h2>State ownership</h2>
 * The router is a stateless static dispatcher. All mutable state remains on the player
 * (input tree, gesture tracker, inventory mode flags). The router only reads and
 * writes through public {@code SwordPlayer} APIs.
 *
 * <h2>Adding a new input source</h2>
 * <ol>
 *   <li>Add a new record to {@link InputIntent}.</li>
 *   <li>Add a {@code case} branch in {@link #route(InputIntent, SwordPlayer)} — the
 *       sealed switch makes this a compile-time requirement.</li>
 *   <li>Add the corresponding Bukkit handler in {@link InputListener} that builds the
 *       intent and calls {@code route}.</li>
 * </ol>
 */
public final class InputRouter {

    private InputRouter() {}

    /**
     * Dispatches the given intent for the given player and returns the listener-side
     * cancellation decision.
     */
    public static InputDecision route(InputIntent intent, SwordPlayer player) {
        return switch (intent) {
            case InputIntent.Attack ignored -> handleAttack(player);
            case InputIntent.Interact i -> handleInteract(player, i);
            case InputIntent.InteractEntity ignored -> handleInteractEntity(player);
            case InputIntent.Drop d -> handleDrop(player, d);
            case InputIntent.SneakBegin ignored -> handleSneakBegin(player);
            case InputIntent.SneakEnd ignored -> handleSneakEnd(player);
            case InputIntent.Swap ignored -> handleSwap(player);
            case InputIntent.HotbarChange ignored -> handleHotbarChange(player);
        };
    }

    // ── Per-intent handlers ───────────────────────────────────────────────────

    private static boolean dispatchEarly(SwordPlayer player, InputType type, ItemStack item) {
        return ItemInputDispatchTable.dispatch(new MatchContext(player, type, item), Phase.EARLY);
    }

    private static InputDecision handleAttack(SwordPlayer player) {
        ItemStack item = player.getItemStackInHand(true);
        if (dispatchEarly(player, InputType.LEFT, item)) return InputDecision.PASS;
        player.act(InputType.LEFT);
        return InputDecision.CANCEL;
    }

    private static InputDecision handleInteract(SwordPlayer player, InputIntent.Interact i) {
        if (player.isInInventorySession()) return InputDecision.PASS;
        if (player.hasPerformedDropAction()) return InputDecision.PASS;
        if (player.isDroppingInInv()) return InputDecision.PASS;

        ItemStack item = player.getItemStackInHand(true);
        InputType type = i.side() == InputIntent.Side.LEFT ? InputType.LEFT : InputType.RIGHT;

        if (dispatchEarly(player, type, item)) return InputDecision.CANCEL;

        if (i.side() == InputIntent.Side.RIGHT) {
            if (ItemClassifier.isUsable(item)) return InputDecision.PASS;
            if (player.isAtRoot() && i.block() != null && InputUtil.isInteractible(i.block())) {
                return InputDecision.PASS;
            }
            if (player.isUnableToBlock()) {
                player.displayDisablingEffect();
                return InputDecision.PASS;
            }
        }

        player.act(type);
        return InputDecision.PASS;
    }

    private static InputDecision handleInteractEntity(SwordPlayer player) {
        ItemStack item = player.getItemStackInHand(true);
        if (dispatchEarly(player, InputType.RIGHT, item)) return InputDecision.CANCEL;
        if (ItemClassifier.isUsable(item)) return InputDecision.PASS;

        player.setInteractingWithEntity(true);
        SwordScheduler.runConsumerNextTick(sp -> sp.setInteractingWithEntity(false), player);

        if (player.isUnableToBlock()) {
            player.displayDisablingEffect();
            return InputDecision.CANCEL;
        }

        player.act(InputType.RIGHT);
        return InputDecision.CANCEL;
    }

    private static InputDecision handleDrop(SwordPlayer player, InputIntent.Drop d) {
        player.setLastHeldItemBeforeDrop(d.droppedStack());
        if (dispatchEarly(player, InputType.DROP, d.droppedStack())) return InputDecision.CANCEL;
        player.setPerformedDropAction();
        player.act(InputType.DROP);
        return InputDecision.CANCEL;
    }

    private static InputDecision handleSneakBegin(SwordPlayer player) {
        ItemStack item = player.getItemStackInHand(true);
        if (dispatchEarly(player, InputType.SHIFT, item)) return InputDecision.PASS;
        player.act(InputType.SHIFT);
        return InputDecision.PASS;
    }

    private static InputDecision handleSneakEnd(SwordPlayer player) {
        player.endSneaking();
        return InputDecision.PASS;
    }

    private static InputDecision handleSwap(SwordPlayer player) {
        ItemStack item = player.getItemStackInHand(true);
        if (dispatchEarly(player, InputType.SWAP, item)) return InputDecision.CANCEL;
        if (player.isSwappingInInv()) return InputDecision.PASS;
        player.act(InputType.SWAP);
        return InputDecision.CANCEL;
    }

    private static InputDecision handleHotbarChange(SwordPlayer player) {
        player.setChangingHandIndex(true);
        if (player.inputReliantOnItem()) player.resetTree();
        if (player.getThrowPhase() == ThrowPhase.THROWING) ThrowAction.throwCancel(player);
        SwordScheduler.runConsumerNextTick(sp -> sp.setChangingHandIndex(false), player);
        return InputDecision.PASS;
    }
}
