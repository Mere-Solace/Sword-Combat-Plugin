package btm.sword.gamemode.ctf;

import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import btm.sword.config.Config;
import btm.sword.control.SwordScheduler;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.utility.Prefab;
import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * Represents a single CTF flag with a three-state lifecycle.
 *
 * <h3>States</h3>
 * <ul>
 *   <li><b>IDLE</b> — an invisible {@link ArmorStand} wearing the team banner sits at the flag
 *       spawn location. Any nearby enemy player can pick it up.</li>
 *   <li><b>CARRIED</b> — the ArmorStand is removed, the banner is placed on the carrier's
 *       helmet slot, and a {@link Prefab.PotionEffects#FLAG_CARRIER_SLOW} effect is applied.</li>
 *   <li><b>DROPPED</b> — the carrier lost the flag (died or match intervention). A banner
 *       {@link Item} entity lies on the ground. After {@link Config.Ctf#FLAG_RETURN_TIMER_SECONDS}
 *       seconds the flag auto-returns to base.</li>
 * </ul>
 *
 * <p>The owning {@link btm.sword.gamemode.type.CaptureTheFlag1v1} instance drives all transitions
 * by calling the appropriate methods. No state is self-managed except the auto-return timer.</p>
 */
public class FlagEntity {

    /** The three observable states of this flag. */
    public enum State {
        /** Flag is at its base spawn, represented by an ArmorStand. */
        IDLE,
        /** Flag is being carried by a player (banner on their helmet). */
        CARRIED,
        /** Flag was dropped; a ground item exists and the auto-return timer is running. */
        DROPPED
    }

    /**
     * -- GETTER --
     *  Returns the team that owns this flag.
     */
    @Getter
    private final CtfTeam team;
    /**
     * -- GETTER --
     *  Returns the current state of this flag.
     */
    @Getter
    private State state = State.IDLE;

    private ArmorStand flagStand;
    /**
     * -- GETTER --
     *  Returns the player currently carrying this flag, or
     *  if no player is carrying the flag, null
     */
    @Getter
    private SwordPlayer carrier;
    private ItemStack carrierPrevHelmet;
    private Item droppedItem;

    private java.util.concurrent.ScheduledFuture<?> returnTimer;

    /**
     * Creates a new {@code FlagEntity} for the given team. Call {@link #spawnIdle()} to place it.
     *
     * @param team the team this flag belongs to
     */
    public FlagEntity(CtfTeam team) {
        this.team = team;
    }

    /**
     * Spawns the flag at the team's configured spawn location as an idle ArmorStand.
     * No-ops if the flag is not currently {@link State#IDLE}.
     */
    public void spawnIdle() {
        Location loc = team.getSpawnLocation();
        spawnIdleAt(loc);
    }

    private void spawnIdleAt(Location loc) {
        flagStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        flagStand.setVisible(false);
        flagStand.setGravity(false);
        flagStand.setMarker(true);
        flagStand.setCollidable(false);
        flagStand.customName(Component.text(team.name() + " FLAG", team.getTextColor()));
        flagStand.setCustomNameVisible(true);
        flagStand.getEquipment().setHelmet(team.createFlagItem());
        state = State.IDLE;
    }

    /**
     * Transitions the flag from {@link State#IDLE} or {@link State#DROPPED} to
     * {@link State#CARRIED} for the given player.
     * <p>
     * Removes the ArmorStand (or ground item), saves the player's current helmet, equips the
     * banner on their head, and applies the carrier slow effect.
     * </p>
     *
     * @param player the player picking up the flag
     */
    public void pickup(SwordPlayer player) {
        if (state == State.CARRIED) return;

        cancelReturnTimer();

        if (flagStand != null && flagStand.isValid()) {
            flagStand.remove();
            flagStand = null;
        }
        if (droppedItem != null && droppedItem.isValid()) {
            droppedItem.remove();
            droppedItem = null;
        }

        carrier = player;
        carrierPrevHelmet = player.player().getInventory().getHelmet();
        player.player().getInventory().setHelmet(team.createFlagItem());
        Prefab.PotionEffects.FLAG_CARRIER_SLOW.apply(player);

        player.message(Component.text("You picked up the " + team.name() + " flag!", team.getTextColor()));
        state = State.CARRIED;
    }

    /**
     * Drops the flag at the given location, transitioning to {@link State#DROPPED}.
     * <p>
     * Removes the banner from the carrier's helmet (restoring their previous helmet),
     * removes the carrier slow effect, and starts the auto-return timer.
     * </p>
     *
     * @param loc the location to drop the flag at
     */
    public void drop(Location loc) {
        if (state != State.CARRIED) return;

        restoreCarrierHelmet();
        spawnDroppedItem(loc);
        scheduleReturn();
        state = State.DROPPED;
    }

    /**
     * Immediately returns the flag to its base spawn, transitioning to {@link State#IDLE}.
     * Works from any state.
     */
    public void returnToBase() {
        cancelReturnTimer();
        cleanupDroppedItem();
        cleanupCarrier();
        spawnIdle();
    }

    /**
     * Cleans up all entities and effects managed by this flag. Call when the match ends.
     */
    public void cleanup() {
        cancelReturnTimer();
        cleanupDroppedItem();
        cleanupCarrier();

        if (flagStand != null && flagStand.isValid()) {
            flagStand.remove();
            flagStand = null;
        }
    }

    /**
     * Returns whether the given player is currently carrying this flag.
     *
     * @param player the player to check
     * @return {@code true} if this flag is {@link State#CARRIED} and {@code player} is the carrier
     */
    public boolean isCarriedBy(SwordPlayer player) {
        return state == State.CARRIED && player.equals(carrier);
    }

    // --- private helpers ---

    private void restoreCarrierHelmet() {
        if (carrier == null) return;
        carrier.player().getInventory().setHelmet(carrierPrevHelmet);
        carrier.player().removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
        carrier = null;
        carrierPrevHelmet = null;
    }

    private void cleanupCarrier() {
        if (carrier != null) {
            restoreCarrierHelmet();
        }
    }

    private void cleanupDroppedItem() {
        if (droppedItem != null && droppedItem.isValid()) {
            droppedItem.remove();
            droppedItem = null;
        }
    }

    private void spawnDroppedItem(Location loc) {
        //TODO: Use the actual dropped item logic that spawns an item display
        droppedItem = loc.getWorld().dropItem(loc, team.createFlagItem());
        droppedItem.setPickupDelay(Integer.MAX_VALUE); // prevent natural pickup
        droppedItem.setGravity(true);
    }

    private void scheduleReturn() {
        returnTimer = SwordScheduler.runBukkitTaskLater(
            this::returnToBase,
            Config.Ctf.FLAG_RETURN_TIMER_SECONDS,
            TimeUnit.SECONDS
        );
    }

    private void cancelReturnTimer() {
        if (returnTimer != null && !returnTimer.isDone()) {
            returnTimer.cancel(false);
            returnTimer = null;
        }
    }
}
