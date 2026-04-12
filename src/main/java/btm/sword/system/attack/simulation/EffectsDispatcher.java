package btm.sword.system.attack.simulation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import btm.sword.Sword;

/**
 * Fires per-keyframe particle and sound effects during a {@link VolumeSimulation} tick.
 *
 * <p>Called each tick with the previous and current normalized time values. Any
 * {@link VolumeKeyframe} whose {@code t} satisfies {@code prevT < kf.t() <= currentT}
 * has its {@link KeyframeEffect} dispatched. Because the Bukkit particle and sound APIs
 * require the main thread, effects are scheduled via {@link Bukkit#getScheduler()}.</p>
 *
 * <p>This class is stateless — all state lives in {@link SimulationAttack}.</p>
 */
public final class EffectsDispatcher {

    private EffectsDispatcher() {}

    /**
     * Checks all keyframes in {@code trajectory} for effects that should fire in the
     * {@code (prevT, currentT]} window and schedules them on the main thread.
     *
     * @param trajectory    the keyframed trajectory to scan
     * @param prevT         normalized time at the previous tick (negative on the first tick)
     * @param currentT      normalized time at the current tick
     * @param worldTransform local-to-world transform at this tick
     * @param world         world in which to spawn effects
     */
    public static void dispatch(KeyframedTrajectory trajectory, float prevT, float currentT,
            Matrix4f worldTransform, World world) {
        for (VolumeKeyframe kf : trajectory.getKeyframes()) {
            if (kf.t() <= prevT || kf.t() > currentT) continue;
            KeyframeEffect effect = kf.effect();
            if (effect == null) continue;

            Vector3f worldPos = worldTransform.transformPosition(new Vector3f(kf.localPosition()));
            Location loc = new Location(world, worldPos.x, worldPos.y, worldPos.z);

            Bukkit.getScheduler().runTask(Sword.getInstance(), () -> {
                for (ParticleEffect pe : effect.particles()) {
                    fireParticle(pe, loc, worldTransform, world);
                }
                if (effect.sound() != null) {
                    SoundCue sc = effect.sound();
                    world.playSound(loc, sc.sound(), sc.category(), sc.volume(), sc.pitch());
                }
            });
        }
    }

    private static void fireParticle(ParticleEffect pe, Location baseLoc,
            Matrix4f worldTransform, World world) {
        Vector3f worldOffset = worldTransform.transformDirection(new Vector3f(pe.offset()));
        Location spawnLoc = baseLoc.clone().add(worldOffset.x, worldOffset.y, worldOffset.z);
        double spread = pe.spread();

        if (pe.dustOptions() != null) {
            world.spawnParticle(pe.type(), spawnLoc, pe.count(),
                spread, spread, spread, 0, pe.dustOptions());
        } else {
            world.spawnParticle(pe.type(), spawnLoc, pe.count(),
                spread, spread, spread, 0);
        }
    }
}
