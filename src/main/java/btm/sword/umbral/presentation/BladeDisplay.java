package btm.sword.umbral.presentation;

import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.runtime.scheduler.TimeArbiter;
import btm.sword.util.display.DisplayUtil;

/**
 * Single owner of an {@link ItemDisplay} handle for an UmbralBlade.
 *
 * <p>All write operations on the underlying {@code ItemDisplay} that originate from the
 * {@code btm.sword.umbral} subtree route through this class. Operations are guarded against
 * the {@link State#UNBOUND UNBOUND} state so calls into a not-yet-spawned (or disposed)
 * display are safe no-ops.</p>
 *
 * <p>Lifecycle: {@code UNBOUND -> BOUND -> UNBOUND}. {@link #adopt} or {@link #respawn}
 * binds a handle; {@link #dispose} releases it. {@link #respawn} on an already-bound display
 * disposes the existing entity first, so the BOUND-to-BOUND replacement path leaks nothing.
 * All public operations are idempotent on the UNBOUND state.</p>
 *
 * <p>Bobbing animation tasks (the cosine idle wobble) are owned exclusively by this class
 * via the {@link #bobbingTask} field. {@link #dispose} and {@link #respawn} both cancel the
 * task before mutating the handle so a stale task can never reference a disposed entity.</p>
 *
 * <p>This class deliberately knows nothing about the FSM or the motion subsystem — it is the
 * presentation layer for the blade.</p>
 */
public final class BladeDisplay {

    /** Lifecycle states for the wrapped handle. */
    public enum State {
        /** No handle held. All operations are no-ops; {@link BladeDisplay#isValid} is {@code false}. */
        UNBOUND,
        /** A handle is held. Operations route to the underlying {@code ItemDisplay}. */
        BOUND
    }

    private final Vector3f scale;

    private ItemDisplay handle;
    private TimeArbiter.TaskHandle bobbingTask;
    private State state = State.UNBOUND;

    /**
     * Creates an unbound BladeDisplay using the given scale for bobbing transformations.
     *
     * @param scale the {@link Vector3f} scale to apply to bobbing transformations
     *              (defensively copied to keep this class's invariants independent of caller mutations)
     */
    public BladeDisplay(Vector3f scale) {
        this.scale = new Vector3f(Objects.requireNonNull(scale, "scale"));
    }

    /**
     * Binds an externally-created {@link ItemDisplay} handle to this wrapper.
     *
     * <p>Used during transition from inheritance-based display creation: the parent constructor
     * spawns the entity, then {@code UmbralBlade} adopts the resulting handle into its
     * {@code BladeDisplay} so subsequent operations route through the wrapper.</p>
     *
     * @param existing the handle to bind; must be non-null and valid
     * @throws IllegalStateException if already bound (call {@link #dispose} first)
     */
    public void adopt(ItemDisplay existing) {
        Objects.requireNonNull(existing, "existing");
        if (state == State.BOUND) {
            throw new IllegalStateException("BladeDisplay already bound; dispose before re-adopting");
        }
        this.handle = existing;
        this.state = State.BOUND;
    }

