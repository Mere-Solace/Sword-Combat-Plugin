package btm.sword.system.entity.umbral;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.GameMode;
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
import btm.sword.system.entity.types.SwordPlayer;
import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.input.InputBuffer;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateMachine;
import btm.sword.system.entity.umbral.statemachine.state.AttackingHeavyState;
import btm.sword.system.entity.umbral.statemachine.state.AttackingQuickState;
import btm.sword.system.entity.umbral.statemachine.state.FlyingState;
import btm.sword.system.entity.umbral.statemachine.state.InactiveState;
import btm.sword.system.entity.umbral.statemachine.state.LodgedState;
import btm.sword.system.entity.umbral.statemachine.state.LungingState;
import btm.sword.system.entity.umbral.statemachine.state.PreviousState;
import btm.sword.system.entity.umbral.statemachine.state.RecallingState;
import btm.sword.system.entity.umbral.statemachine.state.RecoverState;
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

    private Function<Combatant, Attack>[] basicAttacks;
    private Function<Combatant, Attack>[] heavyAttacks;

    private ItemStack link;
    private ItemStack blade;

    private ItemStack weapon;

    private long lastActionTime = 0;
    private Location lastTargetLocation;

    private Vector3f scale = new Vector3f(0.85f, 1.3f, 1f);

    private static final int idleMovementPeriod = 5;
    private BukkitTask idleMovement;

    private final Predicate<UmbralBlade> endHoverPredicate;
    private final Runnable attackEndCallback;
    private boolean attackCompleted = false;

    private final InputBuffer inputBuffer = new InputBuffer();

    public UmbralBlade(Combatant thrower, ItemStack weapon) {
        super(thrower, display -> {
            display.setItemStack(weapon);
            display.setTransformation(new Transformation(
                    new Vector3f(0.28f, -1.35f, -0.42f),
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

        this.attackEndCallback = () -> attackCompleted = true;

        loadBasicAttacks();
        loadHeavyAttacks();

        this.bladeStateMachine = new UmbralStateMachine(this, new SheathedState());
        initStateMachine();

        endHoverPredicate = blade -> !bladeStateMachine.inState(new StandbyState());
    }

    private void initStateMachine() {
        // =====================================================================
        // UNIVERSAL — wildcard transitions
        // =====================================================================

        // 1) Enter inactive from ANYTHING
        bladeStateMachine.addTransition(new Transition<>(
            UmbralStateFacade.class,
            InactiveState.class,
            blade -> (thrower.entity() instanceof SwordPlayer sp &&
                sp.player().getGameMode().equals(GameMode.SPECTATOR)) ||
                isRequested(BladeRequest.DEACTIVATE),
            blade -> {}
        ));

        // 2) Enter recover from ANYTHING when display is invalid
        bladeStateMachine.addTransition(new Transition<>(
            UmbralStateFacade.class,
            RecoverState.class,
            blade -> blade.getDisplay() == null || blade.display.isDead() || !blade.display.isValid(),
            blade -> {}
        ));

        // 3) Reactivate to last state
        bladeStateMachine.addTransition(new Transition<>(
            InactiveState.class,
            PreviousState.class,
            blade -> isRequested(BladeRequest.ACTIVATE_TO_PREVIOUS),
            blade -> {}
        ));

        // 4) Recover and go back to last state
        bladeStateMachine.addTransition(new Transition<>(
            RecoverState.class,
            PreviousState.class,
            blade -> (display != null && !display.isDead() && display.isValid()) ||
                isRequested(BladeRequest.RESUME_FROM_REPAIR),
            blade -> { }
        ));

        // =====================================================================
        // SHEATHED
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            SheathedState.class,
            StandbyState.class,
            blade -> isRequestedAndActive(BladeRequest.TOGGLE),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            SheathedState.class,
            WieldState.class,
            blade -> isRequestedAndActive(BladeRequest.WIELD),
            blade -> {}
        ));


        // =====================================================================
        // STANDBY
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            StandbyState.class,
            SheathedState.class,
            blade -> isRequestedAndActive(BladeRequest.TOGGLE),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            StandbyState.class,
            WieldState.class,
            blade -> isRequestedAndActive(BladeRequest.WIELD),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            StandbyState.class,
            AttackingQuickState.class,
            blade -> isRequestedAndActive(BladeRequest.ATTACK_QUICK),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            StandbyState.class,
            AttackingHeavyState.class,
            blade -> isRequestedAndActive(BladeRequest.ATTACK_HEAVY),
            blade -> {}
        ));


        // =====================================================================
        // WIELD
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            WieldState.class,
            StandbyState.class,
            blade -> isRequestedAndActive(BladeRequest.TOGGLE),
            blade -> {}
        ));


        // =====================================================================
        // ATTACKING (Quick + Heavy)
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            AttackingQuickState.class,
            WaitingState.class,
            blade -> blade.attackCompleted,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            AttackingHeavyState.class,
            WaitingState.class,
            blade -> blade.attackCompleted,
            blade -> {}
        ));


        // =====================================================================
        // WAITING
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            WaitingState.class,
            StandbyState.class,
            blade -> true, //!blade.shouldReturn(), // immediate fallback
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            WaitingState.class,
            ReturningState.class,
            UmbralBlade::isTooFarOrIdleTooLong,
            blade -> {}
        ));


        // =====================================================================
        // RECALLING / RETURNING
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            RecallingState.class,
            SheathedState.class,
            blade -> isRequestedAndActive(BladeRequest.SHEATH),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            ReturningState.class,
            SheathedState.class,
            blade -> isRequestedAndActive(BladeRequest.SHEATH),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            RecallingState.class,
            StandbyState.class,
            blade -> isRequestedAndActive(BladeRequest.STANDBY),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            ReturningState.class,
            StandbyState.class,
            blade -> isRequestedAndActive(BladeRequest.STANDBY),
            blade -> {}
        ));


        // =====================================================================
        // FLYING
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            FlyingState.class,
            LodgedState.class,
            UmbralBlade::hasHitTarget,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            FlyingState.class,
            WaitingState.class,
            UmbralBlade::hasLanded,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            FlyingState.class,
            RecallingState.class,
            blade -> isRequestedAndActive(BladeRequest.RECALL),
            blade -> {}
        ));


        // =====================================================================
        // LODGED
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            LodgedState.class,
            RecallingState.class,
            blade -> isRequestedAndActive(BladeRequest.RECALL),
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            LodgedState.class,
            WaitingState.class,
            UmbralBlade::isTargetDestroyed,
            blade -> {}
        ));


        // =====================================================================
        // LUNGING
        // =====================================================================
        bladeStateMachine.addTransition(new Transition<>(
            LungingState.class,
            LodgedState.class,
            UmbralBlade::hasHitTarget,
            blade -> {}
        ));

        bladeStateMachine.addTransition(new Transition<>(
            LungingState.class,
            WaitingState.class,
            UmbralBlade::lungeMissed,
            blade -> {}
        ));
    }

    public void request(BladeRequest request) {
        inputBuffer.push(request);
    }

    public boolean isRequested(BladeRequest request) {
        return inputBuffer.consumeIfPresent(request);
    }

    public boolean isRequestedAndActive(BladeRequest request) {
        return isRequested(request) && !inState(InactiveState.class);
    }

    public boolean isOwnedBy(Combatant combatant) {
        return combatant.getUniqueId() == thrower.getUniqueId();
    }

    public boolean inState(Class<? extends State<UmbralBlade>> clazz) {
        return bladeStateMachine.getState().getClass().equals(clazz);
    }

    // ALL issues that come up for the blade not working will go here:
    // - Cannot spawn it immediately as the first player is initialized.
    // - If display is removed, go into recovery mode
    //      - if in inactive state, go back into inactive state
    //      - may need inactive blade restart instructions (consumer)
    // -
    public void onTick() {
        if (!thrower.isValid()) {
            thrower.message("Ending Umbral Blade");
            dispose();
        }

        if (bladeStateMachine != null)
            bladeStateMachine.tick();
    }

    public void setDisplayTransformation(Class<? extends State<UmbralBlade>> state) {
        if (display == null) return;

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

    public BukkitTask hoverBehindWielder() {
        // Play unsheathing animation

        // follows player shoulder position smoothly
        return DisplayUtil.itemDisplayFollowLerp(thrower, display,
            new Vector(0.7, 0.7, -0.5),
            5, 3, false);
    }

    public void registerAsInteractableItem() {
        InteractiveItemArbiter.put(this);
    }

    public void unregisterAsInteractableItem() {
        InteractiveItemArbiter.remove(display, false);
    }

    public void updateSheathedPosition() {
        if (inState(WaitingState.class)) return;

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

    // TODO make a stronger and more dynamic verison of this ( could return the task if need be)
    public void returnToWielderAndRequestState(BladeRequest request) {
        BukkitTask lerpTask = DisplayUtil.displaySlerpToOffset(thrower, display,
            new Vector(), 1, 5, 4, 1, false,
            new BukkitRunnable() {
                @Override
                public void run() {
                    thrower.message("I have returned.");

                    request(request);
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

        attack = heavy ? heavyAttacks[0].apply(thrower) : basicAttacks[0].apply(thrower); // TODO dynamic.

        attack.setOriginOfAll(attackOrigin);
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
        basicAttacks = new Function[]{
            combatant -> new ItemDisplayAttack(display, AttackType.WINDUP_1,
                true, null, true, 5,
                30, 1, 2000,
                0, 1)
                .setInitialMovementTicks(5)
                .setDrawParticles(false)
                .setNextAttack(
                    new ItemDisplayAttack(display, AttackType.BASIC_1,
                        true, attackEndCallback, false, 2,
                        5, 10, 30,
                        0, 1), 500)
        };
    }

    private void loadHeavyAttacks() {

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

    public void resetWeaponDisplay() {
        if (display != null) {
            display.remove();
            display = null;
        }

        LivingEntity e = thrower.getSelf();
        display = (ItemDisplay) e.getWorld().spawnEntity(e.getEyeLocation(), EntityType.ITEM_DISPLAY);
        displaySetupInstructions.accept(display);
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
                    resetWeaponDisplay();
                    setupSuccessful = true;
                } catch(Exception e){
                    e.addSuppressed(e);
                }
            }
        }.runTaskTimer(Sword.getInstance(), 0L, period);
    }

    @Override
    public void dispose() {
        bladeStateMachine.setDeactivated(true);
        removeWeaponDisplay();
    }
}
