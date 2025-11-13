package btm.sword.system.entity.umbral;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.system.SwordScheduler;
import btm.sword.system.action.utility.thrown.InteractiveItemArbiter;
import btm.sword.system.action.utility.thrown.ThrownItem;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.AttackType;
import btm.sword.system.attack.ItemDisplayAttack;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.system.entity.umbral.statemachine.UmbralState;
import btm.sword.system.entity.umbral.statemachine.UmbralStateMachine;
import btm.sword.system.entity.umbral.statemachine.state.AttackingHeavyState;
import btm.sword.system.entity.umbral.statemachine.state.AttackingQuickState;
import btm.sword.system.entity.umbral.statemachine.state.FlyingState;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;
import btm.sword.system.entity.umbral.statemachine.state.LungingState;
import btm.sword.system.entity.umbral.statemachine.state.RecallingState;
import btm.sword.system.entity.umbral.statemachine.state.ReturningState;
import btm.sword.system.entity.umbral.statemachine.state.SheathedState;
import btm.sword.system.entity.umbral.statemachine.state.StandbyState;
import btm.sword.system.entity.umbral.statemachine.state.WaitingState;
import btm.sword.system.entity.umbral.statemachine.state.WieldState;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.KeyRegistry;
import btm.sword.system.statemachine.State;
import btm.sword.system.statemachine.Transition;
import btm.sword.util.display.DisplayUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

// while flying and attacking on its own, no soulfire is reaped on attacks
// while in hand, higher soulfire intake on hit
@Getter
@Setter
public class UmbralBlade extends ThrownItem {
    private UmbralStateMachine bladeStateMachine;

    private Attack[] basicAttacks;
    private Attack[] heavyAttacks;

    private ItemStack link;
    private ItemStack blade;

    boolean active;

    private ItemStack weapon;

    private long lastActionTime = 0;
    private Location lastTargetLocation;

    // Transition request flags for external input
    private boolean toggleRequested = false;
    private boolean wieldRequested = false;
    private boolean attackQuickRequested = false;
    private boolean attackHeavyRequested = false;
    private boolean recallRequested = false;
    private boolean attackCompleted = false;

    private Vector3f scale = new Vector3f(0.85f, 1.3f, 1f);

    private static final int idleMovementPeriod = 5;
    private BukkitTask idleMovement;

    private final Predicate<UmbralBlade> endHoverPredicate;
    private final Runnable attackEndCallback;

    private static final int inputTimeout = 60;

