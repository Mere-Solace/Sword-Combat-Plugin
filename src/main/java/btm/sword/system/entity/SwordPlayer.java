package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.SwordScheduler;
import btm.sword.system.action.utility.thrown.ThrowAction;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.input.InputAction;
import btm.sword.system.input.InputExecutionTree;
import btm.sword.system.input.InputType;
import btm.sword.system.item.KeyCache;
import btm.sword.system.playerdata.PlayerData;
import btm.sword.util.DisplayUtil;
import com.destroystokyo.paper.profile.PlayerProfile;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Represents a player-controlled combatant in the Sword plugin system.
 * Extends {@link Combatant} with player-specific functionality such as input handling,
 * item display management, and integration with {@link PlayerData}.
 * <p>
 * This class maintains state related to player inputs, held items,
 * throwing mechanics ({@link ThrowAction}), and visual display elements like a sheathed sword.
 * </p>
 */
@Getter
@Setter
public class SwordPlayer extends Combatant {
    private final Player player;
    private final PlayerProfile profile;
    private final String username;
    private final ItemStack playerHead;

    private ItemDisplay sheathed;
    private boolean sheathedReady;

    private final InputExecutionTree inputExecutionTree;
    private final long inputTimeoutMillis = 1200L;

    private boolean performedDropAction;
    private boolean changingHandIndex;
    private boolean interactingWithEntity;
    private boolean threwItem;

    private BukkitTask rightTask;
    private boolean holdingRight;
    private long rightHoldTimeStart;
    private long timeRightHeld;
    private ItemStack mainItemStackAtTimeOfHold;
    private ItemStack offItemStackAtTimeOfHold;
    private int indexOfRightHold;

    private BukkitTask sneakTask;
    private boolean sneaking;
    private long sneakHoldTimeStart;
    private long timeSneakHeld;

    private int thrownItemIndex;

    private boolean swappingInInv;
    private boolean droppingInInv;

    /**
     * Constructs a new SwordPlayer wrapping a Bukkit {@link Player} with associated {@link PlayerData}.
     * Initializes the input execution tree and player head item.
     *
     * @param associatedEntity the Bukkit living entity (player) to wrap
     * @param data the {@link PlayerData} containing extended player info
     */
    public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
        super(associatedEntity, data.getCombatProfile());
        player = (Player) self;
        profile = player.getPlayerProfile();
        username = profile.getName();

        playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();

        skullMeta.setPlayerProfile(profile);
        playerHead.setItemMeta(skullMeta);

        inputExecutionTree = new InputExecutionTree(inputTimeoutMillis);
        inputExecutionTree.initializeInputTree();

        sheathedReady = true;

        performedDropAction = false;
        changingHandIndex = false;
        interactingWithEntity = false;
        threwItem = false;

        holdingRight = false;
        rightHoldTimeStart = 0L;
        timeRightHeld = 0L;

        sneaking = false;
        sneakHoldTimeStart = 0L;
        timeSneakHeld = 0L;

        thrownItemIndex = -1;

