package btm.sword.system.entity.umbral.statemachine.state;
//
//import btm.sword.system.entity.base.SwordEntity;
//import btm.sword.system.entity.umbral.UmbralBlade;
//import btm.sword.system.entity.umbral.input.BladeRequest;
import btm.sword.system.entity.umbral.UmbralBlade;
import btm.sword.system.entity.umbral.statemachine.UmbralStateFacade;
//
//import org.bukkit.Color;
//import org.bukkit.Location;
//import org.bukkit.scheduler.BukkitRunnable;
//import org.bukkit.util.Transformation;
//import org.joml.Quaternionf;
//import org.joml.Vector3f;

public class FinisherState extends UmbralStateFacade {
    @Override
    public String name() {
        return "FINISHER";
    }

    @Override
    public void onEnter(UmbralBlade context) {

    }

    @Override
    public void onExit(UmbralBlade context) {

    }

    @Override
    public void onTick(UmbralBlade context) {

    }
//    private static final int SPIN_UP_DURATION = 40;      // ticks for spinning up and rising
//    private static final int CRASH_DURATION = 20;        // ticks for lunge down
//    private static final float MAX_SPIN_SPEED = 0.3f;    // max rotation speed radians per tick
//
//    private UmbralBlade blade;
//    private SwordEntity target;
//    private BukkitRunnable spinTask;
//    private BukkitRunnable crashTask;
//
//    private double spinAngle = 0;
//    private float spinSpeed = 0.05f;
//
//    @Override
//    public String name() {
//        return "FINISHER";
//    }
//
//    @Override
//    public void onEnter(UmbralBlade blade) {
//        this.blade = blade;
//        this.target = blade.getThrower().getTargetedEntity(10);
//        if (target == null || target.isInvalid()) {
//            blade.request(BladeRequest.STANDBY);
//            return;
//        }
//
//        if (blade.getThrower().getAspects().formCur() < 7) {
//            blade.request(BladeRequest.STANDBY);
//            return;
//        }
//        blade.getThrower().getAspects().formCur();
//
//        // Enlarge blade and set glowing blue (assuming blade has methods or display supports these)
//        blade.getDisplay().setTransformation(
//            new Transformation(
//                new Vector3f(),
//                new Quaternionf(),
//                new Vector3f(2f, 2f, 2f),
//                new Quaternionf()
//            )
//        );
//        blade.getDisplay().setGlowing(true);
//        blade.getDisplay().setGlowColorOverride(Color.fromRGB(27, 154, 239));
//
//        startSpinAndRise();
//    }
//
//    private void startSpinAndRise() {
//        final Location targetLoc = target.getLocation().clone().add(0, 3, 0); // 3 blocks above target
//
//        spinTask = new BukkitRunnable() {
//            int ticks = 0;
//
//            @Override
//            public void run() {
//                if (ticks >= SPIN_UP_DURATION || blade.getThrower().isInvalid() || target.isInvalid()) {
//                    spinTask.cancel();
//                    startCrashDown();
//                    return;
//                }
//
//                // Gradually increase spin speed
//                spinSpeed = Math.min(MAX_SPIN_SPEED, spinSpeed + 0.003f);
//
//                // Update angle
//                spinAngle += spinSpeed;
//
//                // Calculate circle position around player
//                double x = playerLoc.getX() + SPIN_RADIUS * Math.cos(spinAngle);
//                double z = playerLoc.getZ() + SPIN_RADIUS * Math.sin(spinAngle);
//
//                // Linearly interpolate height from player Y to target above Y
//                double y = playerLoc.getY() + ((targetLoc.getY() - playerLoc.getY()) * ((double)ticks / SPIN_UP_DURATION));
//
//                Location newLoc = new Location(playerLoc.getWorld(), x, y, z);
//
//                // Calculate direction toward spin center for orientation
//                Vector direction = playerLoc.toVector().subtract(newLoc.toVector()).normalize();
//
//                // Apply transform rotation: Set translation and rotate the item display
//                blade.getDisplay().setTransformation(new Transformation(
//                    new Vector3f((float)(x - playerLoc.getX()), (float)(y - playerLoc.getY()), (float)(z - playerLoc.getZ())),
//                    new Quaternionf().rotationY((float)spinAngle * 5f),  // spin about Y axis, faster multiplier for visual effect
//                    new Vector3f(2f, 2f, 2f),  // scaled size
//                    new Quaternionf()
//                ));
//
//                ticks++;
//            }
//        }.runTaskTimer(Sword.getInstance(), 0L, 1L);
//    }
//
//    private void startCrashDown() {
//        final Location startLoc = blade.getDisplay().getLocation().clone();
//        final Location endLoc = target.getLocation().clone();
//
//        final int totalTicks = CRASH_DURATION;
//        crashTask = new BukkitRunnable() {
//            int ticksElapsed = 0;
//
//            @Override
//            public void run() {
//                if (ticksElapsed >= totalTicks || blade.getThrower().isInvalid() || target.isInvalid()) {
//                    crashTask.cancel();
//                    blade.getDisplay().setGlowing(false);
//                    blade.setScale(new Vector3f(1f, 1f, 1f));
//                    return;
//                }
//
//                double t = (double)ticksElapsed / totalTicks;
//
//                // Interpolate position
//                double x = startLoc.getX() + (endLoc.getX() - startLoc.getX()) * t;
//                double y = startLoc.getY() + (endLoc.getY() - startLoc.getY()) * t;
//                double z = startLoc.getZ() + (endLoc.getZ() - startLoc.getZ()) * t;
//
//                Location newLoc = new Location(startLoc.getWorld(), x, y, z);
//                blade.getDisplay().teleport(newLoc.setDirection(endLoc.toVector().subtract(newLoc.toVector()).normalize()));
//
//                // Optional: add impact particles starting late in the crash animation
//                if (ticksElapsed > totalTicks * 0.75) {
//                    Prefab.Particles.EXPLOSION.display(newLoc);
//                }
//
//                ticksElapsed++;
//            }
//        }.runTaskTimer(Sword.getInstance(), 0L, 1L);
//
//        // Schedule impale and finishing after crash ends
//        Bukkit.getScheduler().runTaskLater(Sword.getInstance(), () -> {
//            if (!target.isInvalid()) {
//                blade.impale(target.entity());
//            }
//            // Reset blade scale and glow on finishing
//            blade.getDisplay().setGlowing(false);
//            blade.setScale(new Vector3f(1f, 1f, 1f));
//            blade.request(BladeRequest.STANDBY);
//        }, totalTicks + 1L);
//    }
//
//    @Override
//    public void onExit(UmbralBlade blade) {
//        if (spinTask != null) spinTask.cancel();
//        if (crashTask != null) crashTask.cancel();
//
//        blade.getDisplay().setGlowing(false);
//        blade.setScale(new Vector3f(1f, 1f, 1f));
//
//        blade.setSkillFinished(true);
//    }
//
//    @Override
//    public void onTick(UmbralBlade blade) {
//        // No continuous updates needed, handled by scheduled tasks
//    }
}
