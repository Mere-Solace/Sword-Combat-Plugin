package btm.sword.system.action.utility.thrown;

import java.util.function.Consumer;

import org.bukkit.Color;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;

import btm.sword.Sword;
import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.types.SwordPlayer;

/**
 * Handles the sequence of actions involved in a {@code Combatant} performing a throw action.
 * <p>
 * A throw in this context is a multiphase process composed of:
 * <ul>
 *     <li><b>Preparation</b> – The entity begins aiming or charging a throw.</li>
 *     <li><b>Cancellation</b> – The throw is interrupted before release.</li>
 *     <li><b>Release</b> – The item is actually thrown.</li>
 * </ul>
 * <p>
 * This class manages the visual representation of the thrown item, its logical state
 * (e.g. tracking success or cancellation), and synchronization with the player's held items.
 * <p>
 * Each method in this class operates on a {@link Combatant} (either a player or AI-controlled entity)
 * and interacts with a {@link ThrownItem} object to handle the in-world physics and display logic.
 *
 * @see SwordAction
 * @see ThrownItem
 * @see Combatant
 * @see SwordPlayer
 */
public class ThrowAction extends SwordAction {
    /**
     * Initializes the throw sequence for the given executor.
     * <p>
     * This method marks the executor as attempting a throw, spawns an
     * {@link ItemDisplay} at the entity's eye location to represent the thrown item,
     * and creates a corresponding {@link ThrownItem} wrapper for simulation control.
     * <p>
     * If the executor is a {@link SwordPlayer}, the thrown item's display will mirror
     * the item currently in hand (based on the item snapshot taken at hold time).
     *
     * @param executor The combatant beginning a throw action.
     */
    public static void throwReady(Combatant executor) {
        // Guard against throwing the Umbral Items
        // TODO: #122 - Add logic here for lunging/directing/hurling the umbral blade
        if (executor instanceof SwordPlayer swordPlayer && swordPlayer.isUmbralItem(swordPlayer.getMainItemStackAtTimeOfHold())) {
            swordPlayer.resetTree();
            swordPlayer.displayMistake();
            return;
        }

        executor.setAttemptingThrow(true);
        executor.setThrowCancelled(false);
        executor.setThrowSuccessful(false);

        Consumer<ItemDisplay> setupInstructions;
        ThrownItem thrownItem;
        if (executor instanceof SwordPlayer sp && !sp.getItemStackInHand(true).isEmpty()) {
            setupInstructions = display -> {
                display.setItemStack(sp.getMainItemStackAtTimeOfHold());
                display.setGlowing(true);
                display.setGlowColorOverride(Color.fromRGB(255, 0, 15));
            };
        }
        else {
            setupInstructions = display -> {
                display.setItemStack(executor.getItemStackInHand(true));
                display.setGlowing(true);
                display.setGlowColorOverride(Color.fromRGB(255, 0, 15));
            };
        }

        thrownItem = new ThrownItem(executor, setupInstructions, 0);
        executor.setThrownItem(thrownItem);

        new BukkitRunnable() {
            int misses = 0;
            @Override
            public void run() {
                if (misses > 20) {
                    cancel();
                }
                if (thrownItem.getDisplay() == null) {
                    misses++;
                } else {
                    thrownItem.onReady();
                    cancel();
                }
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 3L);
    }

    /**
     * Cancels a throw action before it is released.
     * <p>
     * This restores the executor’s held item states to what they were prior to
     * initiating the throw, clears the temporary {@link ThrownItem} reference,
     * and marks the throw as canceled in the combatant’s state.
     * <p>
     * This method is typically called when the player releases the throw input too early,
     * switches items, or is interrupted by another action.
     *
     * @param executor The combatant whose throw action is being canceled.
     */
    public static void throwCancel(Combatant executor) {
        Sword.getInstance().getLogger().info("\nThrow was <CANCELED>\n");
        executor.setAttemptingThrow(false);
        executor.setThrowCancelled(true);
        executor.setThrowSuccessful(false);

        if (executor instanceof SwordPlayer sp) {
            sp.setItemAtIndex(sp.getMainHandItemStackDuringThrow(), sp.getThrownItemIndex());
        }
        else {
            executor.setItemStackInHand(executor.getMainHandItemStackDuringThrow(), true);
        }
        executor.setItemStackInHand(executor.getOffHandItemStackDuringThrow(), false);

        executor.setThrownItem(null);
    }

    /**
     * Executes the final release of a throw after successful preparation.
     * <p>
     * This marks the throw as successful, then schedules the {@link ThrownItem}
     * to be released after a short delay. The delay allows synchronization with
     * client-side animations or wind-up frames before the item actually leaves the hand.
     * <p>
     * Once released, the item’s {@link ThrownItem#onRelease(double)} method is invoked,
     * which handles projectile motion, collision detection, and effects.
     *
     * @param executor The combatant performing the throw.
     */
    public static void throwItem(Combatant executor) {
        if (executor.isThrowCancelled()) return;

        executor.setAttemptingThrow(false);
        executor.setThrowSuccessful(true);

        cast(executor, 10, new BukkitRunnable() {
            @Override
            public void run() {
                executor.getThrownItem().onRelease(2);
            }
        });
    }
}
