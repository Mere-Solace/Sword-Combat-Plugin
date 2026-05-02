package btm.sword.action.throwing;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;

import btm.sword.Sword;
import btm.sword.action.throwing.types.Projectile;

/**
 * Manages all ACTIVE {@link Projectile} instances and drives their physics loop
 * from a single repeating Bukkit task.
 * <p>
 * {@link btm.sword.umbral.UmbralBlade UmbralBlade} and other FSM-driven
 * projectiles are <em>not</em> registered here — they are ticked by their own state machine.
 * Only standalone projectiles spawned via {@link Projectile#launch} are registered.
 * </p>
 */
public final class ProjectileManager {

    private ProjectileManager() {}

    private static final List<Projectile> ACTIVE = new ArrayList<>();

    /**
     * Registers a {@link Projectile} so that it will be ticked each physics step.
     *
     * @param projectile the projectile to track
     */
    public static void register(Projectile projectile) {
        ACTIVE.add(projectile);
    }

    /**
     * Starts the global physics loop. Call once from {@code Sword.onEnable()}.
     * The loop runs every server tick and removes projectiles whose
     * {@link Projectile#tick()} returns {@code false}.
     */
    public static void startTicking() {
        Bukkit.getScheduler().runTaskTimer(
            Sword.getInstance(),
            () -> ACTIVE.removeIf(p -> !p.tick()),
            0L, 1L
        );
    }
}