    public UmbralBlade(Combatant thrower, ItemStack weapon) {
        super(thrower, display -> {
            display.setItemStack(weapon);
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf().rotationY((float) Math.PI / 2).rotateZ(-(float) Math.PI / (1.65f)),
                    new Vector3f(0.85f, 1.3f, 1f),
                    new Quaternionf()
            ));
            display.setPersistent(false);

            thrower.entity().addPassenger(display);
            display.setBillboard(Display.Billboard.FIXED);
        }, 5);

        this.weapon = weapon;

        generateUmbralItems();

        this.attackEndCallback = () -> {
            setState(UmbralState.WAITING);
        };

        loadBasicAttacks();
        loadHeavyAttacks();

        this.bladeStateMachine = new UmbralStateMachine(this, new SheathedState());
        initStateMachine();

        // Runnable to correctly set up blade
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bladeStateMachine.inState(new SheathedState())) {
                    bladeStateMachine.setState(new StandbyState());
                    bladeStateMachine.setState(new SheathedState());
                    cancel();
                }
            }
        }.runTaskTimer(Sword.getInstance(), 10L, 5L);

        endHoverPredicate = blade -> !bladeStateMachine.inState(new StandbyState());
    }

    private void initStateMachine() {
        // SHEATHED transitions
        bladeStateMachine.addTransition(new Transition<>(
            new SheathedState(),
            new StandbyState(),
            blade -> blade.isActive() && blade.toggleRequested,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            new SheathedState(),
            new WieldState(),
            blade -> blade.wieldRequested,
            blade -> {}
        ));

        // STANDBY transitions
        bladeStateMachine.addTransition(new Transition<>(
            new StandbyState(),
            new SheathedState(),
            blade -> blade.toggleRequested,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            new StandbyState(),
            new WieldState(),
            blade -> blade.wieldRequested,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            new StandbyState(),
            new AttackingQuickState(),
            blade -> blade.attackQuickRequested,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            new StandbyState(),
            new AttackingHeavyState(),
            blade -> blade.attackHeavyRequested,
            blade -> {}
        ));

        // WIELD transitions
        bladeStateMachine.addTransition(new Transition<>(
            new WieldState(),
            new StandbyState(),
            blade -> blade.toggleRequested,
            blade -> {}
        ));

        // ATTACKING transitions
        bladeStateMachine.addTransition(new Transition<>(
            new AttackingQuickState(),
            new WaitingState(),
            blade -> blade.attackCompleted,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            new AttackingHeavyState(),
            new WaitingState(),
            blade -> blade.attackCompleted,
            blade -> {}
        ));

        // WAITING transitions
        bladeStateMachine.addTransition(new Transition<>(
            new WaitingState(),
            new StandbyState(),
            blade -> true,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            new WaitingState(),
            new ReturningState(),
            UmbralBlade::isTooFarOrIdleTooLong,
            blade -> {}
        ));

        // RECALLING/RETURNING transitions
        bladeStateMachine.addTransition(new Transition<>(
            new RecallingState(),
            new SheathedState(),
            blade -> true,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            new ReturningState(),
            new SheathedState(),
            blade -> true,
            blade -> {}
        ));

        // FLYING transitions
        bladeStateMachine.addTransition(new Transition<>(
            new FlyingState(),
            new LodgedState(),
            UmbralBlade::hasHitTarget,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            new FlyingState(),
            new WaitingState(),
            UmbralBlade::hasLanded,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            new FlyingState(),
            new RecallingState(),
            blade -> blade.recallRequested,
            blade -> {}
        ));

        // LODGED transitions
        bladeStateMachine.addTransition(new Transition<>(
            new LodgedState(),
            new RecallingState(),
            blade -> blade.recallRequested,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            new LodgedState(),
            new WaitingState(),
            UmbralBlade::isTargetDestroyed,
            blade -> {}
        ));

        // LUNGING transitions
        bladeStateMachine.addTransition(new Transition<>(
            new LungingState(),
            new LodgedState(),
            UmbralBlade::hasHitTarget,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            new LungingState(),
            new WaitingState(),
            UmbralBlade::lungeMissed,
            blade -> {}
        ));
    }

    public boolean isOwnedBy(Combatant combatant) {
        return combatant.getUniqueId() == thrower.getUniqueId();
    }

    public boolean inState(UmbralState state) {
        return bladeStateMachine.getState().name().equals(state.name());
    }

    public void setState(UmbralState newState) {
        State<UmbralBlade> targetState = switch (newState) {
            case SHEATHED -> new SheathedState();
            case STANDBY -> new StandbyState();
            case WIELD -> new WieldState();
            case RECALLING -> new RecallingState();
            case RETURNING -> new ReturningState();
            case WAITING -> new WaitingState();
            case ATTACKING_QUICK -> new AttackingQuickState();
            case ATTACKING_HEAVY -> new AttackingHeavyState();
            case LUNGING -> new LungingState();
            case FLYING -> new FlyingState();
            case LODGED -> new LodgedState();
        };
        bladeStateMachine.setState(targetState);
    }

    public UmbralState getState() {
        String stateName = bladeStateMachine.getState().name();
        return UmbralState.valueOf(stateName);
    }

    // TODO: make this one method and make some data struct to store the bools or find a better soln.
    // reset 60 ms later (1.2 ticks) to give state machine time to process.
    public void requestToggle() {
        toggleRequested = true;
        SwordScheduler.runLater(() -> toggleRequested = false, inputTimeout, TimeUnit.MILLISECONDS);
    }

    public void requestWield() {
        wieldRequested = true;
        SwordScheduler.runLater(() -> wieldRequested = false, inputTimeout, TimeUnit.MILLISECONDS);
    }

    public void requestAttackQuick() {
        attackQuickRequested = true;
        SwordScheduler.runLater(() -> attackQuickRequested = false, inputTimeout, TimeUnit.MILLISECONDS);
    }

    public void requestAttackHeavy() {
        attackHeavyRequested = true;
        SwordScheduler.runLater(() -> attackHeavyRequested = false, inputTimeout, TimeUnit.MILLISECONDS);
    }

    public void requestRecall() {
        recallRequested = true;
        SwordScheduler.runLater(() -> recallRequested = false, inputTimeout, TimeUnit.MILLISECONDS);
    }

    private void clearRequestFlags() {
        toggleRequested = false;
        wieldRequested = false;
        attackQuickRequested = false;
        attackHeavyRequested = false;
        recallRequested = false;
        attackCompleted = false;
    }

    public void onTick() {
        if (!active) {
            thrower.message("Umbral Blade Not active.");
            return;
        }

        if (!thrower.isValid()) {
            thrower.message("Ending Umbral Blade");
            dispose();
        }

        if (thrower.getTicks() % 2 == 0) {
            if ((display == null || display.isDead()) && active) {
                thrower.message("Restarting UmbralBlade Display");
                restartDisplay();
            }
        }

        if (bladeStateMachine != null)
            bladeStateMachine.tick();
    }

    public void setDisplayTransformation(Class<? extends State<UmbralBlade>> state) {
        if (display == null) {
            restartDisplay();
            return;
        }
        DisplayUtil.setInterpolationValues(display, 0, 4);

        if (state == SheathedState.class) {
            display.setTransformation(new Transformation(
                new Vector3f(0.28f, -1.35f, -0.42f),
                new Quaternionf().rotationY((float) Math.PI / 2).rotateZ(-(float) Math.PI / 1.65f),
                scale,
                new Quaternionf()));
        }
        else if (state == StandbyState.class) {
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf().rotationY(0).rotateZ((float) Math.PI),
                scale,
                new Quaternionf()));
        }
        else if (state == FlyingState.class || state == RecallingState.class) {
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                scale,
                new Quaternionf()));
        }
        else {
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                scale,
                new Quaternionf()));
        }
    }

    public void hoverBehindWielder() {
        // Play unsheathing animation

        // follows player shoulder position smoothly
        DisplayUtil.itemDisplayFollowLerp(thrower, display,
            new Vector(0.7, 0.7, -0.5),
            5, 3, false, endHoverPredicate, this);

        startIdleMovement();
    }

    public void restartDisplay() {
        active = false;
        Bukkit.getScheduler().runTaskLater(Sword.getInstance(), () -> {
            if (!thrower.isValid()) return;

            Location loc = thrower.entity().getLocation();

            if (!loc.getChunk().isLoaded()) loc.getChunk().load();

            setup(false, 5);
        }, 2L);
    }

    @Override
    protected BukkitTask setup(boolean firstTime, int period) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (setupSuccessful) {
                    if (firstTime) afterSpawn();
                    cancel();
                    return;
                }
                try {
                    LivingEntity e = thrower.getSelf();
                    display = (ItemDisplay) e.getWorld().spawnEntity(e.getEyeLocation(), EntityType.ITEM_DISPLAY);
                    displaySetupInstructions.accept(display);
                    // >>>
                    reassignDisplayToAttacks();

                    active = true;
                    setupSuccessful = true;
                } catch(Exception e){
                    e.addSuppressed(e);
                }
            }
        }.runTaskTimer(Sword.getInstance(), 0L, period);
    }

    public void registerAsInteractableItem() {
        InteractiveItemArbiter.put(this);
    }

    public void unregisterAsInteractableItem() {
        InteractiveItemArbiter.remove(display);
    }

    public void updateSheathedPosition() {
        if (inState(UmbralState.WIELD)) return;

        long[] lastTimeSent = { System.currentTimeMillis() };

        int x = 3;
        for (int i = 0; i < x; i++) {
            SwordScheduler.runBukkitTaskLater(new BukkitRunnable() {
                @Override
                public void run() {
                    DisplayUtil.smoothTeleport(display, 2);
                    display.teleport(thrower.entity().getLocation().setDirection(thrower.getFlatDir()));
                    thrower.entity().addPassenger(display);

                    //TODO: Remove later
                    if (System.currentTimeMillis() - lastTimeSent[0] > 1500) {
                        lastTimeSent[0] = System.currentTimeMillis();
                        thrower.message("Updating pos apparently...");
                    }

                }
            }, 50/x, TimeUnit.MILLISECONDS);  // 50 because that's the millisecond value of a tick
                                                    // TODO Prefab or config value
        }
    }

    public void startIdleMovement() {
        idleMovement = new BukkitRunnable() {
            double step = 0;
            @Override
            public void run() {

                // TODO implement a sinusoidal solution.

                step += Math.PI/3;
            }
        }.runTaskTimer(Sword.getInstance(), 0L, idleMovementPeriod);
    }

    public void endIdleMovement() {
        if (idleMovement != null && !idleMovement.isCancelled()) {
            idleMovement.cancel();
            idleMovement = null;
        }
    }

    public void returnToSheath() {
        UmbralBlade pass = this;
        BukkitTask lerpTask = DisplayUtil.displaySlerpToOffset(thrower, display,
            new Vector(), 2, 4, 3, 3, false,
            new BukkitRunnable() {
                @Override
                public void run() {
                    thrower.message("I have returned.");

                    pass.setState(UmbralState.SHEATHED);
                }
            });
    }

    public void performAttack(double range, boolean heavy) {
        SwordEntity target = thrower.getTargetedEntity(range);
        Attack attack;
        Location attackOrigin;

        if (target == null || !target.isValid()) {
            attackOrigin = thrower.getChestLocation().clone().add(thrower.entity().getEyeLocation().getDirection().multiply(range));
        }
        else {
            Vector to = target.getChestVector().clone().subtract(thrower.getChestVector());
            attackOrigin = target.getChestLocation().clone().subtract(to);
            thrower.message("Targeted this guy: " + target.getDisplayName());
        }

        attack = heavy ? heavyAttacks[0] : basicAttacks[0]; // TODO dynamic.

        attack.setOrigin(attackOrigin);
        attack.execute(thrower);
    }

    private boolean isTooFarOrIdleTooLong() {
        if (display == null) return false;
        double distance = thrower.entity().getLocation().distance(display.getLocation());
        long timeSinceLastAction = System.currentTimeMillis() - lastActionTime;
        return distance > 20.0 || timeSinceLastAction > 30000;
    }

    // TODO probably gonna have to make  better checks for these methods, but good template
    private boolean hasHitTarget() {
        return lastTargetLocation != null;
    }

    private boolean hasLanded() {
        return display != null && display.isOnGround();
    }

    private boolean isTargetDestroyed() {
        return lastTargetLocation == null;
    }

    private boolean lungeMissed() {
        return !hasHitTarget();
    }

    private void loadBasicAttacks() {
        // load from config or registry later
        basicAttacks = new Attack[]{
            new ItemDisplayAttack(display, AttackType.WINDUP_1,
                true, null, true, 5,
                20, 1, 700,
                0, 1)
                .setNextAttack(
                    new ItemDisplayAttack(display, AttackType.BASIC_1,
                        true, attackEndCallback, false, 2,
                        5, 10, 30,
                        0, 1), 100)

        };
    }

    private void loadHeavyAttacks() {
        heavyAttacks = new Attack[]{};
    }

    private void reassignDisplayToAttacks() {
        if (basicAttacks != null) {
            for (Attack attack : basicAttacks) {
                if (attack instanceof ItemDisplayAttack ida) {
                    ida.setWeaponDisplay(display);
                    Attack cur = ida;
                    while (cur.hasNextAttack()) {
                        cur = cur.getNextAttack();
                        if (cur instanceof ItemDisplayAttack nextIda) {
                            nextIda.setWeaponDisplay(display);
                        }
                    }
                }
            }
        }
        if (heavyAttacks != null) {
            for (Attack attack : heavyAttacks) {
                if (attack instanceof ItemDisplayAttack ida) {
                    ida.setWeaponDisplay(display);
                    Attack cur = ida;
                    while (cur.hasNextAttack()) {
                        cur = cur.getNextAttack();
                        if (cur instanceof ItemDisplayAttack nextIda) {
                            nextIda.setWeaponDisplay(display);
                        }
                    }
                }
            }
        }
    }

    private void generateUmbralItems() {
        // item Stack used for determining umbral blade inputs
        this.link = new ItemStackBuilder(Material.HEAVY_CORE)
            .name(Component.text("~ ", TextColor.color(160, 17, 17))
                .append(Component.text(thrower.getDisplayName() + "'s Soul Link",
                    TextColor.color(204, 0, 0), TextDecoration.BOLD))
                .append(Component.text(" ~", TextColor.color(160, 17, 17))))
            .lore(List.of(
                Component.text(""),
                Component.text("Controls:", TextColor.color(200, 200, 200), TextDecoration.ITALIC),
                Component.text("Drop + Swap", TextColor.color(255, 100, 100))
                    .append(Component.text(" - Toggle Standby/Sheathed", TextColor.color(150, 150, 150))),
                Component.text("  • Standby: ", TextColor.color(180, 180, 180))
                    .append(Component.text("Blade hovers, ready to attack", TextColor.color(120, 120, 120))),
                Component.text("  • Sheathed: ", TextColor.color(180, 180, 180))
                    .append(Component.text("Blade stored on back", TextColor.color(120, 120, 120))),
                Component.text(""),
                Component.text("Swap + Left Click", TextColor.color(255, 100, 100))
                    .append(Component.text(" - Wield Blade", TextColor.color(150, 150, 150))),
                Component.text("  • Equip as weapon in hand", TextColor.color(120, 120, 120))
            ))
            .unbreakable(true)
            .tag(KeyRegistry.SOUL_LINK_KEY, thrower.getUniqueId().toString())
            .hideAll()
            .build();

        this.blade = new ItemStackBuilder(weapon.getType())
            .name(Component.text("~ ", TextColor.color(219, 17, 17))
                .append(Component.text(thrower.getDisplayName() + "'s Blade",
                    TextColor.color(17, 17, 17), TextDecoration.BOLD))
                .append(Component.text(" ~", TextColor.color(219, 17, 17))))
            .lore(List.of(
                Component.text(""),
                Component.text("Wielded Form", TextColor.color(200, 200, 200), TextDecoration.ITALIC),
                Component.text("Use normal combat inputs", TextColor.color(150, 150, 150)),
                Component.text(""),
                Component.text("Q + F", TextColor.color(255, 100, 100))
                    .append(Component.text(" - Return to Standby", TextColor.color(150, 150, 150)))
            ))
            .unbreakable(true)
            .tag(KeyRegistry.SOUL_LINK_KEY, thrower.getUniqueId().toString())
            .hideAll()
            .build();
    }

    public void removeWeaponDisplay() {
        if (display != null)
            display.remove();
    }

    @Override
    public void dispose() {
        removeWeaponDisplay();
        active = false;
    }
}
