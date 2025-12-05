package btm.sword.system.action.throwing;

import lombok.Getter;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.system.control.TimeArbiter;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.ParticleWrapper;

public class StuckItem implements InteractiveItem {
    @Getter
    private final ItemStack itemStack;
    private final World world;
    private final ItemDisplay display;

    private Location pos;
    private Vector to;
    private final Vector velocity;

    private BlockData hitBlockData;

    private BukkitTask physicsTask;

    // Physics constants
    private static final double TICK_GRAVITY = -0.055;  // per physics tick
    private static final double DRAG = 0.99;
    private static final double PHYSICS_INTERVAL = 1; // ticks between updates
    private static final double STEP_SIZE = 0.3;       // forward raytrace resolution

    private boolean stuck = false;

    public StuckItem(Location start, Vector initialVelocity, ItemStack stack) {
        this.world = start.getWorld();
        this.pos = start.clone();
        this.velocity = initialVelocity.clone();
        this.to = velocity.clone();
        this.itemStack = stack;

        this.display = spawnDisplay(start, stack);
        determineOrientation();

        startPhysics();
    }

    private ItemDisplay spawnDisplay(Location origin, ItemStack item) {
        ItemDisplay display = (ItemDisplay) world.spawnEntity(origin, EntityType.ITEM_DISPLAY);
        display.setItemStack(item);
        display.setBillboard(Display.Billboard.FIXED);
        display.setPersistent(true);
        display.setInterpolationDuration(1);
//        display.setTeleportDuration(2);
        display.setGravity(false);
        display.setRotation(0f, 0f);

        return display;
    }

    public void register() {
        if (display != null) {
            InteractiveItemArbiter.put(this);
        }
    }

    public void determineOrientation() {
        String name = display.getItemStack().getType().toString();

        // *** These numbers are fine for now, config later but this system may change.

        if (name.endsWith("_SWORD")) {
            display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf()
                    .rotateY((float) Math.PI / 2)
                    .rotateZ((float) Math.PI / 2),
                new Vector3f(1, 1, 1),
                new Quaternionf()
            ));
        } else if (name.endsWith("AXE") || name.endsWith("_HOE") || name.endsWith("_SHOVEL")) {
            display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateY((float) -Math.PI / 2)
                    .rotateZ((float) Math.PI / 4),
                new Vector3f(1.5f, 1.5f, 1.5f),
                new Quaternionf()
            ));
        } else if (display.getItemStack().getType() == Material.SHIELD) {
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf().rotateY((float) (Math.PI / 1.01f) * 0),
                new Vector3f(1, 1, 1),
                new Quaternionf()
            ));
        } else {
            display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateZ((float) Math.PI / 8),
                new Vector3f(1, 1, 1),
                new Quaternionf()
            ));
        }
    }

    // Physics lifecycle
    private void startPhysics() {
        physicsTask = Bukkit.getScheduler().runTaskTimer(
            Sword.getInstance(),
            this::tickPhysics,
            0L,
            (long) PHYSICS_INTERVAL
        );
    }

    private void tickPhysics() {
        if (display == null || display.isDead() || stuck) {
            stopPhysics();
            return;
        }

        applyGravityAndDrag();
        moveAndCheckCollision();

        TimeArbiter.teleportDisplay(display, pos, to, 2);
    }

    private void stopPhysics() {
        if (physicsTask != null) {
            physicsTask.cancel();
            physicsTask = null;
        }
    }

    private void applyGravityAndDrag() {
        velocity.setY(velocity.getY() + TICK_GRAVITY);
        velocity.multiply(DRAG);
    }

    private void moveAndCheckCollision() {
        if (velocity.lengthSquared() == 0) {
            settleOnGround();
            return;
        }

        Vector stepDir = velocity.clone().normalize();

        double remaining = velocity.length();
        Location cur = pos.clone();

        while (remaining > 0) {
            double step = Math.min(STEP_SIZE, remaining);

            Location next = cur.clone().add(stepDir.clone().multiply(step));

            RayTraceResult hit = world.rayTraceBlocks(
                cur,
                stepDir,
                step,
                FluidCollisionMode.NEVER,
                true
            );

            if (hit != null && hit.getHitBlock() != null) {
                hitBlockData = hit.getHitBlock().getBlockData();
                resolveCollision(hit);
                return;
            }

            to = next.toVector().subtract(cur.toVector());
            cur = next;
            remaining -= step;
        }

        pos = cur;
    }

    private void resolveCollision(RayTraceResult hit) {
        pos = hit.getHitPosition().toLocation(world);
        velocity.zero();
        settledStick();
    }

    private void settleOnGround() {
        // Ensure it rests on surface: move down until block isn't passable
        Location test = pos.clone();
        for (int i = 0; i < 30; i++) {
            Location up = test.clone().add(0, 0.05, 0);
            if (up.getBlock().isPassable()) {
                test = up;
            } else break;
        }

        pos = test;
        settledStick();
    }

    private void settledStick() {
        stuck = true;

        new ParticleWrapper(Particle.BLOCK, 50, 1, 1, 1, hitBlockData).display(pos);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (display == null || display.isDead()) {
                    cancel();
                    return;
                }

                Prefab.Particles.THROWN_ITEM_MARKER.display(display.getLocation());
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 2L);

        display.setRotation(pos.getYaw(), pos.getPitch());
        stopPhysics();
    }

    @Override
    public void dispose() {
        stopPhysics();

        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    @Override
    public ItemDisplay getDisplay() {
        return display;
    }
}
