package btm.sword.system.attack.simulation;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.system.attack.visuals.EffectsContext;
import btm.sword.system.attack.visuals.ParticleDisplay;

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
     * @param seq     the keyframed trajectory to scan
     * @param attack         the running attack — source of attacker UUID and locked origin
     * @param prevT          normalized time at the previous tick (negative on the first tick)
     * @param currentT       normalized time at the current tick
     * @param worldTransform local-to-world transform at this tick
     * @param world          world in which to spawn effects
     */
    public static void dispatch(KeyframedSequence seq, SimulationAttack attack,
                                float prevT, float currentT, Matrix4f worldTransform, World world) {
        List<VolumeKeyframe> keyframes = seq.keyframes();
        for (int i = 0; i < keyframes.size(); i++) {
            VolumeKeyframe kf = keyframes.get(i);
            // Run all keyframes in between the time steps
            if (kf.t() <= prevT || kf.t() > currentT) continue;
            KeyframeEffect effect = kf.effect();
            if (effect == null) continue;

            Vector3f worldPos = worldTransform.transformPosition(new Vector3f(kf.localPosition()));
            Location loc = new Location(world, worldPos.x, worldPos.y, worldPos.z);

            float nextT = (i + 1 < keyframes.size()) ? keyframes.get(i + 1).t() : 1.0f;
            long windowMs = (long) ((nextT - kf.t()) * attack.getDurationMs());

            EffectsContext ctx = new EffectsContext(
                new Matrix4f(worldTransform),
                world,
                attack.getAttackerUuid(),
                seq,
                attack.getLockedCenter(),
                i);

            Bukkit.getScheduler().runTask(Sword.getInstance(), () -> {
                if (effect.displays() != null) {
                    for (ParticleDisplay display : effect.displays()) {
                        display.render(ctx);
                        int bkr = display.getBetweenKfRepeat();
                        if (bkr > 1 && windowMs > 0) {
                            for (int r = 1; r < bkr; r++) {
                                long delayTicks = Math.max(1, (windowMs * r / bkr) / 50);
                                Bukkit.getScheduler().runTaskLater(
                                    Sword.getInstance(),
                                    () -> display.renderOnce(ctx),
                                    delayTicks);
                            }
                        }
                    }
                }
                if (effect.sound() != null) {
                    SoundCue sc = effect.sound();
                    world.playSound(loc, sc.sound(), sc.category(), sc.volume(), sc.pitch());
                }
            });
        }
    }
}