    /**
     * Disposes any current handle and spawns a fresh {@link ItemDisplay} at the given origin's
     * eye location, then runs {@code setup} on the new handle.
     *
     * <p>Idempotent on rapid successive calls — each call disposes the previous handle before
     * spawning the new one.</p>
     *
     * @param origin the entity whose eye location is used as the spawn point
     * @param setup  consumer applied to the freshly-spawned handle
     * @return the newly-bound {@link ItemDisplay} handle
     */
    public ItemDisplay respawn(LivingEntity origin, Consumer<ItemDisplay> setup) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(setup, "setup");
        dispose();
        ItemDisplay spawned = (ItemDisplay) origin.getWorld()
            .spawnEntity(origin.getEyeLocation(), EntityType.ITEM_DISPLAY);
        this.handle = spawned;
        this.state = State.BOUND;
        setup.accept(spawned);
        return spawned;
    }

    /** Returns {@code true} if a handle is bound and the underlying entity is still valid. */
    public boolean isValid() {
        return state == State.BOUND && handle != null && handle.isValid();
    }

    /** Returns the current world-space {@link Location} of the display, or {@code null} if unbound/invalid. */
    public Location currentLocation() {
        return isValid() ? handle.getLocation() : null;
    }

    /** Returns the current {@link Transformation} of the display, or {@code null} if unbound/invalid. */
    public Transformation currentTransformation() {
        return isValid() ? handle.getTransformation() : null;
    }

    /**
     * Returns the underlying {@link ItemDisplay} handle for transitional callers that have not yet
     * been migrated off the raw entity reference (currently {@code UmbralBladeAttack} construction
     * and inherited physics methods).
     *
     * <p>This accessor is removed when inheritance is severed in step 9 of the decoupling refactor.</p>
     *
     * @return the bound handle, or {@code null} if unbound
     */
    public ItemDisplay handle() {
        return handle;
    }

    /**
     * Smoothly teleports the display to {@code to} along {@code direction} over {@code duration} ticks.
     * No-op if unbound or invalid.
     */
    public void teleportTo(Location to, Vector direction, int duration) {
        if (!isValid()) return;
        TimeArbiter.teleportDisplay(handle, to, direction, duration, BladeDisplay.class, 0);
    }

    /**
     * Schedules a smooth {@link Transformation} transition over {@code duration} ticks.
     * No-op if unbound or invalid.
     */
    public void setTransformation(Transformation transformation, int duration) {
        if (!isValid()) return;
        TimeArbiter.setDisplayTransformation(handle, transformation, duration);
    }

    /**
     * Applies a {@link Transformation} immediately with no interpolation.
     * No-op if unbound or invalid.
     */
    public void setTransformationImmediate(Transformation transformation) {
        if (!isValid()) return;
        handle.setTransformation(transformation);
    }

    /** Configures interpolation timing on the display. No-op if unbound or invalid. */
    public void setInterpolation(int delay, int duration) {
        if (!isValid()) return;
        DisplayUtil.setInterpolationValues(handle, delay, duration);
    }

    /**
     * Starts a cosine bobbing animation on the display, replacing any existing bobbing task.
     *
     * <p>Each tick of the timer applies a fresh {@link Transformation} with a vertical offset of
     * {@code cos(step) * amplitude}, preserving the current left rotation. {@link #stopBobbing}
     * cancels it; {@link #dispose} cancels it implicitly.</p>
     *
     * @param amplitude vertical bob amplitude in world units
     * @param period    bobbing period in ticks
     */
    public void startBobbing(double amplitude, int period) {
        stopBobbing();
        if (!isValid()) return;
        final double[] step = {0};
        bobbingTask = TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            () -> {
                if (!isValid()) return;
                TimeArbiter.setDisplayTransformation(handle,
                    new Transformation(
                        new Vector3f(0, (float) (Math.cos(step[0]) * amplitude), 0),
                        handle.getTransformation().getLeftRotation(),
                        scale,
                        new Quaternionf()
                    ),
                    period
                );
                step[0] += Math.PI / 8;
            },
            null,
            null,
            0, period,
            BladeDisplay.class, "startBobbing"
        );
    }

    /** Cancels the bobbing animation if running. Idempotent. */
    public void stopBobbing() {
        if (bobbingTask != null) {
            bobbingTask.cancel();
            bobbingTask = null;
        }
    }

    /**
     * Cancels the bobbing task, removes the underlying entity if valid, and unbinds the handle.
     * Idempotent — calling on an already-disposed wrapper is a no-op.
     */
    public void dispose() {
        stopBobbing();
        if (handle != null && handle.isValid()) {
            handle.remove();
        }
        handle = null;
        state = State.UNBOUND;
    }
}
