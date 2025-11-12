package btm.sword.system.entity.umbral;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import btm.sword.system.attack.AttackType;
import btm.sword.system.action.utility.thrown.InteractiveItemArbiter;
import btm.sword.system.attack.ItemDisplayAttack;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.system.item.KeyRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

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
import btm.sword.system.action.utility.thrown.ThrownItem;
import btm.sword.system.attack.Attack;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.util.display.DisplayUtil;
import lombok.Getter;
import lombok.Setter;

// while flying and attacking on its own, no soulfire is reaped on attacks
// while in hand, higher soulfire intake on hit
@Getter
@Setter
public class UmbralBlade extends ThrownItem {
    private Attack[] basicAttacks;
    private Attack[] heavyAttacks;
    private UmbralState state = UmbralState.SHEATHED;

    private final ItemStack link;
    private final ItemStack blade;

    boolean active;

    private ItemStack weapon;

    private long lastActionTime = 0;
    private Location lastTargetLocation;

    private Predicate<UmbralBlade> endHoverPredicate;

    private Vector3f scale = new Vector3f(0.85f, 1.3f, 1f);

    private static final int idleMovementPeriod = 7;
    private BukkitTask idleMovement;

    private final Runnable attackEndCallback;

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

        // item Stack used for determining umbral blade inputs
        this.link = new ItemStackBuilder(Material.HEAVY_CORE)
            .name(Component.text("~ ", TextColor.color(160, 17, 17))
                .append(Component.text(thrower.getDisplayName() + "'s Soul Link",
                    TextColor.color(204, 0, 0), TextDecoration.BOLD))
                .append(Component.text(" ~", TextColor.color(160, 17, 17))))
            .lore(List.of(
                // TODO: instructions for use here
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
                // TODO: instructions for use here
            ))
            .unbreakable(true)
            .tag(KeyRegistry.SOUL_LINK_KEY, thrower.getUniqueId().toString())
            .hideAll()
            .build();

        UmbralBlade pass = this;
        attackEndCallback = new BukkitRunnable() {
            @Override
            public void run() {
                pass.setState(UmbralState.WAITING);
            }
        };

        this.weapon = weapon;
        this.endHoverPredicate = umbralBlade -> !umbralBlade.getState().equals(UmbralState.STANDBY);
        this.state = null;

