package btm.sword.system.entity.impl;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.destroystokyo.paper.profile.PlayerProfile;

import btm.sword.config.Config;
import btm.sword.system.action.BlockAction;
import btm.sword.system.action.throwing.ThrowAction;
import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.input.ActivationContext;
import btm.sword.system.input.InputAction;
import btm.sword.system.input.InputActionExecutor;
import btm.sword.system.input.InputBuffer;
import btm.sword.system.input.InputExecutionTree;
import btm.sword.system.input.InputRegistrar;
import btm.sword.system.input.InputType;
import btm.sword.system.inventory.InventoryMenuManager;
import btm.sword.system.inventory.PlayerMenuManager;
import btm.sword.system.inventory.menu.MainMenu;
import btm.sword.system.item.ItemClass;
import btm.sword.system.item.ItemClassifier;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.KeyRegistry;
import btm.sword.system.item.SwordItemType;
import btm.sword.system.item.special.NonMovableItem;
import btm.sword.system.item.special.SlotAnchoredItem;
import btm.sword.system.playerdata.PlayerData;
import btm.sword.utility.Prefab;
import btm.sword.utility.SwordTimeUnit;
import btm.sword.utility.display.DisplayUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

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
public class SwordPlayer extends Combatant {
    private final Player player;
    private final PlayerProfile profile;
    private final String username;
    private final ItemStack playerHead;

    private final PlayerMenuManager playerMenuManager;
    private final SlotAnchoredItem menuButton;
    private final SlotAnchoredItem shieldItem;
    private final SlotAnchoredItem chestplateItem;

    private final int maxNumDummies = 3;
    private int curNumDummies = 0;
    private final HashSet<Dummy> yourDummies = new HashSet<>();

    private final InputExecutionTree inputExecutionTree;
    private final InputBuffer inputBuffer;
    private final long baseInputTimeoutMillis = 1400L;

    /** Current input context; controls which action paths are visible in the execution tree. */
    @Setter
    private ActivationContext activationContext = ActivationContext.NORMAL;

    @Setter
    private boolean performedDropAction;
    @Getter
    @Setter
    private ItemStack lastHeldItemBeforeDrop = new ItemStack(Material.AIR);
    @Setter
    private boolean changingHandIndex;
    @Setter
    private boolean interactingWithEntity;
    @Setter
    private boolean threwItem;
    @Setter
    private boolean blocking;

    /** System.currentTimeMillis() deadline for the active parry hit-detection window. */
    @Setter
    private long parryWindowEnd = 0L;

    private TimeArbiter.TaskHandle blockDrainTask;

    private TimeArbiter.TaskHandle rightClickHoldTask;
    private boolean holdingRight;
    private long rightHoldTimeStart;
    private long timeRightHeld;
    private ItemStack mainItemStackAtTimeOfHold;
    private ItemStack offItemStackAtTimeOfHold;
    private int indexOfRightHold;

    private TimeArbiter.TaskHandle sneakTask;
    private boolean sneaking;
    private long sneakHoldTimeStart;
    private long timeSneakHeld;

    private int thrownItemIndex;

    private boolean swappingInInv;
    private boolean droppingInInv;

    private BukkitTask targetIndicatorTask;
    private SwordEntity targetedEntity;
    private TextDisplay targetIndicator;

    private int prevFormVal;
    private float formProgress;
    private final Supplier<Float> formExpTickStepVal =
        () -> 1.0f / SwordTimeUnit.millisToTicks(aspects.form().effectivePeriod());
           // 1.0f because needs to be scaled between 0 and 1.

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

