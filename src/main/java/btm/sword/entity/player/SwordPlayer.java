package btm.sword.entity.player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.destroystokyo.paper.profile.PlayerProfile;

import btm.sword.action.core.BlockAction;
import btm.sword.action.core.UmbralBladeAction;
import btm.sword.action.skill.Skill;
import btm.sword.action.skill.SkillId;
import btm.sword.action.skill.SkillIds;
import btm.sword.action.skill.SkillRegistry;
import btm.sword.action.skill.container.PlayerSkillContainer;
import btm.sword.action.skill.container.SkillSlot;
import btm.sword.action.skill.container.SkillSlotActionFactory;
import btm.sword.action.skill.container.SkillSlotState;
import btm.sword.action.skill.type.ActiveSkill;
import btm.sword.action.skill.type.impl.charge.ChargeAction;
import btm.sword.action.skill.type.impl.charge.ChargeSession;
import btm.sword.action.throwing.ThrowAction;
import btm.sword.action.throwing.types.DroppedItem;
import btm.sword.config.Config;
import btm.sword.entity.aspect.AspectType;
import btm.sword.entity.base.Combatant;
import btm.sword.entity.base.SwordEntity;
import btm.sword.entity.mob.Dummy;
import btm.sword.input.ActivationContext;
import btm.sword.input.InputAction;
import btm.sword.input.InputActionExecutor;
import btm.sword.input.InputBuffer;
import btm.sword.input.InputExecutionTree;
import btm.sword.input.InputListener;
import btm.sword.input.InputRegistrar;
import btm.sword.input.InputType;
import btm.sword.item.core.ItemClass;
import btm.sword.item.core.ItemClassifier;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.item.core.KeyRegistry;
import btm.sword.item.core.StorageCategory;
import btm.sword.item.core.SwordItemType;
import btm.sword.item.special.AbilitySlotManager;
import btm.sword.item.special.NonMovableItem;
import btm.sword.item.special.SlotAnchoredItem;
import btm.sword.menu.ArtifactPouchMenu;
import btm.sword.menu.CurrencyMenu;
import btm.sword.menu.InventoryMenuManager;
import btm.sword.menu.MainMenu;
import btm.sword.menu.MaterialPouchMenu;
import btm.sword.menu.PlayerMenuManager;
import btm.sword.playerdata.PlayerData;
import btm.sword.playerdata.PlayerStorage;
import btm.sword.runtime.scheduler.PredicateRunnablePair;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.runtime.scheduler.TimeArbiter;
import btm.sword.scene.animation.CutsceneInputHandler;
import btm.sword.scene.camera.CameraController;
import btm.sword.ui.hud.HudOverrideManager;
import btm.sword.ui.hud.HudRenderState;
import btm.sword.util.display.DisplayUtil;
import btm.sword.util.misc.Debug;
import btm.sword.util.misc.SwordTimeUnit;
import btm.sword.util.prefab.Prefab;
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

    /** Maximum number of individual material items that can be stored across all types. */
    public static final int MATERIAL_SLOTS_TOTAL = 96;

    private final PlayerMenuManager playerMenuManager;
    private final SlotAnchoredItem menuButton;
    private SlotAnchoredItem currencyStorageButton;
    private SlotAnchoredItem materialStorageButton;
    private SlotAnchoredItem questStorageButton;
    private final SlotAnchoredItem shieldItem;
    private final SlotAnchoredItem chestplateItem;
    @Getter
    private final AbilitySlotManager abilitySlotManager;

    /** Active charge session for chargeable abilities, or {@code null} if not charging. */
    @Getter
    @Setter
    private ChargeSession activeCharge;

    /** Economy storage for materials, credits, and auto-pickup preferences. Loaded from and saved to the database. */
    private PlayerStorage playerStorage;

    private final Supplier<List<Component>> currencyLore =
        () -> List.of(Component.text(playerStorage.getSteelCredits() + " Steel Credits")
            .color(Config.SwordColor.TEXT_COOL)
            .decoration(TextDecoration.ITALIC, false));

    private final Supplier<List<Component>> materialLore =
        () -> List.of(Component.text(playerStorage.getTotalMaterialSlots() + " / " + MATERIAL_SLOTS_TOTAL + " slots")
            .color(Config.SwordColor.TEXT_COOL)
            .decoration(TextDecoration.ITALIC, false));

    private final int maxNumDummies = 3;
    private int curNumDummies = 0;
    private final HashSet<Dummy> yourDummies = new HashSet<>();

    private final InputExecutionTree inputExecutionTree;
    private final InputBuffer inputBuffer;
    private final long baseInputTimeoutMillis = 1400L;

    /** Current input context; controls which action paths are visible in the execution tree. */
    @Setter
    private ActivationContext activationContext = ActivationContext.NORMAL;

    /** Set to true by the damage pipeline when a healing channel is interrupted by incoming damage. */
    @Getter
    @Setter
    private boolean channelInterrupted = false;

    private boolean performedDropAction;
    @Getter
    @Setter
    private ItemStack lastHeldItemBeforeDrop = ItemStack.of(Material.AIR);
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

    @Getter
    @Setter
    private TimeArbiter.TaskHandle healChannelTask;

    private int thrownItemIndex;

    private InventoryMode inventoryMode = InventoryMode.NONE;

    /** True while the scene overlay (glass-pane fill + invisibility) is active. Suppresses inventory upkeep. */
    private boolean inSceneOverlay = false;

    /** Snapshot of inventory slots 0–35 taken when the scene overlay is entered; restored on exit. */
    private ItemStack[] savedInventory = null;

    /** Snapshot of chestplate slot (38) taken when the scene overlay is entered; restored on exit. */
    private ItemStack savedSceneChestplate = null;

    /** Snapshot of offhand slot (40) taken when the scene overlay is entered; restored on exit. */
    private ItemStack savedOffhand = null;

    /**
     * True while the creative dev mode (clear inventory, wooden axe) is active.
     * Suppresses all item upkeep except the menu button (maintained at slot 17).
     * -- GETTER --
     * Returns
     * if the player is currently in creative dev mode.
     */
    private boolean inCreativeDevMode = false;

    /**
     * All {@link SlotAnchoredItem}s owned by this player that participate in periodic upkeep.
     * Rebuilt via {@link #updateManagedItems()} whenever the set of items changes.
     * Call {@link #setAllAnchoredItemUpkeep(boolean)} to toggle upkeep for all items at once.
     */
    private List<SlotAnchoredItem> managedItems;

    /** Snapshot of inventory slots 0–35 taken when creative dev mode is entered; restored on exit. */
    private ItemStack[] savedCreativeDevInventory = null;

    /** Snapshot of chestplate slot (38) taken when creative dev mode is entered; restored on exit. */
    private ItemStack savedCreativeDevChestplate = null;

    /** Snapshot of offhand slot (40) taken when creative dev mode is entered; restored on exit. */
    private ItemStack savedCreativeDevOffhand = null;

    @Getter
    @Setter
    private CameraController activeCameraController;

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
     * @param data             the {@link PlayerData} containing extended player info
     */
    public SwordPlayer(LivingEntity associatedEntity, PlayerData data) {
        super(associatedEntity, data.getCombatProfile());
        playerStorage = data.getPlayerStorage();
        player = (Player) self;
        profile = player.getPlayerProfile();
        username = profile.getName();

        ItemStack temp = ItemStack.of(Material.PLAYER_HEAD);
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

        currencyStorageButton = buildStorageButton(StorageCategory.CURRENCY, currencyLore);
        materialStorageButton = buildStorageButton(StorageCategory.MATERIAL, materialLore);
        questStorageButton = buildStorageButton(StorageCategory.QUEST, List::of);

        shieldItem = new SlotAnchoredItem(
            new ItemStackBuilder(Material.SHIELD).build(),
            40,
            Material.SHIELD
        );

        chestplateItem = new SlotAnchoredItem(
            new ItemStackBuilder(Material.NETHERITE_CHESTPLATE)
                .hideAll()
                .stripAttributeModifiers()
                .build(),
            38,
            Material.NETHERITE_CHESTPLATE
        );

        abilitySlotManager = new AbilitySlotManager(this);
        abilitySlotManager.initialize();

        // TODO: remove once test abilities are replaced with real found items
        PlayerSkillContainer skillContainer = getCombatProfile().getPlayerSkillContainer();
        skillContainer.discover(SkillIds.TEST_ALPHA);
        skillContainer.discover(SkillIds.TEST_BETA);
        skillContainer.discover(SkillIds.TEST_GAMMA);

        updateManagedItems();

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

        inventoryMode = InventoryMode.NONE;
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

        boolean noHealChannelTask = healChannelTask == null;
        boolean sneakingAndHolding = isSneakingAndHoldingRight();
        boolean canHeal = canPerformHealAction();
        boolean notMaxShards = aspects.shards().belowMax();
        boolean holdingBlade = holdingUmbralBlade();

        if (noHealChannelTask &&
            sneakingAndHolding &&
            canHeal &&
            notMaxShards &&
            holdingBlade) {

            Debug.umbral("BEGIN HEAL CHANNEL");
            UmbralBladeAction.beginHealChannel(this);
        }
    }

    /**
     * Called when the player entity spawns or respawns.
     * Extends {@link Combatant#onSpawn()}.
     */
    @Override
    public void onSpawn() {
        super.onSpawn();
        joinTeam(btm.sword.entity.team.SwordTeam.BLUE);
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
        if (activeCameraController != null) {
            activeCameraController.stop();
        }
        if (getUmbralBlade() != null) {
            getUmbralBlade().dispose();
            setUmbralBlade(null);
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
     * {@link btm.sword.action.core.BlockAction#startBlock}.
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

    /** Returns {@code true} if the player is both sneaking and currently blocking (holding right). */
    public boolean isSneakingAndHoldingRight() {
        return player.isSneaking() && player.isBlocking();
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

        if (activationContext == ActivationContext.CUTSCENE) {
            CutsceneInputHandler.handle(this, input);
            return;
        }

        if (activationContext.equals(ActivationContext.CHANNELING)) {
            activationContext = ActivationContext.NORMAL;
        }

        Debug.input("Pre-Ability Check Message, cur path=" + inputExecutionTree.getPlainTextInputSequence()
            + "\n           input=" + input
            + "\n           holdingAbilityItem?=" + !notHoldingAbilityItem()
            + "\n           isAtRoot=" + isAtRoot()
            + "\n           notHoldingChargeable=" + !ChargeAction.isHoldingChargeable(this));

        if (isAtRoot() && !ChargeAction.isHoldingChargeable(this) && handleAbilityInput(input)) {
            resetTree();
            return;
        }

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
        } else if (input == InputType.SHIFT) {
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
        } else if (input == InputType.SHIFT_HOLD) {
            long minTime = inputExecutionTree.getMinHoldLengthOfNext(input);
            if (minTime == -1 || timeSneakHeld < minTime) {
                return;
            }
        }

        Debug.input("Line before step is resolved.");

        // The execution trie is only traversed if the code makes it here!
        InputExecutionTree.InputNode node = inputExecutionTree.step(input);

        if (node == null)
            return;
        else if (node.isDisplay())
            displayInputSequence();

        InputAction action = inputExecutionTree.getNextAction();

        if (action != null) {
            // The simplest way for charge action to pass is simply to let it step using the
            // Left of a basic attack, but just block the action
            if (isAtRoot() && input == InputType.LEFT && ChargeAction.isHoldingChargeable(this)) return;

            InputActionExecutor.execute(action, this);
        }

        Consumer<SwordPlayer> internalAction = node.getInternalAction();

        if (internalAction != null) {
            internalAction.accept(this);
        }
    }

    /** Returns {@code true} if the player's currently held item has an active cooldown. */
    public boolean isHeldItemOnCooldown() {
        return getItemStackInHand(true) instanceof ItemStack item && player.getCooldown(item) > 0;
    }

    private boolean handleAbilityInput(InputType input) {
        int heldSlot = player.getInventory().getHeldItemSlot();

        // Gate: is this even an ability slot? If not, let the normal input tree handle it.
        SwordItemType itemType = abilitySlotManager.getActiveTypeForHeldSlot(heldSlot);
        if (itemType == null) return false;

        // From here on, this IS an ability slot (return true to consume the input)
        if (input == InputType.LEFT) {
            if (isHeldItemOnCooldown()) return true;

            SkillSlot slot = itemType == SwordItemType.ACTIVE_1 ? SkillSlot.ACTIVE_1 : SkillSlot.ACTIVE_2;

            InputAction action = SkillSlotActionFactory.create(this, slot, false);
            if (action == null) return true;

            SkillId equippedId = getCombatProfile().getPlayerSkillContainer().getEquipped(slot);
            Skill skill = SkillRegistry.get(equippedId);
            if (!(skill instanceof ActiveSkill active) || !active.canPerform(this)) return true;

            SkillSlotState state = getCombatProfile().getPlayerSkillContainer().getSlotState(slot);
            if (System.currentTimeMillis() < state.cooldownExpiresAt()) return true;

            InputActionExecutor.execute(action, this);

            abilitySlotManager.consumeUse(heldSlot);
            long expiry = System.currentTimeMillis() + active.calculateCooldown(this);
            SkillSlotState current = getCombatProfile().getPlayerSkillContainer().getSlotState(slot);
            getCombatProfile().getPlayerSkillContainer().setSlotState(slot,
                new SkillSlotState(current.remainingUses(), current.remainingDurability(), expiry));

            return true;
        } else {
            return false;
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
        } else {
            player.setLevel(curFormVal);
            formProgress = 0;
        }
        player.setExp(Math.max(0.0f, Math.min(1.0f, formProgress))); // clamp between 0 and 1
        prevFormVal = curFormVal;
    }

    private SlotAnchoredItem buildStorageButton(StorageCategory category, Supplier<List<Component>> lore) {
        return new SlotAnchoredItem(
            new ItemStackBuilder(category.material())
                .hideAll()
                .name(category.displayName())
                .lore(lore.get())
                .tagStorageButton(category)
                .tag(KeyRegistry.ITEM_CLASS_KEY, PersistentDataType.STRING, ItemClass.BLOCKED.name())
                .build(),
            category.slot(),
            KeyRegistry.STORAGE_BUTTON_KEY
        );
    }

    private void refreshStorageLore(SlotAnchoredItem button, Supplier<List<Component>> lore) {
        ItemStack item = player.getInventory().getItem(button.getTargetSlot());
        if (item != null && !item.isEmpty()) {
            item.lore(lore.get());
        }
    }

    /**
     * Rebuilds all anchored inventory buttons (menu button and storage shortcuts)
     * and forces them back into their slots. Call this after config hot-reloads to
     * pick up material or colour changes immediately.
     */
    public void reloadInventoryButtons() {
        currencyStorageButton = buildStorageButton(StorageCategory.CURRENCY, currencyLore);
        materialStorageButton = buildStorageButton(StorageCategory.MATERIAL, materialLore);
        questStorageButton = buildStorageButton(StorageCategory.QUEST, List::of);

        updateManagedItems();

        menuButton.restore(player);
        currencyStorageButton.restore(player);
        materialStorageButton.restore(player);
        questStorageButton.restore(player);
    }

    /**
     * Enters the scene overlay: saves the player's inventory (slots 0–35), fills all slots with
     * blue stained-glass panes (keeping the menu button in slot 8), hides the player from others,
     * and deactivates the umbral blade. Also suppresses {@link #inventoryUpkeep()} until exited.
     * <p>
     * Call from a {@link btm.sword.scene.camera.CameraController}'s {@code onStart()} hook.
     * Pair every call with {@link #exitSceneOverlay()}.
     * </p>
     */
    public void enterSceneOverlay() {
        if (inSceneOverlay) return;

        if (Debug.SPECIAL_ITEM_CHECKS_ENABLED) {
            setAllAnchoredItemUpkeep(false);

            // Save hotbar + main inventory (0–35), chestplate (38), and offhand (40)
            savedInventory = new ItemStack[36];
            for (int i = 0; i < 36; i++) {
                savedInventory[i] = player.getInventory().getItem(i);
            }
            savedSceneChestplate = player.getInventory().getItem(38);
            savedOffhand = player.getInventory().getItem(40);

            // Clear hotbar (0–8); glass panes in main inventory (9–35) with menu button in center (22)
            ItemStack wallItem = MainMenu.WALL.getItemProvider().get();
            ItemStack glass = new ItemStackBuilder(wallItem.getType())
                .name(wallItem.displayName())
                .hideAll()
                .tag(KeyRegistry.NON_MOVABLE_KEY, PersistentDataType.BOOLEAN, true)
                .build();
            for (int i = 0; i < 9; i++) {
                player.getInventory().setItem(i, null); // TODO: Find out why sometimes the soul link stays in the inventory...
            }
            for (int i = 9; i < 36; i++) {
                if (i == 22) {
                    player.getInventory().setItem(22, menuButton.getItemStack());
                } else {
                    player.getInventory().setItem(i, glass);
                }
            }

            // Clear sword (offhand shield) and chestplate armor slot
            player.getInventory().setItem(38, null);
            player.getInventory().setItem(40, null);

            ableToPickup = false;
        }

        player.setInvisible(true);

        deactivateUmbralBlade();

        inSceneOverlay = true;
    }

    /**
     * Exits the scene overlay: restores saved inventory contents (slots 0–35 and offhand),
     * makes the player visible again, and re-enables {@link #inventoryUpkeep()}.
     * <p>
     * Call from a {@link btm.sword.scene.camera.CameraController}'s {@code onStop()} hook.
     * </p>
     */
    public void exitSceneOverlay() {
        if (!inSceneOverlay) return;
        inSceneOverlay = false;
        ableToPickup = true;

        if (savedInventory != null) {
            setAllAnchoredItemUpkeep(true);

            for (int i = 0; i < 36; i++) {
                player.getInventory().setItem(i, savedInventory[i]);
            }
            savedInventory = null;

            player.getInventory().setItem(38, savedSceneChestplate);
            savedSceneChestplate = null;

            player.getInventory().setItem(40, savedOffhand);
            savedOffhand = null;
        }

        player.setInvisible(false);
        activateUmbralBlade();
    }

    /**
     * Called when the player left-clicks a glass pane slot while the scene overlay is active.
     * Stops the active camera controller to exit the scene.
     * <p>
     * Override point for future scene-specific exit behaviour (e.g. fade-out, return animation).
     * </p>
     */
    public void onSceneOverlayClickExit() {
        if (activeCameraController != null) {
            activeCameraController.stop();
        }
    }

    /**
     * Enters creative dev mode: saves the player's inventory (slots 0–35, chestplate, offhand),
     * clears all slots including armor, places the main menu button at slot 17 (one row above
     * the hotbar), puts a wooden axe in slot 0, disables special item checks, and enables block
     * placing. While active, {@link #inventoryUpkeep()} only maintains the menu button at slot 17.
     */
    public void enterCreativeDevMode() {
        if (inCreativeDevMode) return;

        player.setGameMode(GameMode.CREATIVE);
        setActivationContext(ActivationContext.BUILDING);
//        SwordScheduler.runBukkitTaskLater(() -> ) // TODO: Deactivate Umbral Blade

        savedCreativeDevInventory = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            savedCreativeDevInventory[i] = player.getInventory().getItem(i);
        }
        savedCreativeDevChestplate = player.getInventory().getItem(38);
        savedCreativeDevOffhand = player.getInventory().getItem(40);

        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, null);
        }
        player.getInventory().setItem(38, null);
        player.getInventory().setItem(40, null);

        // Place menu button one row above the hotbar (slot 17) to free up the hotbar
        player.getInventory().setItem(17, menuButton.getItemStack());
        player.getInventory().setItem(0, ItemStack.of(Material.WOODEN_AXE));
        player.getInventory().setItem(1, ItemStack.of(Material.FIREWORK_ROCKET));
        player.getInventory().setItem(38, ItemStack.of(Material.ELYTRA));

        setAllAnchoredItemUpkeep(false);
        Debug.SPECIAL_ITEM_CHECKS_ENABLED = false;
        Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING = true;

        inCreativeDevMode = true;
    }

    /**
     * Exits creative dev mode: restores the saved inventory (slots 0–35, chestplate, offhand),
     * re-enables special item checks, disables block placing, and reloads inventory buttons
     * (restoring all anchored items to their normal slots).
     */
    public void exitCreativeDevMode() {
        if (!inCreativeDevMode) return;

        player.setGameMode(GameMode.SURVIVAL);
        setActivationContext(ActivationContext.NORMAL);

        inCreativeDevMode = false;

        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, savedCreativeDevInventory != null ? savedCreativeDevInventory[i] : null);
        }
        savedCreativeDevInventory = null;

        player.getInventory().setItem(38, savedCreativeDevChestplate);
        savedCreativeDevChestplate = null;

        player.getInventory().setItem(40, savedCreativeDevOffhand);
        savedCreativeDevOffhand = null;

        setAllAnchoredItemUpkeep(true);
        Debug.SPECIAL_ITEM_CHECKS_ENABLED = true;
        Config.World.BLOCK_INTERACTION_ALLOW_BLOCK_PLACING = false;

        reloadInventoryButtons();
        shieldItem.restore(player);
        chestplateItem.restore(player);
    }

    /**
     * Rebuilds {@link #managedItems} from the current set of anchored item fields.
     * Must be called after construction and after {@link #reloadInventoryButtons()}.
     */
    private void updateManagedItems() {
        List<SlotAnchoredItem> items = new ArrayList<>(List.of(
            shieldItem, chestplateItem, menuButton,
            currencyStorageButton, materialStorageButton, questStorageButton
        ));
        items.addAll(abilitySlotManager.getSlotItems());
        managedItems = List.copyOf(items);
    }

    /**
     * Enables or disables periodic upkeep for every item in {@link #managedItems}.
     * Use this as the single call-site when entering or exiting any mode that should
     * suppress automatic item replacement (cutscenes, creative dev mode, etc.).
     *
     * @param enabled {@code true} to re-enable replacement; {@code false} to suppress it
     */
    public void setAllAnchoredItemUpkeep(boolean enabled) {
        managedItems.forEach(item -> item.setUpkeepEnabled(enabled));
    }

    private void inventoryUpkeep() {
        if (inSceneOverlay) return;

        if (inCreativeDevMode) {
            // In creative dev mode, only maintain the menu button at slot 17 (above the hotbar)
            ItemStack at17 = player.getInventory().getItem(17);
            if (at17 == null || !KeyRegistry.hasKey(at17, KeyRegistry.MAIN_MENU_BUTTON_KEY)) {
                player.getInventory().setItem(17, menuButton.getItemStack());
            }
            return;
        }

        for (SlotAnchoredItem item : managedItems) {
            if (item.isUpkeepEnabled() && !item.isSatisfied(player)) {
                if (KeyRegistry.hasKey(item.getItemStack(), KeyRegistry.MAIN_MENU_BUTTON_KEY) ||
                    Debug.SPECIAL_ITEM_CHECKS_ENABLED) {
                    item.restore(player);
                }
            }
        }

        if (currencyStorageButton.isUpkeepEnabled()) refreshStorageLore(currencyStorageButton, currencyLore);
        if (materialStorageButton.isUpkeepEnabled()) refreshStorageLore(materialStorageButton, materialLore);

        if (getUmbralBlade() != null && !getUmbralBlade().getLinkAnchor().isSatisfied(player)) {
            getUmbralBlade().getLinkAnchor().restore(player);
        }
    }

    /**
     * Dispatches an input event for items that carry a Sword-managed type key.
     * This is the primary point of contact for typed-item input handling, called by
     * {@link InputListener} before any class-based filtering or
     * routing through {@link #act(InputType)}.
     * <p>
     * If this method returns {@code true}, the caller must cancel the originating Bukkit
     * event and skip all further input processing — even if no action was taken (e.g.
     * shift is suppressed for the menu button without opening the menu).
     * </p>
     *
     * @param itemStack the item stack involved in the input; must not be null
     * @param input     the input type being evaluated
     * @return {@code true} if the input was fully handled and the event should be cancelled
     */
    public boolean handleItemInteraction(ItemStack itemStack, InputType input) {
        if (KeyRegistry.hasKey(itemStack, KeyRegistry.MAIN_MENU_BUTTON_KEY)) {
            if (input != InputType.SHIFT) {
                InventoryMenuManager.openMenu(MainMenu.class, this);
            }
            return true;
        }

        StorageCategory category = StorageCategory.fromItem(itemStack);
        if (category != null) {
            openMenuForCategory(category);
            return true;
        }
        return false;
    }

    /**
     * Opens the menu associated with the given {@link StorageCategory}.
     * Shared by {@link #handleItemInteraction} and {@link #tryOpenMenuForItem}.
     *
     * @param category the storage category whose menu to open
     */
    private void openMenuForCategory(StorageCategory category) {
        switch (category) {
            case MATERIAL -> InventoryMenuManager.openMenu(MaterialPouchMenu.class, this);
            case CURRENCY -> InventoryMenuManager.openMenu(CurrencyMenu.class, this);
            case QUEST -> InventoryMenuManager.openMenu(ArtifactPouchMenu.class, this);
        }
    }

    /**
     * If {@code item} is a Sword UI button (main menu or any storage shortcut), opens its
     * corresponding menu and returns {@code true}. Returns {@code false} otherwise.
     *
     * <p>Intended for inventory-click context where there is no SHIFT carve-out.
     * The main-menu SHIFT suppression is handled separately in {@link #handleItemInteraction}.</p>
     *
     * @param item the item to inspect; may be null
     * @return {@code true} if a menu was opened
     */
    private boolean tryOpenMenuForItem(ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        if (KeyRegistry.hasKey(item, KeyRegistry.MAIN_MENU_BUTTON_KEY)) {
            InventoryMenuManager.openMenu(MainMenu.class, this);
            return true;
        }
        StorageCategory category = StorageCategory.fromItem(item);
        if (category == null) return false;
        openMenuForCategory(category);
        return true;
    }

    /**
     * Handles inventory click events to interact with the player's inventory.
     * Can be customized to modify behavior based on click type, inventory, and slot.
     *
     * @param e the inventory click event to handle
     * @return true if the event was handled and should be cancelled, false otherwise
     */
    public boolean handleInventoryInput(InventoryClickEvent e) {
        ClickType clickType = e.getClick();
        InventoryAction action = e.getAction();
        ItemStack onCursor = e.getCursor();
        ItemStack clicked = e.getCurrentItem();

        if (clicked == null) {
            return false;
        }

        // Any Sword UI button (main menu or storage shortcuts) opens its menu.
        // Checked before the scene overlay guard — UI buttons are always accessible.
        if (tryOpenMenuForItem(clicked) || tryOpenMenuForItem(onCursor)) {
            return true;
        }

        // Scene overlay is active — cancel all inventory interactions except the menu button above.
        // Left-clicking an interactable (non-NON_MOVABLE) slot fires the scene-exit stub.
        // When special item checks are disabled, all overlay restrictions are bypassed.
        if (inSceneOverlay && Debug.SPECIAL_ITEM_CHECKS_ENABLED) {
            if (clickType == ClickType.LEFT && !clicked.getType().isAir() &&
                !KeyRegistry.hasKey(clicked, KeyRegistry.NON_MOVABLE_KEY) &&
                e.getClickedInventory() == player.getInventory()) {
                onSceneOverlayClickExit();
            }
            return true;
        }


        // Protect non-movable items on cursor from being placed into any slot
        if (NonMovableItem.isNonMovable(onCursor)) {
            return true;
        }

        // Protect non-movable items in slots from being moved or interacted with
        if (NonMovableItem.isNonMovable(clicked)) {
            return true;
        }

        // Protect non-movable items in hotbar slots from being swapped via number keys
        if (clickType == ClickType.NUMBER_KEY) {
            int hotbarSlot = e.getHotbarButton();
            if (hotbarSlot >= 0 && NonMovableItem.isNonMovable(player.getInventory().getItem(hotbarSlot))) {
                return true;
            }
        }

        // Protect non-movable items in the offhand slot from being swapped via F key
        if (clickType == ClickType.SWAP_OFFHAND && NonMovableItem.isNonMovable(player.getInventory().getItem(40))) {
            return true;
        }

        // Block all Q / Ctrl+Q drops from inventory — players cannot drop items via inventory
        return action == InventoryAction.DROP_ONE_SLOT || action == InventoryAction.DROP_ALL_SLOT
            || action == InventoryAction.DROP_ONE_CURSOR || action == InventoryAction.DROP_ALL_CURSOR;
    }

    /**
     * Spawns an appropriate world drop for an item leaving the player's inventory.
     * <p>
     * {@link ItemClass#THROWABLE} items (weapons, axes, etc.) become {@link DroppedItem}s with
     * custom physics so they must be picked up manually from the ground. All other items become
     * standard Bukkit item entities.
     * </p>
     * <p>
     * Called both from {@link #handleInventoryInput} (Q/Ctrl+Q in inventory) and from
     * {@link InputListener#onPlayerDropEvent} when the player drags an item
     * outside the inventory window.
     * </p>
     *
     * @param item the stack to drop into the world; must not be null or empty
     */
    public void spawnInventoryDrop(ItemStack item) {
        Location dropLocation = locFromFlatDir(1.5);

        if (ItemClassifier.classify(item) == ItemClass.THROWABLE) {
            new DroppedItem(dropLocation, new Vector(0, 0, 0), item).register();
        } else {
            Item itemDrop = player.getWorld().dropItem(dropLocation, item);
            itemDrop.setPickupDelay(20);
        }
    }

    /** Synchronises the player's visual stat bars (health, food, experience) with current aspect values. */
    public void updateVisualStats() {
        player.setAbsorptionAmount(aspects.toughnessCur());
        HudRenderState state = new HudRenderState(
            Math.max(2, 2 * aspects.shardsCur()),
            (int) (20 * (aspects.soulfireCur() / aspects.soulfireMaxVal())),
            5.0f,
            player.getRemainingAir()
        );
        HudOverrideManager.apply(player, HudOverrideManager.resolve(player, state));
    }

    /**
     * Cycles through four HUD effect visuals for testing purposes, each lasting 5 seconds:
     * wither hearts, poisoned hearts, empty food bar, then bubbles animating full to empty to full.
     */
    public void testHudSequence() {
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0, false, false));

        SwordScheduler.after(5, TimeUnit.SECONDS, () -> {
            player.removePotionEffect(PotionEffectType.WITHER);
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0, false, false));
        })
        .andThen(5, TimeUnit.SECONDS, () -> {
            player.removePotionEffect(PotionEffectType.POISON);
            HudOverrideManager.register(player, "hud_test", 200,
                (p, state) -> new HudRenderState(state.health(), 0, 0.0f, state.air()));
        })
        .andThen(5, TimeUnit.SECONDS, () -> {
            HudOverrideManager.clear(player, "hud_test");
            int maxAir = player.getMaximumAir();
            int[] it = {0};
            TimeArbiter.runFixedIterationTaskTimer(
                () -> {
                    int step = it[0]++;
                    int air = step <= 50
                        ? (int) ((maxAir - 1) * (1.0 - step / 50.0))
                        : (int) ((maxAir - 1) * (step - 50) / 50.0);
                    player.setRemainingAir(air);
                },
                null, 0, 50, 100,
                SwordPlayer.class, "testHudSequence",
                () -> player.setRemainingAir(maxAir)
            );
        });
    }

    /**
     * Returns the underlying {@link Player} entity for this SwordPlayer.
     *
     * @return the Bukkit player entity
     */
    public Player player() {
        return player;
    }

    /** Returns a player-head item stack with the player's skin and the provided display name and lore. */
    public ItemStack getPlayerHeadItemWithCustomText(Component title, List<Component> lore) {
        ItemStack temp = ItemStack.of(Material.PLAYER_HEAD);
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

    /** Clears the active item and applies a cooldown to the off-hand item for the given number of ticks. */
    public void disableShield(int ticks) {
        self.clearActiveItem();
        player.setCooldown(getItemStackInHand(false), ticks);
    }

    /** Returns {@code true} if the player's off-hand item has an active cooldown (shield is on cooldown). */
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

    /** Returns {@code true} if the player's currently held slot is not an ability slot. */
    public boolean notHoldingAbilityItem() {
        return !abilitySlotManager.isAbilityHeldSlot(player.getInventory().getHeldItemSlot());
    }

    /** Returns {@code true} if the player is in a normal, non-channelling, non-ability-item state. */
    public boolean normalNonAbilityState() {
     return normalActState() &&
        !ChargeAction.isHoldingChargeable(this) &&
        notHoldingAbilityItem();
    }

    /** Returns {@code true} if the player's activation context is {@link ActivationContext#NORMAL}. */
    public boolean normalActState() {
        return activationContext == ActivationContext.NORMAL;
    }

    /** Returns true while the player is in throw-ready posture (between throwReady and release/cancel). */
    public boolean throwingState() {
        return activationContext == ActivationContext.THROWING;
    }

    /** Returns {@code true} if the player is throwing or in a normal non-umbral state. */
    public boolean throwingNonUmbralState() {
        return (throwingState() || nonUmbralState());
    }

    /** Returns {@code true} if the player can initiate a throw (non-umbral and not in an ability slot). */
    public boolean canBeginThrow() {
        return nonUmbralState() && notHoldingAbilityItem();
    }

    /** Returns {@code true} if the player is in normal state without holding any umbral weapon. */
    public boolean nonUmbralState() {
        return normalActState() && !holdingSoulLink() && !holdingUmbralBlade();
    }

    /** Returns {@code true} if the player is in normal state while holding a soul link. */
    public boolean soulLinkState() {
        return normalActState() && holdingSoulLink();
    }

    /** Returns {@code true} if the player is in normal state while holding the UmbralBlade. */
    public boolean umbralBladeState() {
        return normalActState() && holdingUmbralBlade();
    }

    /** Returns {@code true} if the player is in normal state while holding any umbral weapon. */
    public boolean umbralState() {
        return normalActState() && (holdingSoulLink() || holdingUmbralBlade());
    }

    /**
     * Returns {@code true} if the player's held item has the given persistent data key.
     *
     * <p>During a drop event the item is temporarily removed from the hand before the
     * event is cancelled. When {@link #isPerformedDropAction()} is {@code true}, the
     * pre-drop item stored in {@link #getLastHeldItemBeforeDrop()} is checked instead,
     * matching the same pattern used by {@link #holdingUmbralBlade()}.</p>
     */
    public boolean heldItemHasKey(NamespacedKey key) {
        ItemStack item = isPerformedDropAction() ? getLastHeldItemBeforeDrop() : getItemStackInHand(true);
        return KeyRegistry.hasKey(item, key);
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
            .append(Component.text("  «", TextColor.color(80, 80, 80), TextDecoration.BOLD)), null);
    }

    /** Displays a title showing the player's current soulfire versus the required amount. */
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
        double timeToDisplay = timeLeft > 1000L ? (double) timeLeft / 1000 : timeLeft;
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
     * @param fadeIn duration of fade-in in milliseconds
     * @param duration duration to display the title in milliseconds
     * @param fadeOut duration of fade-out in milliseconds
     */
    public void displayTitle(@Nullable Component title, @Nullable Component subtitle, long fadeIn, long duration, long fadeOut) {
        if (title == null && subtitle == null) return;
        self.showTitle(Title.title(
                title == null ? Component.text("") : title,
                subtitle == null ? Component.text("") : subtitle,
                Title.Times.times(
                    Duration.ofMillis(fadeIn),
                    Duration.ofMillis(duration),
                    Duration.ofMillis(fadeOut))));
    }

    // TODO: Some way to cache this new itemStack and just send the equipment change each time
    /** Temporarily updates the held item's display name and material to show a contextual label. */
    public void itemNameDisplay(Component displayName, Material newMaterial) {
        ItemStack stack = getItemStackInHand(true).clone();
        if (newMaterial != null && (stack.isEmpty() || stack.getType().isAir())) {
            stack = ItemStack.of(newMaterial);
        }
        ItemMeta metaData = stack.getItemMeta();
        if (metaData == null) {
            return;
        }

        metaData.customName(displayName);

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
    public void itemNameDisplay(String toDisplay, TextColor color, @Nullable TextDecoration style, Material newMaterial) {
        if (style == null) {
            itemNameDisplay(Component.text(toDisplay, color), newMaterial);
        }
        else {
            itemNameDisplay(Component.text(toDisplay, color, style), newMaterial);
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

    /** Updates the targeted entity, removing target indicators from the previous one if it differs. */
    public void setTargetedEntity(SwordEntity newTarget) {
        if (newTarget == null) return;

        if (targetedEntity != null && (newTarget.getUuid() != targetedEntity.getUuid())) {
            removeTargetIndicators();
        }

        targetedEntity = newTarget;
    }

    // TODO: Apply more often and give more meaning (Issue #279)
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
                new Vector3f(0, 1.35f + ((float) Math.cos(ticks * Math.PI / 16) * 0.5f), 0),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
            )
        );
    }

    /** Removes all target indicators from the currently targeted entity. */
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

        ItemStack mainHand = getItemStackInHand(true);
        ItemStack offHand = getItemStackInHand(false);
        mainItemStackAtTimeOfHold = mainHand == null ? ItemStack.of(Material.AIR) : mainHand.clone();
        offItemStackAtTimeOfHold = offHand == null ? ItemStack.of(Material.AIR) : offHand.clone();

        indexOfRightHold = getCurrentInvIndex();

        if (!mainItemStackAtTimeOfHold.isEmpty() &&
            !holdingUmbralItemInMainHand() &&
            notHoldingAbilityItem() &&
            !heldItemHasKey(KeyRegistry.TEST_VOLUME_ATTACK_KEY)) {
            setItemStackInHand(ItemStack.of(Material.GUNPOWDER), true); // can change the logic here later
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
        if (!threwItem && !holdingUmbralItemInMainHand()) {
            if (abilitySlotManager.isAbilityHeldSlot(indexOfRightHold)) {
                abilitySlotManager.restoreSlot(indexOfRightHold);
            }
            else if (!mainItemStackAtTimeOfHold.isEmpty()) {
                setItemAtIndex(mainItemStackAtTimeOfHold, indexOfRightHold);
            }
        }
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
        if (index < 0 || index > 42) {
            return;
        }
        player.getInventory().setItem(index, item);
    }

    /**
     * Marks that the player is currently swapping items in inventory.
     * Resets to {@link InventoryMode#NONE} after ~1 tick.
     */
    public void setSwappingInInv() {
        inventoryMode = InventoryMode.SWAPPING;
        SwordScheduler.runBukkitTaskLater(
            () -> { if (inventoryMode == InventoryMode.SWAPPING) inventoryMode = InventoryMode.NONE; },
            50, TimeUnit.MILLISECONDS
        );
    }

    /**
     * Marks that the player is currently dropping items in inventory.
     * Resets to {@link InventoryMode#NONE} after ~2 ticks.
     */
    public void setDroppingInInv() {
        Debug.inventory(">> set dropping in inv");
        inventoryMode = InventoryMode.DROPPING;
        SwordScheduler.runBukkitTaskLater(
            () -> {
                Debug.inventory(">> no longer dropping in inv");
                if (inventoryMode == InventoryMode.DROPPING) inventoryMode = InventoryMode.NONE;
            },
            100, TimeUnit.MILLISECONDS
        );
    }

    /** Sets the inventory mode to {@link InventoryMode#SESSION} when the player opens a screen. */
    public void setInInventorySession(boolean active) {
        inventoryMode = active ? InventoryMode.SESSION : InventoryMode.NONE;
    }

    /** Returns {@code true} if the player currently has an inventory screen open. */
    public boolean isInInventorySession() {
        return inventoryMode == InventoryMode.SESSION;
    }

    /** Returns {@code true} if the player is in a momentary item-drop action. */
    public boolean isDroppingInInv() {
        return inventoryMode == InventoryMode.DROPPING;
    }

    /** Returns {@code true} if the player is in a momentary item-swap action. */
    public boolean isSwappingInInv() {
        return inventoryMode == InventoryMode.SWAPPING;
    }

    /** Marks that the player has performed a drop action; automatically resets after 100 ms. */
    public void setPerformedDropAction() {
        performedDropAction = true;
        SwordScheduler.runBukkitTaskLater(
            () -> performedDropAction = false,
            100, TimeUnit.MILLISECONDS
        );
    }

    /** Increments the count of training dummies currently owned by this player. */
    public void incrementNumDummies() {
        curNumDummies++;
    }

    /** Decrements the count of training dummies, clamped to a minimum of zero. */
    public void decrementNumDummies() {
        curNumDummies = Math.max(0, curNumDummies - 1);
    }
}
