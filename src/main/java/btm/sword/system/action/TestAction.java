package btm.sword.system.action;

import btm.sword.system.SwordScheduler;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.util.BezierUtil;
import btm.sword.util.Cache;
import btm.sword.util.VectorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TestAction extends SwordAction {
    public static void testAttack(Combatant executor) {
        LivingEntity ex = executor.entity();
        Location o = ex.getEyeLocation();
        int period = 5;

        float radius = 2.5f;
        float xScale = 0.75f;
        float yScale = 0.25f;
        float zScale = 2.0f;
        float startAngle = (float) -Math.PI/2;
        float endAngle = (float) Math.PI/2;
        float increment = (float) Math.PI/45;
        int t = 0;
        for (float i = startAngle; i < endAngle; i+=increment) {
            float x = i;
            int T = t;
            SwordScheduler.runLater(new BukkitRunnable() {
                final Vector3f base = new Vector3f(radius*(float) Math.sin(x), 0, radius*(float) Math.cos(x));
                @Override
                public void run() {
                    BlockDisplay bd = (BlockDisplay) ex.getWorld().spawnEntity(o, EntityType.BLOCK_DISPLAY);
                    bd.setBlock(Material.SCULK.createBlockData());
                    bd.setTransformation(
                            new Transformation(
                                    new Vector3f(base).sub(xScale/2, yScale/2, zScale/2),
                                    new Quaternionf().rotateY(x),
                                    new Vector3f(xScale, yScale, zScale),
                                    new Quaternionf()
                            )
                    );
                    bd.setGlowing(true);
                    bd.setGlowColorOverride(Color.RED);

                    SwordScheduler.runLater(new BukkitRunnable() {
                        @Override
                        public void run() {
                            new BukkitRunnable() {
                                final float check = 0.07f*xScale;
                                final float rate = 0.7f;
                                @Override
                                public void run() {
                                    Transformation cur = bd.getTransformation();
                                    Vector3f s = cur.getScale().mul(rate);
                                    bd.setTransformation(
                                            new Transformation(
                                                    new Vector3f(base).sub(new Vector3f(s).mul(0.5f)),
                                                    cur.getLeftRotation(),
                                                    s,
                                                    cur.getRightRotation()
                                            )
                                    );
                                    if (s.x() < check) {
                                        bd.remove();
                                        cancel();
                                    }
                                }
                            }.runTaskTimer(plugin, 0L, 1L);
                        }
                    },100 + period*T, TimeUnit.MILLISECONDS);
                }
            }, period*t, TimeUnit.MILLISECONDS);
            t++;
        }
    }

    public static void testAttack2(Combatant executor) {
        LivingEntity ex = executor.entity();
        Location o = ex.getEyeLocation();
        List<Vector> controlVectors = Cache.basicSword3;

        ArrayList<Vector> basis = VectorUtil.getBasis(o, o.getDirection());

        double rangeMultiplier = 2;
        List<Vector> tcv = controlVectors.stream()
                .map(v -> VectorUtil.transformWithNewBasis(basis, v).multiply(rangeMultiplier))
                .toList();

        List<Vector> bezierVectors = BezierUtil.cubicBezier3D(tcv.getFirst(), tcv.get(1), tcv.get(2), tcv.getLast(), 6);

        Predicate<Entity> filter = entity ->
                entity instanceof LivingEntity l
                && !l.isDead()
                && l.getType() != EntityType.ARMOR_STAND
                && l.getUniqueId() != executor.getUniqueId();

        int size = bezierVectors.size();
        int halfSize = size/2;
        float xBase = 0.75f;
        float yBase = 0.25f;
        Vector cur = bezierVectors.getFirst();
        for (int i = 0; i < bezierVectors.size(); i++) {
            final Vector next = i + 1 < bezierVectors.size() ? bezierVectors.get(i+1) : null;
            if (next == null) return;
            final Vector _cur = cur;
            final Vector toNext = next.clone().subtract(cur);
            final int I = i;
            float sizeMul = 1 + 0.3f*((halfSize*halfSize) - (i-halfSize));
            final float xScale = xBase * sizeMul;
            final float yScale = yBase * sizeMul;
            SwordScheduler.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    Location s = o.clone().add(_cur).setDirection(toNext);
                    float length = (float) toNext.length();
                    BlockDisplay bd = (BlockDisplay) ex.getWorld().spawnEntity(s, EntityType.BLOCK_DISPLAY);
                    bd.setBlock(Material.SCULK.createBlockData());
                    bd.setTransformation(
                            new Transformation(
                                    new Vector3f(0).sub(xScale/2, yScale/2, 0),
                                    new Quaternionf().rotateZ(0),
                                    new Vector3f(xScale, yScale, length),
                                    new Quaternionf()
                            )
                    );
                    bd.setGlowing(true);
                    bd.setGlowColorOverride(Color.RED);

                    RayTraceResult result = bd.getWorld().rayTraceEntities(s, toNext, length, xScale, filter);

                    if (result != null) {
                        if (result.getHitEntity() != null) {
                            SwordEntityArbiter.getOrAdd(result.getHitEntity().getUniqueId())
                                    .hit(executor, 1, 1, 34, 0, new Vector());
                        }
                    }

                    SwordScheduler.runLater(new BukkitRunnable() {
                        @Override
                        public void run() {
                            bd.remove();
                        }
                    }, 150 + 35*I, TimeUnit.MILLISECONDS);
                }
            }, 35*i, TimeUnit.MILLISECONDS);

            cur = next;
        }
    }

    public static void testCustomAttack(Combatant executor) {
        LivingEntity ex = executor.entity();
        Location o = ex.getEyeLocation();

        for (int i = 0; i < 15; i++) {
            final int I = i;
            SwordScheduler.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    Vector dir;
                    Vector offset;
                    Vector3f scale;
                    switch (I) {
                        case 0:
                            dir = new Vector();
                            offset = new Vector();
                            scale = new Vector3f();
                            break;

                        default:
                            dir = o.getDirection();
                            offset = new Vector();
                            scale = new Vector3f();
                            break;
                    }
                    BlockDisplay bd = (BlockDisplay) ex.getWorld().spawnEntity(o.clone().add(offset).setDirection(dir), EntityType.BLOCK_DISPLAY);
                    bd.setBlock(Material.SPRUCE_WOOD.createBlockData());

                    bd.setGlowing(true);
                    bd.setGlowColorOverride(Color.RED);

                    bd.setTransformation(
                            new Transformation(
                                    new Vector3f().sub(new Vector3f(scale).mul(0.5f)),
                                    new Quaternionf().rotateZ(0),
                                    scale,
                                    new Quaternionf()
                            )
                    );
                }
            }, 30*i, TimeUnit.MILLISECONDS);
        }
    }
}