        ItemStack temp = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) temp.getItemMeta();
        skullMeta.setPlayerProfile(profile);

        playerHead = new ItemStackBuilder(Material.PLAYER_HEAD)
            .setMeta(skullMeta)
            .hideAll()
            .name(Component.text("Your Stats", Config.SwordColor.TEXT_COOL_DARK))
            .lore(aspects.toComponentList())
            .build();

        playerMenuManager = new PlayerMenuManager(this);

        menuButton = new SlotAnchoredItem(
            new ItemStackBuilder(Material.ECHO_SHARD)
                .hideAll()
                .name(Component.text("~ | Main Menu | ~", Config.SwordColor.TEXT_COOL, TextDecoration.BOLD))
                .tag(KeyRegistry.MAIN_MENU_BUTTON_KEY, PersistentDataType.STRING, "yes")
                .tag(KeyRegistry.ITEM_CLASS_KEY, PersistentDataType.STRING, ItemClass.BLOCKED.name())
                .build(),
            8,
            KeyRegistry.MAIN_MENU_BUTTON_KEY
        );

        shieldItem = new SlotAnchoredItem(
            new ItemStackBuilder(Material.SHIELD).build(),
            40,
            Material.SHIELD
        );

        chestplateItem = new SlotAnchoredItem(
            new ItemStackBuilder(Material.NETHERITE_CHESTPLATE).build(),
            38,
            Material.NETHERITE_CHESTPLATE
        );

        inputExecutionTree = new InputExecutionTree(this);
        inputBuffer = new InputBuffer();
        InputRegistrar.initializeInputTree(inputExecutionTree.getRoot(), this);
        InputRegistrar.initializeMovementInputs(inputExecutionTree.getRoot());

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
        inputBuffer.tick();

        if (player.getHealth() > 0) updateVisualStats();

        if (ticks % 5 == 0) {
            inventoryUpkeep();
        }

        targetEntityIndicatorTick();

        expBarTick();
    }

    /**
     * Called when the player entity spawns or respawns.
     * Extends {@link Combatant#onSpawn()}.
     */
    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    /**
     * Called when the player dies.
     * Cleans up the sheathed sword display entity.
     */
    @Override
    public void onDeath() {
        super.onDeath();
        endIndicatorDisplay();
    }

    /**
     * Called when the player leaves the game.
     */
    public void onLeave() {
        if (getUmbralBlade() != null) {
            getUmbralBlade().dispose();
        }
        endStatusDisplay();
        endIndicatorDisplay();
        destroyed = true;
    }

    /**
     * Returns true if the parry hit-detection window is active (an incoming
     * BLOCKABLE hit within this window will be parried).
     *
     * @return true if the parry window is active
     */
    @Override
    public boolean isInParryWindow() {
        return System.currentTimeMillis() < parryWindowEnd;
    }

    /**
     * Cancels the active soulfire block drain task, if any.
     */
    public void cancelBlockDrainTask() {
        if (blockDrainTask != null && !blockDrainTask.isCancelled()) {
            blockDrainTask.cancel();
            blockDrainTask = null;
        }
    }

    /**
     * Starts the repeating soulfire drain ticker while blocking.
     * Drains {@link Config.Combat#BLOCK_SOULFIRE_DRAIN_PER_SECOND} per second.
     * Breaks the block automatically if soulfire hits zero. Called from
     * {@link btm.sword.system.action.BlockAction#startBlock}.
     */
    public void startBlockDrain() {
        cancelBlockDrainTask();
        // Tick every 200ms (5 times/sec); each tick drains 1/5 of the per-second cost.
        int periodMs = 200;
        blockDrainTask = TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            null,
            () -> {
                if (!isBlocking()) return;
                Prefab.Particles.UMBRAL_FLAME.display(getChestLocation().add(dir()));
                float drainPerTick = Config.Combat.BLOCK_SOULFIRE_DRAIN_PER_SECOND / (1000f / periodMs);
                aspects.soulfire().remove(drainPerTick);
                if (aspects.soulfire().cur() <= 0) {
                    BlockAction.onBlockBroken(this);
                }
            },
            periodMs, periodMs,
            SwordPlayer.class, "startBlockDrain",
            new PredicateRunnablePair(
                () -> !isBlocking(),
                this::cancelBlockDrainTask
            )
        );
    }

    /**
     * Processes a player input of {@link InputType}, executing associated {@link InputAction}s
     * based on the input execution tree. Handles interrupting throwing, grabbing, swapping,
     * and cooldowns.
     *
     * @param input the input type from the player to process
     */
    public void act(InputType input) {
        if (ItemClassifier.isBlocked(getItemStackInHand(true))) return;

        if (throwingState()) {
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
            if (rightClickHoldTask == null)
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
                if (throwingState()) ThrowAction.throwCancel(this);
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

        InputAction action = inputExecutionTree.getNextAction();

        if (action != null) {
            InputActionExecutor.execute(action, this);
        }

        Consumer<SwordPlayer> internalAction = node.getInternalAction();

        if (internalAction != null) {
            internalAction.accept(this);
        }
    }

    @Override
    protected void updateStatus() {
        super.updateStatus();
    }

    private void expBarTick() {
        int curFormVal = (int) aspects.formCur();
        if (prevFormVal == curFormVal) {
            if (curFormVal == aspects.formMaxVal()) {
                formProgress = 0.99f;
                return; // don't want to go over: it causes an error
            }
            formProgress += formExpTickStepVal.get();
        }
        else {
            player.setLevel(curFormVal);
            formProgress = 0;
        }
        player.setExp(Math.max(0.0f, Math.min(1.0f, formProgress))); // clamp between 0 and 1
        prevFormVal = curFormVal;
    }

    private void inventoryUpkeep() {
        if (!shieldItem.isSatisfied(player)) {
            shieldItem.restore(player);
        }

        if (!chestplateItem.isSatisfied(player)) {
            chestplateItem.restore(player);
        }

        if (!menuButton.isSatisfied(player)) {
            menuButton.restore(player);
        }

        if (getUmbralBlade() != null && !getUmbralBlade().getLinkAnchor().isSatisfied(player)) {
            getUmbralBlade().getLinkAnchor().restore(player);
        }
    }

    /**
     * Dispatches an input event for items that carry a Sword-managed type key.
     * This is the primary point of contact for typed-item input handling, called by
     * {@link btm.sword.listeners.InputListener} before any class-based filtering or
     * routing through {@link #act(InputType)}.
     * <p>
     * If this method returns {@code true}, the caller must cancel the originating Bukkit
     * event and skip all further input processing — even if no action was taken (e.g.
     * shift is suppressed for the menu button without opening the menu).
     * </p>
     *
     * @param itemStack the item stack involved in the input; must not be null
     * @param input the input type being evaluated
     * @return {@code true} if the input was fully handled and the event should be cancelled
     */
    public boolean handleItemInteraction(ItemStack itemStack, InputType input) {
        if (KeyRegistry.hasKey(itemStack, KeyRegistry.MAIN_MENU_BUTTON_KEY)) {
            if (input != InputType.SHIFT) {
                InventoryMenuManager.openMenu(MainMenu.class, this);
            }
            return true;
        }

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

        if (clicked == null) {

            return false;
        }

        // Protect non-movable items from being interacted with
        if (NonMovableItem.isNonMovable(clicked) || NonMovableItem.isNonMovable(onCursor)) {
            return true; // Cancel the action
        }
//        message("\n\n~|------Beginning of new inventory interact event------|~"
//                + "\n       Inventory: " + inv.getType()
//                + "\n       Click type: " + clickType
//                + "\n       Action type: " + action
//                + "\n       Item on cursor: " + onCursor
//                + "\n       Current Item in slot: " + clicked
//                + "\n       slot number: " + slotNumber);
        return false;
    }

    public void updateVisualStats() {
        player.setAbsorptionAmount(aspects.toughnessCur());
        player.setHealth(Math.max(2, 2 * aspects.shardsCur()));
        player.setFoodLevel((int) (20 * (aspects.soulfireCur()/aspects.soulfireMaxVal())));
    }

    /**
     * Returns the underlying {@link Player} entity for this SwordPlayer.
     *
     * @return the Bukkit player entity
     */
    public Player player() {
        return player;
    }

    public ItemStack getPlayerHeadItemWithCustomText(Component title, List<Component> lore) {
        ItemStack temp = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) temp.getItemMeta();
        skullMeta.setPlayerProfile(profile);

        return new ItemStackBuilder(Material.PLAYER_HEAD)
            .setMeta(skullMeta)
            .name(title)
            .lore(lore)
            .hideAll()
            .build();
    }

    /**
     * Checks if the player has performed a drop action recently.
     *
     * @return true if a drop action was performed, false otherwise
     */
    public boolean hasPerformedDropAction() {
        return performedDropAction;
    }

    public void disableShield(int ticks) {
        self.clearActiveItem();
        player.setCooldown(getItemStackInHand(false), ticks);
    }

    public boolean isUnableToBlock() {
        return player.getCooldown(getItemStackInHand(false)) > 0;
    }

    @Override
    public boolean holdingUmbralBlade() {
        return super.holdingUmbralBlade() ||
            (isPerformedDropAction() && KeyRegistry.hasKey(getLastHeldItemBeforeDrop(), KeyRegistry.UMBRAL_BLADE_KEY));
    }

    @Override
    public boolean holdingSoulLink() {
        return super.holdingSoulLink() ||
            (isPerformedDropAction() && KeyRegistry.hasKey(getLastHeldItemBeforeDrop(), KeyRegistry.SOUL_LINK_KEY));
    }

    public boolean normalActState() {
        return activationContext == ActivationContext.NORMAL;
    }

    /** Returns true while the player is in throw-ready posture (between throwReady and release/cancel). */
    public boolean throwingState() {
        return activationContext == ActivationContext.THROWING;
    }

    public boolean throwingNonUmbralState() {
        return (throwingState() || nonUmbralState());
    }

    public boolean nonUmbralState() {
        return normalActState() && !holdingSoulLink() && !holdingUmbralBlade();
    }

    public boolean soulLinkState() {
        return normalActState() && holdingSoulLink();
    }

    public boolean umbralBladeState() {
        return normalActState() && holdingUmbralBlade();
    }

    public boolean umbralState() {
        return normalActState() && (holdingSoulLink() || holdingUmbralBlade());
    }

    public boolean activeItemState(int slot) {
        return normalActState() &&
                SwordItemType.fromString(getItemStackInHand(true)) ==
                    (slot == 1 ? SwordItemType.ACTIVE_1 : SwordItemType.ACTIVE_2);
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
     * Returns whether movement (dash) inputs are currently enabled for this player.
     *
     * @return true if movement inputs are enabled
     */
    public boolean isMovementEnabled() {
        return inputExecutionTree.isMovementEnabled();
    }

    /**
     * Displays the current input sequence progress of the player as a title.
     */
    public void displayInputSequence() {
        self.showTitle(Title.title(
                Component.text(""),
                inputExecutionTree.getInputSequenceAsComponent(),
                Title.Times.times(
                    Duration.ofMillis(20),
                    Duration.ofMillis(inputExecutionTree.timeoutTicks() * 50),
                    Duration.ofMillis(20))));
    }

    /**
     * Displays a visual indication of a mistake in input as a title.
     */
    public void displayMistake() {
        self.showTitle(Title.title(
                Component.text(""),
                Component.text("~*#*~", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC),
                Title.Times.times(
                    Duration.ofMillis(20),
                    Duration.ofMillis(baseInputTimeoutMillis),
                    Duration.ofMillis(20))));
    }

    /**
     * Displays current and max soulfire as a subtitle immediately after a successful cast.
     * Shows the remaining input sequence (if any) alongside the soulfire values, with a color
     * that interpolates from dark to bright based on how full soulfire is.
     */
    public void displaySoulfireConsumed(int consumed) {
        float cur = aspects.soulfireCur();
        itemNameDisplay(Component.text("»  " + (int) cur, TextColor.color(80, 80, 80), TextDecoration.BOLD)
            .append(Component.text("  ᅳ" + consumed, TextColor.color(30, 108, 167), TextDecoration.BOLD))
            .append(Component.text("  «", TextColor.color(80, 80, 80), TextDecoration.BOLD)));
    }

    public void displayLackOfSoulfire(float required) {
        self.showTitle(Title.title(
            Component.text("✖", Config.SwordColor.TEXT_COOL_DARK),
            Component.text(String.format("%.1f", aspects.soulfireCur()) + "/" + required, Config.SwordColor.TEXT_ITEM_BASE, TextDecoration.ITALIC),
            Title.Times.times(
                Duration.ofMillis(20),
                Duration.ofMillis(baseInputTimeoutMillis),
                Duration.ofMillis(20))));
    }

    /**
     * Displays a visual indication that the player is disabled, via a title.
     */
    public void displayDisablingEffect() {
        self.showTitle(Title.title(
                Component.text(""),
                Component.text("*}- " + activationContext.name() + " -{*", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC),
                Title.Times.times(
                    Duration.ofMillis(20),
                    Duration.ofMillis(baseInputTimeoutMillis),
                    Duration.ofMillis(20))));
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
                    Duration.ofMillis(20),
                    Duration.ofMillis(baseInputTimeoutMillis),
                    Duration.ofMillis(20))));
    }

    /**
     * Displays a custom title and subtitle to the player with specified timing.
     *
     * @param title main title text component
     * @param subtitle subtitle text component
     * @param fade_in duration of fade-in in milliseconds
     * @param duration duration to display the title in milliseconds
     * @param fade_out duration of fade-out in milliseconds
     */
    public void displayTitle(@Nullable Component title, @Nullable Component subtitle, long fade_in, long duration, long fade_out) {
        if (title == null && subtitle == null) return;
        self.showTitle(Title.title(
                title == null ? Component.text("") : title,
                subtitle == null ? Component.text("") : subtitle,
                Title.Times.times(
                    Duration.ofMillis(fade_in),
                    Duration.ofMillis(duration),
                    Duration.ofMillis(fade_out))));
    }

    public void itemNameDisplay(Component displayName) {
        ItemStack stack = getItemStackInHand(true).clone();
        if (stack.isEmpty() || stack.getType().isAir()) stack = new ItemStack(Material.GUNPOWDER);
        ItemMeta metaData = stack.getItemMeta();
        if (metaData == null) {
            return;
        }
        metaData.displayName(displayName);

        stack.setItemMeta(metaData);
        player.sendEquipmentChange(self, EquipmentSlot.HAND, stack);
    }

    /**
     * Changes the display name of the item in the player's main hand temporarily, showing it with a color and style.
     *
     * @param toDisplay the string to show as the item name
     * @param color the {@link TextColor} to apply
     * @param style the {@link TextDecoration} to apply, or null for none
     */
    public void itemNameDisplay(String toDisplay, TextColor color, TextDecoration style) {
        if (style == null) {
            itemNameDisplay(Component.text(toDisplay, color));
        }
        else {
            itemNameDisplay(Component.text(toDisplay, color, style));
        }
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

    protected void removeTargetIndicators() {
        if (targetIndicator != null && targetIndicator.isValid()) {
            targetIndicator.remove();
        }
    }

    /**
     * Very important method: it not only gets the targeted entity,
     * but also sets the currently targeted entity and adjusts the indicator display
     * <p>
     * Use whenever targeting enemies with specific attacks (AOE attacks like the
     * basic attack might not fit for this situation.)
     */
    @Override
    public SwordEntity getTargetedEntity(double range) {
        SwordEntity newTarget = super.getTargetedEntity(range);
        if (newTarget == null) return null;

        if (targetedEntity != null && (newTarget.getUuid() != targetedEntity.getUuid())) {
            removeTargetIndicators();
        }

        targetedEntity = newTarget;

        return newTarget;
    }

    public void setTargetedEntity(SwordEntity newTarget) {
        if (newTarget == null) return;

        if (targetedEntity != null && (newTarget.getUuid() != targetedEntity.getUuid())) {
            removeTargetIndicators();
        }

        targetedEntity = newTarget;
    }

    protected void targetEntityIndicatorTick() {
        if (targetedEntity == null) return;

        if (targetedEntity.isDead()) {
            removeTargetIndicators();
            return;
        }

        if (targetIndicator == null || !targetIndicator.isValid()) {
            targetIndicator = (TextDisplay) self().getWorld().spawnEntity(targetedEntity.getLocation(), EntityType.TEXT_DISPLAY);

            targetedEntity.self().addPassenger(targetIndicator);

            targetIndicator.setBillboard(Display.Billboard.VERTICAL);
            targetIndicator.text(Component.text("⮟",
                TextColor.color(255, 0, 0), TextDecoration.BOLD));
            targetIndicator.setDefaultBackground(false);
            targetIndicator.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            targetIndicator.setBrightness(new Display.Brightness(15, 15));
            targetIndicator.setShadowed(true);
            targetIndicator.setGlowColorOverride(Color.fromRGB(255, 0, 0));
            targetIndicator.setGlowing(true);
        }

        float scale = 3.0f;

        DisplayUtil.setInterpolationValues(targetIndicator, 0, 10);
        targetIndicator.setTransformation(
            new Transformation(
                new Vector3f(0, 1.35f + ((float) Math.cos(ticks * Math.PI/16) * 0.5f), 0),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
            )
        );
    }

    public void endIndicatorDisplay() {
        removeTargetIndicators();
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
     * Starts holding the start mouse button, tracking the hold time and managing state.
     * Changes the player's main hand item to a placeholder while holding (gunpowder).
     */
    public void startHoldingRight() {
        if (holdingRight) return;

        if (rightClickHoldTask != null && !rightClickHoldTask.isCancelled()) rightClickHoldTask.cancel();

        holdingRight = true;
        rightHoldTimeStart = System.currentTimeMillis();

        // TODO: #123 - Handle umbral blade holding
        mainItemStackAtTimeOfHold = getItemStackInHand(true);
        offItemStackAtTimeOfHold = getItemStackInHand(false);

        indexOfRightHold = getCurrentInvIndex();

        if (!holdingUmbralItemInMainHand()) {

            // TODO: #123 - This is where to implement catches for start clicking different items

            if (!mainItemStackAtTimeOfHold.isEmpty() &&
                !holdingUmbralItemInMainHand()) {
                setItemStackInHand(new ItemStack(Material.GUNPOWDER), true); // can change the logic here later
            }
        }

        rightClickHoldTask = TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            () -> {
                // While blocking, let the trie timeout expire naturally — that closes the parry window.
                // For all other right-hold scenarios (throw ready, etc.) keep the timer alive.
                if (!isBlocking()) inputExecutionTree.restartTimeoutTimer();
                // player must ALWAYS be holding a shield in offhand, then... I can work with this though
                if (!player.isHandRaised() && !player.isBlocking()) {
                    endHoldingRight();
                }
            },
            null,
            100, 50,
            SwordPlayer.class, "startHoldingRight",
            new PredicateRunnablePair(
                () -> !holdingRight,
                () -> {
                    if (timeRightHeld < 162)
                        act(InputType.RIGHT_TAP);
                    else
                        act(InputType.RIGHT_HOLD);
                    resetHoldingRight();
                }
            )
        );
    }

    /**
     * Resets the holding start state and cancels the associated task.
     */
    public void resetHoldingRight() {
        rightClickHoldTask = null;
        holdingRight = false;
        rightHoldTimeStart = 0L;
        timeRightHeld = 0L;
        setBlocking(false);
        cancelBlockDrainTask();
    }

    /**
     * Ends holding start-click input, restoring item stacks appropriately.
     */
    public void endHoldingRight() {
        holdingRight = false;
        timeRightHeld = System.currentTimeMillis() - rightHoldTimeStart;
        setBlocking(false);
        cancelBlockDrainTask();
        setItemStackInHand(offItemStackAtTimeOfHold, false);
        if (!mainItemStackAtTimeOfHold.isEmpty() && !threwItem && !holdingUmbralItemInMainHand())
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

        sneakTask = TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            inputExecutionTree::restartTimeoutTimer,
            null,
            0, 50,
            SwordPlayer.class, "startSneaking",
            new PredicateRunnablePair(
                () -> !sneaking,
                () -> {
                    if (timeSneakHeld < 162)
                        act(InputType.SHIFT_TAP);
                    else
                        act(InputType.SHIFT_HOLD);
                    resetSneaking();
                }
            )
        );
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

        SwordScheduler.runBukkitTaskLater(
            () -> swappingInInv = false,
            50, TimeUnit.MILLISECONDS
        );
    }

    /**
     * Marks that the player is currently dropping items in inventory.
     * Resets the flag shortly after (1 tick).
     */
    public void setDroppingInInv() {
        droppingInInv = true;
        SwordScheduler.runBukkitTaskLater(
            () -> droppingInInv = false,
            50, TimeUnit.MILLISECONDS
        );
    }

    public void incrementNumDummies() {
        curNumDummies++;
    }

    public void decrementNumDummies() {
        curNumDummies = Math.max(0, curNumDummies - 1);
    }
}
