package btm.sword.system.action.utility;

import btm.sword.Sword;
import btm.sword.system.action.SwordAction;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.util.ParticleWrapper;
import btm.sword.util.SoundUtil;
import btm.sword.util.sound.SoundType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

/**
 * Provides utility actions and development/testing tools.
 * <p>
 * This class contains both production utility actions (e.g., death ray)
 * and development testing methods (sound test, particle test, orientation test).
 * </p>
 *
 * @see SwordAction
 */
public class UtilityAction extends SwordAction {
    /**
     * Executes a death ray attack that targets entities in the executor's line of sight.
     * <p>
     * This method performs a raycast from the executor's eye location and applies
     * massive damage to the first entity hit. If the target is not a LivingEntity,
     * it creates an explosion at the target's location.
     * </p>
     *
     * @param executor the combatant executing the death ray action
     */
    public static void death(Combatant executor) {
        cast(executor, 0L, new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity ex = executor.entity();
                Location l = executor.entity().getEyeLocation();
                RayTraceResult ray = ex.getWorld().rayTraceEntities(l, l.getDirection(), 6, entity -> entity.getUniqueId() != ex.getUniqueId());
                if (ray != null && ray.getHitEntity() != null) {
                    Entity target = ray.getHitEntity();
                    if (target instanceof LivingEntity le)
                        SwordEntityArbiter.getOrAdd(le.getUniqueId()).hit(
                                executor, 0,
                                1000, 20000,
                                1, l.getDirection().multiply(100));
                    else {
                        target.getWorld().createExplosion(target.getLocation(), 5, true, true);
                    }
                }
            }
        });
    }

    /**
     * Development tool for testing all available sound effects.
     * <p>
     * Plays each {@link SoundType} sequentially with a delay, sending a message
     * to the executor with the index and sound key for identification.
     * </p>
     *
     * @param executor the combatant running the sound test
     * @param startIndex the index to start testing from (allows resuming)
     */
    public static void soundTest(Combatant executor, int startIndex) {
        int i = 0;
        for (SoundType soundType : SoundType.values()) {
            if (i < startIndex) {
                i++;
                continue;
            }
            int finalI = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    SoundUtil.playSound(executor.entity(), soundType, 1f, 1f);
                    executor.message("i: " + finalI + ", " + soundType.getKey());
                }
            }.runTaskLater(Sword.getInstance(), 30L * (i-startIndex));
            i++;
        }
    }

    /**
     * Development tool for testing all available particle effects.
     * <p>
     * Spawns each {@link Particle} type sequentially at a location in front
     * of the executor, sending a message with the index and particle name.
     * Catches and reports any exceptions for unsupported particle types.
     * </p>
     *
     * @param executor the combatant running the particle test
     */
    public static void particleTest(Combatant executor) {
        Location l = executor.getChestLocation().add(executor.entity().getEyeLocation().getDirection().multiply(2));
        int i = 0;
        for (Particle particle : Particle.values()) {
            int finalI = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        new ParticleWrapper(particle, 1, 0, 0, 0, 0).display(l);
                    } catch (Exception e) {
                        executor.message(e.getMessage());
                    }
                    executor.message("i: " + finalI + ", " + particle.name());
                }
            }.runTaskLater(Sword.getInstance(), 30L * i);
            i++;
        }
    }

    /**
     * Development tool for testing ItemDisplay rotation orientations.
     * <p>
     * Spawns item displays (shields, axes, swords) and rotates them around
     * the X, Y, and Z axes to visualize rotation behavior. The displays are
     * automatically removed after 30 iterations.
     * </p>
     *
     * @param executor the combatant running the orientation test
     */
    public static void orientationTest(Combatant executor) {
        Location base = executor.getChestLocation().setDirection(executor.entity().getEyeLocation().getDirection());
        World world = base.getWorld();
        List<List<ItemDisplay>> displays = new ArrayList<>(3);
        List<ItemDisplay> displaysX = new ArrayList<>(3);
        List<ItemDisplay> displaysY = new ArrayList<>(3);
        List<ItemDisplay> displaysZ = new ArrayList<>(3);
        displays.add(displaysX);
        displays.add(displaysY);
        displays.add(displaysZ);

        ItemDisplay shieldDisplayX = (ItemDisplay) world.spawnEntity(base, EntityType.ITEM_DISPLAY);
        shieldDisplayX.setItemStack(new ItemStack(Material.SHIELD));
        ItemDisplay shieldDisplayY = (ItemDisplay) world.spawnEntity(base, EntityType.ITEM_DISPLAY);
        shieldDisplayY.setItemStack(new ItemStack(Material.SHIELD));
        ItemDisplay shieldDisplayZ = (ItemDisplay) world.spawnEntity(base, EntityType.ITEM_DISPLAY);
        shieldDisplayZ.setItemStack(new ItemStack(Material.SHIELD));

        ItemDisplay axeDisplayX = (ItemDisplay) world.spawnEntity(base, EntityType.ITEM_DISPLAY);
        axeDisplayX.setItemStack(new ItemStack(Material.NETHERITE_AXE));
        ItemDisplay axeDisplayY = (ItemDisplay) world.spawnEntity(base, EntityType.ITEM_DISPLAY);
        axeDisplayY.setItemStack(new ItemStack(Material.NETHERITE_AXE));
        ItemDisplay axeDisplayZ = (ItemDisplay) world.spawnEntity(base, EntityType.ITEM_DISPLAY);
        axeDisplayZ.setItemStack(new ItemStack(Material.NETHERITE_AXE));

        ItemDisplay swordDisplayX = (ItemDisplay) world.spawnEntity(base, EntityType.ITEM_DISPLAY);
        swordDisplayX.setItemStack(new ItemStack(Material.NETHERITE_SWORD));
        ItemDisplay swordDisplayY = (ItemDisplay) world.spawnEntity(base, EntityType.ITEM_DISPLAY);
        swordDisplayY.setItemStack(new ItemStack(Material.NETHERITE_SWORD));
        ItemDisplay swordDisplayZ = (ItemDisplay) world.spawnEntity(base, EntityType.ITEM_DISPLAY);
        swordDisplayZ.setItemStack(new ItemStack(Material.NETHERITE_SWORD));

        displaysX.add(shieldDisplayX);
        displaysX.add(axeDisplayX);
        displaysX.add(swordDisplayX);

        displaysY.add(shieldDisplayY);
        displaysY.add(axeDisplayY);
        displaysY.add(swordDisplayY);

        displaysZ.add(shieldDisplayZ);
        displaysZ.add(axeDisplayZ);
        displaysZ.add(swordDisplayZ);

        int[] step = {0};
        new BukkitRunnable() {
            @Override
            public void run() {
                int i = 0;
                for (ItemDisplay display : displaysX) {
                    Transformation cur = display.getTransformation();
                    display.setTransformation(
                            new Transformation(
                                    new Vector3f(3, i, 1),
                                    cur.getLeftRotation().rotateX((float) Math.PI/3),
                                    cur.getScale(),
                                    cur.getRightRotation()
                            )
                    );
                    i += 2;
                }
                i = 0;
                for (ItemDisplay display : displaysY) {
                    Transformation cur = display.getTransformation();
                    display.setTransformation(
                            new Transformation(
                                    new Vector3f(0, i, 2),
                                    cur.getLeftRotation().rotateY((float) Math.PI/3),
                                    cur.getScale(),
                                    cur.getRightRotation()
                            )
                    );
                    i += 2;
                }
                i = 0;
                for (ItemDisplay display : displaysZ) {
                    Transformation cur = display.getTransformation();
                    display.setTransformation(
                            new Transformation(
                                    new Vector3f(-3, i, -1),
                                    cur.getLeftRotation().rotateZ((float) Math.PI/3),
                                    cur.getScale(),
                                    cur.getRightRotation()
                            )
                    );
                    i += 2;
                }

                if (step[0] > 30) {
                    for (List<ItemDisplay> displays_ : displays) {
                        for (ItemDisplay display : displays_) {
                            display.remove();
                        }
                    }
                    cancel();
                }
                step[0]++;
            }
        }.runTaskTimer(Sword.getInstance(), 0L, 10L);
    }
}