        swappingInInv = false;
        droppingInInv = false;
    }

    /**
     * Called each server tick to update the player state.
     * Extends {@link Combatant#onTick()} to restore food and absorption,
     * and handle the visual sheathed sword display using {@link ItemDisplay}.
     */
    @Override
    protected void onTick() {
        super.onTick();
        player.setFoodLevel(20);
        player.setAbsorptionAmount(20);

        if ((sheathed == null || sheathed.isDead()) && isSheathedReady()) {
            setSheathedReady(false);
            Bukkit.getScheduler().runTaskLater(Sword.getInstance(), () -> {
                if (!player.isOnline() || !player.isValid()) return;

                World world = player.getWorld();
                Location loc = player.getLocation();

                if (!loc.getChunk().isLoaded()) loc.getChunk().load();

                sheathed = (ItemDisplay) world.spawnEntity(loc, EntityType.ITEM_DISPLAY);
                sheathed.setItemStack(new ItemStack(Material.STONE_SWORD));

                sheathed.setTransformation(new Transformation(
                        new Vector3f(0.28f, 0.06f, -0.5f),
                        new Quaternionf().rotationY((float) Math.PI / 2).rotateZ(-(float) Math.PI / (1.65f)),
                        new Vector3f(1f, 1f, 1f),
                        new Quaternionf()
                ));

                setSheathedReady(true);
            }, 5L);
        }

        if (isSheathedReady()) {
            int its = 3;
            for (int i = 0; i < its; i++) {
                SwordScheduler.runLater(new BukkitRunnable() {
                    @Override
                    public void run() {

                        DisplayUtil.smoothTeleport(sheathed, 1);
                        sheathed.teleport(player.getLocation().add(new Vector(0, 0.5, 0)).setDirection(getFlatBodyDir()));
                    }
                }, 1000/its * i, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Called when the player entity spawns or respawns.
     * Extends {@link Combatant#onSpawn()}.
     */
    @Override
    public void onSpawn() {
        super.onSpawn();

//        Bukkit.getScheduler().runTaskLater(Sword.getInstance(), () -> {
//            if (!player.isOnline() || !player.isValid()) return;
//
//            World world = player.getWorld();
//            Location loc = player.getLocation();
//
//            if (!loc.getChunk().isLoaded()) loc.getChunk().load();
//
//            sheathed = (ItemDisplay) world.spawnEntity(loc, EntityType.ITEM_DISPLAY);
//            sheathed.setItemStack(new ItemStack(Material.STONE_SWORD));
//
//            sheathed.setTransformation(new Transformation(
//                    new Vector3f(0.28f, 0.06f, -0.5f),
//                    new Quaternionf().rotationY((float) Math.PI / 2).rotateZ(-(float) Math.PI / (1.65f)),
//                    new Vector3f(1f, 1f, 1f),
//                    new Quaternionf()
//            ));
//
//            setSheathedReady(true);
//        }, 15L);
    }

    /**
     * Called when the player dies.
     * Cleans up the sheathed sword display entity.
     */
    @Override
    public void onDeath() {
        if (sheathed != null)
            sheathed.remove();
    }

    /**
     * Called when the player leaves the game.
     * Removes the sheathed sword display.
     */
    public void onLeave() {
        if (sheathed != null)
            sheathed.remove();
    }

    /**
     * Processes a player input of {@link InputType}, executing associated {@link InputAction}s
     * based on the input execution tree. Handles interrupting throwing, grabbing, swapping,
     * and cooldowns.
     *
     * @param input the input type from the player to process
     */
    public void act(InputType input) {
        if (isAttemptingThrow()) {
            if (input != InputType.RIGHT && input != InputType.RIGHT_HOLD) {
                ThrowAction.throwCancel(this);
                resetTree();
                return;
            }
        }

        if (input == InputType.SWAP && isGrabbing()) {
            setGrabbing(false);
            return;
        }

        if (input == InputType.LEFT && isGrabbing()) {
            onGrabHit();
            return;
        }

        if (getAbilityCastTask() != null) {
            return;
        }

        if (input == InputType.RIGHT) {
            if (rightTask == null)
                startHoldingRight();
            else
                return;
        }
        else if (input == InputType.SHIFT) {
            if (sneakTask == null)
                startSneaking();
            else
                return;
        }

        if (input == InputType.RIGHT_TAP || input == InputType.SHIFT_TAP) {
            if (!inputExecutionTree.nextExists(input)) return;
        }

        if (input == InputType.RIGHT_HOLD) {
            long minTime = inputExecutionTree.getMinHoldLengthOfNext(input);
            if (minTime == -1 || timeRightHeld < minTime) {
                if (isAttemptingThrow()) ThrowAction.throwCancel(this);
                return;
            }
        }
        else if (input == InputType.SHIFT_HOLD) {
            long minTime = inputExecutionTree.getMinHoldLengthOfNext(input);
            if (minTime == -1 || timeSneakHeld < minTime) {
                return;
            }
        }

        InputExecutionTree.InputNode node = inputExecutionTree.step(input);

        if (node == null)
            return;
        else if (node.isDisplay())
            displayInputSequence();

        InputAction action = node.getAction();

        if (action != null) {
            if (!action.execute(this)) {
                resetTree();
            }
        }
    }

    /**
     * Evaluates an inventory item input before processing it in {@link #act(InputType)}.
     * Can be used to filter out inputs or trigger cancellations.
     *
     * @param itemStack the item stack involved in the input
     * @param inputType the input type being evaluated
     * @return true to cancel the action, false to allow processing
     */
    public boolean evaluateItemInput(ItemStack itemStack, InputType inputType) {
        Material type = itemStack.getType();
        ItemMeta meta = itemStack.getItemMeta();
        String id = meta != null ? meta.getPersistentDataContainer().get(KeyCache.buttonTagKey, PersistentDataType.STRING) : null;

        return false;
    }

    /**
     * Handles inventory click events to interact with the player's inventory.
     * Can be customized to modify behavior based on click type, inventory, and slot.
     *
     * @param e the inventory click event to handle
     * @return true if the event was handled and should be cancelled, false otherwise
     */
    public boolean handleInventoryInput(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        ClickType clickType = e.getClick();
        InventoryAction action = e.getAction();
        ItemStack onCursor = e.getCursor();
        ItemStack clicked = e.getCurrentItem();
        int slotNumber = e.getSlot();

//		message("\n\n~|------Beginning of new inventory interact event------|~"
//				+ "\n       Inventory: " + inv.getType()
//				+ "\n       Click type: " + clickType
//				+ "\n       Action type: " + action
//				+ "\n       Item on cursor: " + onCursor
//				+ "\n       Current Item in slot: " + clicked
//				+ "\n       slot number: " + slotNumber);
//
//		message("Normal click event.");
        return false;
    }

    /**
     * Returns the underlying {@link Player} entity for this SwordPlayer.
     *
     * @return the Bukkit player entity
     */
    public Player player() {
        return player;
    }

    /**
     * Checks if the player has performed a drop action recently.
     *
     * @return true if a drop action was performed, false otherwise
     */
    public boolean hasPerformedDropAction() {
        return performedDropAction;
    }

    /**
     * Resets the input execution tree to its root state.
     */
    public void resetTree() {
        inputExecutionTree.reset();
    }

    /**
     * Checks if the input execution tree is at its root node.
     *
     * @return true if at root, false otherwise
     */
    public boolean isAtRoot() {
        return inputExecutionTree.isAtRoot();
    }

    /**
     * Displays the current input sequence progress of the player as a title.
     */
    public void displayInputSequence() {
        self.showTitle(Title.title(
                Component.text(""),
                Component.text(inputExecutionTree.toString(), NamedTextColor.DARK_RED, TextDecoration.ITALIC),
                Title.Times.times(
                        Duration.ofMillis(0),
                        Duration.ofMillis(inputTimeoutMillis),
                        Duration.ofMillis(100))));
    }

    /**
     * Displays a visual indication of a mistake in input as a title.
     */
    public void displayMistake() {
        self.showTitle(Title.title(
                Component.text(""),
                Component.text("~*#*~", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC),
                Title.Times.times(
                        Duration.ofMillis(0),
                        Duration.ofMillis(inputTimeoutMillis),
                        Duration.ofMillis(100))));
    }

    /**
     * Displays a visual indication that the player is disabled, via a title.
     */
    public void displayDisablingEffect() {
        self.showTitle(Title.title(
                Component.text(""),
                Component.text("ur disabled", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC),
                Title.Times.times(
                        Duration.ofMillis(0),
                        Duration.ofMillis(inputTimeoutMillis),
                        Duration.ofMillis(100))));
    }

    /**
     * Displays a cooldown timer remaining to the player as a title,
     * showing time in seconds if above 1000ms, else milliseconds.
     *
     * @param timeLeft time left on cooldown in milliseconds
     */
    public void displayCooldown(long timeLeft) {
        double timeToDisplay = timeLeft > 1000L ? (double)timeLeft/1000 : timeLeft;
        String unit = timeLeft > 1000L ? "s" : "ms";
        self.showTitle(Title.title(
                Component.text(""),
                Component.text("on cooldown: " + timeToDisplay + " " + unit, NamedTextColor.GRAY, TextDecoration.ITALIC),
                Title.Times.times(
                        Duration.ofMillis(0),
                        Duration.ofMillis(inputTimeoutMillis),
                        Duration.ofMillis(100))));
    }

    /**
     * Displays a custom title and subtitle to the player with specified timing.
     *
     * @param title main title text component
     * @param subtitle subtitle text component
     * @param fadein duration of fade-in in milliseconds
     * @param duration duration to display the title in milliseconds
     * @param fadeout duration of fade-out in milliseconds
     */
    public void displayTitle(Component title, Component subtitle, long fadein, long duration, long fadeout) {
        self.showTitle(Title.title(
                title,
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(fadein),
                        Duration.ofMillis(duration),
                        Duration.ofMillis(fadeout))));
    }

    /**
     * Changes the display name of the item in the player's main hand temporarily, showing it with a color and style.
     *
     * @param toDisplay the string to show as the item name
     * @param color the {@link TextColor} to apply
     * @param style the {@link TextDecoration} to apply, or null for none
     */
    public void itemNameDisplay(String toDisplay, TextColor color, TextDecoration style) {
        ItemStack stack = getItemStackInHand(true).clone();
        if (stack.isEmpty() || stack.getType().isAir()) stack = new ItemStack(Material.GUNPOWDER);
        ItemMeta metaData = stack.getItemMeta();
        if (metaData == null) {
            message("MetaData for item to display is null!");
            return;
        }
        if (style == null)
            metaData.itemName(Component.text(toDisplay, color));
        else
            metaData.itemName(Component.text(toDisplay, color, style));

        stack.setItemMeta(metaData);
        player.sendEquipmentChange(self, EquipmentSlot.HAND, stack);
    }

    /**
     * Adds a base value to a given {@link AspectType} stat on this player.
     *
     * @param stat the {@link AspectType} to increment
     * @param amount the amount to add to the base value
     */
    public void addStat(AspectType stat, int amount) {
        aspects.getAspect(stat).addBaseValue(amount);
        // invalidate all cached, calculated values with that stat
    }

    /**
     * Checks if the input execution tree requires the same item to be used for inputs.
     *
     * @return true if input actions are item-specific, false otherwise
     */
    public boolean inputReliantOnItem() {
        return inputExecutionTree.requiresSameItem();
    }

    /**
     * Starts holding the right mouse button, tracking the hold time and managing state.
     * Changes the player's main hand item to a placeholder while holding (gunpowder).
     */
    public void startHoldingRight() {
        if (holdingRight) return;

        if (rightTask != null && !rightTask.isCancelled()) rightTask.cancel();

        holdingRight = true;
        rightHoldTimeStart = System.currentTimeMillis();

        mainItemStackAtTimeOfHold = getItemStackInHand(true);
        offItemStackAtTimeOfHold = getItemStackInHand(false);

        indexOfRightHold = getCurrentInvIndex();

        if (!mainItemStackAtTimeOfHold.isEmpty())
            setItemStackInHand(new ItemStack(Material.GUNPOWDER), true); // can change the logic here later

        rightTask = new BukkitRunnable() {
            @Override
            public void run() {
                inputExecutionTree.restartTimeoutTimer();
                if (!player.isHandRaised() && !player.isBlocking()) { // player must ALWAYS be holding a shield in offhand, then... I can work with this though
                    endHoldingRight();
                }
                if (!holdingRight) {
                    if (timeRightHeld < 162)
                        act(InputType.RIGHT_TAP);
                    else
                        act(InputType.RIGHT_HOLD);
                    resetHoldingRight();
                    cancel();
                }
            }
        }.runTaskTimer(Sword.getInstance(), 2L, 1L);
    }

    /**
     * Resets the holding right state and cancels the associated task.
     */
    public void resetHoldingRight() {
        rightTask = null;
        holdingRight = false;
        rightHoldTimeStart = 0L;
        timeRightHeld = 0L;
    }

    /**
     * Ends holding right-click input, restoring item stacks appropriately.
     */
    public void endHoldingRight() {
//        message(">>> End of Right Hold, threw item? " + (threwItem ? "yes" : "nope"));
        holdingRight = false;
        timeRightHeld = System.currentTimeMillis() - rightHoldTimeStart;
        setItemStackInHand(offItemStackAtTimeOfHold, false);
        if (!mainItemStackAtTimeOfHold.isEmpty() && !threwItem)
            setItemAtIndex(mainItemStackAtTimeOfHold, indexOfRightHold);
    }

    /**
     * Starts sneaking state, tracking the hold time and scheduling updates.
     */
    public void startSneaking() {
        if (sneaking) return;

        if (sneakTask != null && !sneakTask.isCancelled()) sneakTask.cancel();

        sneaking = true;
        sneakHoldTimeStart = System.currentTimeMillis();

        sneakTask = new BukkitRunnable() {
            @Override
            public void run() {
                inputExecutionTree.restartTimeoutTimer();
                if (!sneaking) {
                    if (timeSneakHeld < 162)
                        act(InputType.SHIFT_TAP);
                    else
                        act(InputType.SHIFT_HOLD);
                    resetSneaking();
                    cancel();
                }
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 1L);
    }

    /**
     * Resets sneaking state and cancels the associated task.
     */
    public void resetSneaking() {
        sneakTask = null;
        sneaking = false;
        sneakHoldTimeStart = 0L;
        timeSneakHeld = 0L;
    }

    /**
     * Ends sneaking state and calculates how long the player sneaked.
     */
    public void endSneaking() {
        sneaking = false;
        timeSneakHeld = System.currentTimeMillis() - sneakHoldTimeStart;
    }

    /**
     * Records the current held inventory slot index as the thrown item index.
     */
    public void setThrownItemIndex() {
        thrownItemIndex = getCurrentInvIndex();
    }

    /**
     * Gets the current inventory slot index the player is holding.
     *
     * @return the held item slot index
     */
    public int getCurrentInvIndex() {
        return player.getInventory().getHeldItemSlot();
    }

    /**
     * Sets the {@link ItemStack} in the player's inventory at the specified index.
     *
     * @param item the {@link ItemStack} to set
     * @param index the inventory slot index
     */
    public void setItemAtIndex(ItemStack item, int index) {
        player.getInventory().setItem(index, item);
    }

    /**
     * Marks that the player is currently swapping items in inventory.
     * Resets the flag shortly after (1 tick).
     */
    public void setSwappingInInv() {
        swappingInInv = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                swappingInInv = false;
            }
        }.runTaskLater(Sword.getInstance(), 1L);
    }

    /**
     * Marks that the player is currently dropping items in inventory.
     * Resets the flag shortly after (1 tick).
     */
    public void setDroppingInInv() {
        droppingInInv = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                droppingInInv = false;
            }
        }.runTaskLater(Sword.getInstance(), 1L);
    }
}
