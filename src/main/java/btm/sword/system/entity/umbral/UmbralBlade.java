package btm.sword.system.entity.umbral;

import btm.sword.Sword;
import btm.sword.system.SwordScheduler;
import btm.sword.system.action.utility.thrown.ThrownItem;
import btm.sword.system.attack.Attack;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.util.display.DisplayUtil;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// while flying and attacking on its own, no soulfire is reaped on attacks
// while in hand, higher soulfire intake on hit
@Getter
@Setter
public class UmbralBlade extends ThrownItem {
    private final Attack[] basicAttacks;
    private final Attack[] heavyAttacks;
    private UmbralState state = UmbralState.SHEATHED;

    boolean active;

    private ItemStack weapon;

    private long lastActionTime = 0;
    private Location lastTargetLocation;

    private Predicate<UmbralBlade> endHoverPredicate;

    private Vector3f scale = new Vector3f(0.85f, 1.3f, 1f);

    public UmbralBlade(Combatant thrower, ItemStack weapon) {
        super(thrower, display -> {
            display.setItemStack(new ItemStack(Material.STONE_SWORD)); // TODO: Later - make dynamic
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf().rotationY((float) Math.PI / 2).rotateZ(-(float) Math.PI / (1.65f)),
                    new Vector3f(0.85f, 1.3f, 1f),
                    new Quaternionf()
            ));
            display.setPersistent(false);

            thrower.entity().addPassenger(display);
            display.setBillboard(Display.Billboard.FIXED);
            display.setItemStack(weapon);
        }, 5);
        this.weapon = weapon;
        this.basicAttacks = loadBasicAttacks();
        this.heavyAttacks = loadHeavyAttacks();
        this.endHoverPredicate = umbralBlade -> !umbralBlade.getState().equals(UmbralState.STANDBY);
        this.state = null;
    }

    public void setState(UmbralState newState) throws InterruptedException {
        this.state = newState;
        setDisplayTransformation(newState);
        switch (newState) {
            case RECALLING -> returnToSheath();
            case STANDBY -> hoverBehindWielder();
            case ATTACKING -> display.remove();
            default -> {} // FLYING, LODGED, SHEATHED don't require special transition logic
        }
    }

    public void onTick() {
        if (!active) {
            thrower.message("Umbral Blade Not active.");
            return;
        }

        if (state == null) {
            try {
                setState(UmbralState.SHEATHED);
            } catch (InterruptedException e) {
                Sword.print(">> Interrupted Exception:\n" + e.getMessage() + "\n");
            }
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
                    new Vector3f(0.28f, -1.1f, -0.42f),
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
            default -> {} // ATTACKING, LODGED don't set display transformations
        }
    }

    public void restartDisplay() {
        active = false;
        Bukkit.getScheduler().runTaskLater(Sword.getInstance(), () -> {
            if (!thrower.isValid()) return;

            World world = thrower.entity().getWorld();
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

    public void updateSheathedPosition() {
        int x = 3;
        for (int i = 0; i < x; i++) {
            SwordScheduler.runBukkitTaskLater(new BukkitRunnable() {
                @Override
                public void run() {
                    DisplayUtil.smoothTeleport(display, 2);
                    display.teleport(thrower.entity().getLocation().setDirection(thrower.getFlatDir()));
                    thrower.entity().addPassenger(display);
                }
            }, 50/x, TimeUnit.MILLISECONDS);
        }
    }

    public boolean inState(UmbralState state) {
        return getState().equals(state);
    }

    public void hoverBehindWielder() {
        // Play unsheathing animation

        // follows player shoulder position smoothly
        DisplayUtil.itemDisplayFollowLerp(thrower, display,
                new Vector(0.7, 0.7, -0.5),
                5, 3, false, endHoverPredicate, this);

        // play idle animations ever so often
    }

    public void returnToSheath() throws InterruptedException {
        BukkitTask lerpTask = DisplayUtil.displaySlerpToOffset(thrower, display,
                new Vector(), 2, 4, 3, 3, false);
        lerpTask.wait();
        thrower.message("I have returned.");

        setState(UmbralState.SHEATHED);
    }

    public void performAttack(SwordEntity target, boolean heavy) {

    }

    public void lungeToTarget(SwordEntity target) {
        if (target == null) return;
        Location start = display.getLocation();
        Location end = target.entity().getLocation().clone().add(0, 1, 0);
        DisplayUtil.smoothTeleport(this.display, 6);
        // optional: particle trail + impalement call
    }

    public void recall() throws InterruptedException {
        // if too far or idle too long, return automatically
        returnToSheath();
    }

    private Attack[] loadBasicAttacks() {
        // load from config or registry later
        return new Attack[]{};
    }

    private Attack[] loadHeavyAttacks() {
        return new Attack[]{};
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