        loadBasicAttacks();
        loadHeavyAttacks();
    }

    public boolean isOwnedBy(Combatant combatant) {
        return combatant.getUniqueId() == thrower.getUniqueId();
    }

    public void setState(UmbralState newState) {
        this.state = newState;
        thrower.message("Updating state: " + newState.name());
        handleStateChange(newState); // TODO make a State Wrapper with defined transitions for stronger architecture
        switch (newState) {
            case RECALLING, RETURNING -> returnToSheath();
            case STANDBY -> hoverBehindWielder();
            case ATTACKING_QUICK -> performAttack(3, false);
            case WAITING -> registerAsInteractableItem();
            case WIELD -> onWield();
            default -> {} // FLYING, LODGED, SHEATHED don't require special transition logic
        }
    }

    private void handleStateChange(UmbralState newState) {
        setDisplayTransformation(newState);

        if (newState != UmbralState.STANDBY) endIdleMovement();
        if (newState != UmbralState.WIELD) {
            display.setInvisible(false);
            thrower.setItemStackInHand(link, true);
        }
    }

    public boolean inState(UmbralState state) {
        return getState().equals(state);
    }

    public void onTick() {
        if (!active) {
            thrower.message("Umbral Blade Not active.");
            return;
        }

        if (state == null) {
            setState(UmbralState.SHEATHED);
        }

        if (!thrower.isValid()) {
            thrower.message("Ending Umbral Blade");
            dispose();
        }

        if (display != null && active && inState(UmbralState.SHEATHED)) {
            updateSheathedPosition();
        }

        if (thrower.getTicks() % 2 == 0) {
            if ((display == null || display.isDead()) && active) {
                thrower.message("Restarting UmbralBlade Display");
                restartDisplay();
            }
        }
    }

    public void setDisplayTransformation(UmbralState state) {
        DisplayUtil.setInterpolationValues(display, 0, 4);
        switch (state) {
            case SHEATHED -> display.setTransformation(new Transformation(
                    new Vector3f(0.28f, -1.35f, -0.42f),
                    new Quaternionf().rotationY((float) Math.PI / 2).rotateZ(-(float) Math.PI / (1.65f)),
                    scale,
                    new Quaternionf()));
            case STANDBY -> display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf().rotationY(0).rotateZ((float) Math.PI),
                    scale,
                    new Quaternionf()));
            case FLYING, RECALLING -> display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    scale,
                    new Quaternionf()));
            default -> display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                scale,
                new Quaternionf())); // ATTACKING, LODGED don't set display transformations
        }
    }

    public void restartDisplay() {
        active = false;
        Bukkit.getScheduler().runTaskLater(Sword.getInstance(), () -> {
            if (!thrower.isValid()) return;

            Location loc = thrower.entity().getLocation();

            if (!loc.getChunk().isLoaded()) loc.getChunk().load();

            BukkitTask setupTask = setup(false, 5);
            try {
                setupTask.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            active = true;
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

                    setupSuccessful = true;
                } catch(Exception e){
                    e.addSuppressed(e);
                }
            }
        }.runTaskTimer(Sword.getInstance(), 0L, period);
    }

    public void updateSheathedPosition() {
        // don't waste computing power to update resources while wielding the blade.
        if (inState(UmbralState.WIELD)) return;

        int x = 3;
        for (int i = 0; i < x; i++) {
            SwordScheduler.runBukkitTaskLater(new BukkitRunnable() {
                @Override
                public void run() {
                    DisplayUtil.smoothTeleport(display, 2);
                    display.teleport(thrower.entity().getLocation().setDirection(thrower.getFlatDir()));
                    thrower.entity().addPassenger(display);
                }
            }, 50/x, TimeUnit.MILLISECONDS);  // 50 because that's the millisecond value of a tick
                                                    // TODO Prefab or config value
        }
    }

    private void hoverBehindWielder() {
        // Play unsheathing animation

        // follows player shoulder position smoothly
        DisplayUtil.itemDisplayFollowLerp(thrower, display,
                new Vector(0.7, 0.7, -0.5),
                5, 3, false, endHoverPredicate, this);

        startIdleMovement();
    }

    private void registerAsInteractableItem() {
        startIdleMovement();
        InteractiveItemArbiter.put(this);
    }

    public void onWield() {
        display.setInvisible(true);
        thrower.setItemStackInHand(blade, true);
    }

    public void startIdleMovement() {
        idleMovement = new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {

                boolean change = true;
                Transformation newTr;
                Transformation old;
                switch (step) {
                    case 1:
                        old = display.getTransformation();
                        newTr = new Transformation(
                            old.getTranslation().add(0.005f, -0.3f, 0),
                            old.getLeftRotation(),
                            old.getScale(),
                            old.getRightRotation()
                        );
                        break;

                    case 3:
                        old = display.getTransformation();
                        newTr = new Transformation(
                            old.getTranslation().add(-0.005f, 0.3f, 0),
                            old.getLeftRotation(),
                            old.getScale(),
                            old.getRightRotation()
                        );
                        break;

                    default:
                        setDisplayTransformation(UmbralState.STANDBY);
                        newTr = display.getTransformation();
                        change = false;
                        if (step >= 4) step = 0;
                        break;

                }
                if (change) {
                    DisplayUtil.setInterpolationValues(display, 0, idleMovementPeriod*2);
                    display.setTransformation(newTr);
                }

                change = true;
                step++;
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
        }

        attack = heavy ? heavyAttacks[0] : basicAttacks[0]; // TODO dynamic.

        attack.setOrigin(attackOrigin);
        attack.execute(thrower);
    }

    public void recall() {
        // if too far or idle too long, return automatically

    }

    private void loadBasicAttacks() {
        // load from config or registry later
        basicAttacks = new Attack[]{
            new ItemDisplayAttack(display, AttackType.BASIC_1,
                true, attackEndCallback, false, 2,
                5, 10, 30,
                0, 1)
                .setNextAttack(
                    new ItemDisplayAttack(display, AttackType.WINDUP_1,
                        true, null, true, 5,
                        20, 1, 700,
                        0, 1),
                    50)
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
                    while (ida.hasNextAttack()) {
                        if (ida.getNextAttack() instanceof ItemDisplayAttack nextIda) {
                            nextIda.setWeaponDisplay(display);
                        }
                        cur = ida.getNextAttack();
                    }
                }
            }
        }
        if (heavyAttacks != null) {
            for (Attack attack : heavyAttacks) {
                if (attack instanceof ItemDisplayAttack ida) {
                    ida.setWeaponDisplay(display);
                    Attack cur = ida;
                    while (ida.hasNextAttack()) {
                        if (ida.getNextAttack() instanceof ItemDisplayAttack nextIda) {
                            nextIda.setWeaponDisplay(display);
                        }
                        cur = ida.getNextAttack();
                    }
                }
            }
        }
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
