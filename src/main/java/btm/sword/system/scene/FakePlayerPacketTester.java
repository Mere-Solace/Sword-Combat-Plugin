package btm.sword.system.scene;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.comphenix.protocol.wrappers.EnumWrappers;

import btm.sword.Sword;
import btm.sword.system.entity.impl.SwordPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * In-game packet test harness for {@link FakePlayerManager}.
 *
 * <h2>Looping tests</h2>
 * <p>Each {@code loop*} method starts a repeating task (every 2 seconds) that sends its
 * specific packet to the active fake player NPC. The NPC is spawned once on the first
 * iteration and kept alive between iterations. If a packet call returns {@code false},
 * the loop cancels itself and messages the viewer.</p>
 *
 * <p>Only one loop can be active per player at a time. Starting a new loop cancels the
 * previous one. Call {@link #stopLoop} to stop manually and despawn the NPC.</p>
 *
 * <h2>Run All</h2>
 * <p>{@link #runAll} is a single-shot batch: spawns, sends each packet once, despawns.
 * Use it to quickly check all six types without looping.</p>
 *
 * <h2>Console output</h2>
 * <p>All results (PASS / FAIL) are logged. Failures include a full stack trace via
 * {@link Logger#log(Level, String, Throwable)}.</p>
 */
public final class FakePlayerPacketTester {

    private FakePlayerPacketTester() {}

    /** Repeating tasks keyed by the viewer's UUID. At most one active per player. */
    private static final Map<UUID, BukkitTask> ACTIVE_LOOPS = new HashMap<>();

    /** Repeat interval for looping tests, in server ticks (20 ticks = 1 second). */
    private static final long LOOP_INTERVAL_TICKS = 40L;

    // =========================================================================
    // Public loop API
    // =========================================================================

    /**
     * Starts a looping test for {@code REL_ENTITY_MOVE}: moves the NPC +0.5 blocks on the
     * X axis every {@value #LOOP_INTERVAL_TICKS} ticks. Cancels any previously active loop.
     *
     * @param viewer the player to run the test against
     */
    public static void loopMoveRelative(SwordPlayer viewer) {
        startLoop(viewer, "REL_ENTITY_MOVE",
            () -> FakePlayerManager.moveRelative(viewer, 0.5, 0.0, 0.0, true));
    }

    /**
     * Starts a looping test for {@code ENTITY_TELEPORT}: snaps the NPC +1 block upward
     * from its spawn position every {@value #LOOP_INTERVAL_TICKS} ticks.
     *
     * @param viewer the player to run the test against
     */
    public static void loopTeleport(SwordPlayer viewer) {
        Location spawn = spawnAhead(viewer);
        Location target = spawn.clone().add(0, 1, 0);
        startLoop(viewer, "ENTITY_TELEPORT",
            () -> FakePlayerManager.teleportFake(viewer, target));
    }

    /**
     * Starts a looping test for {@code ENTITY_LOOK} + {@code ENTITY_HEAD_ROTATION}: rotates
     * the NPC to 90° yaw, 20° pitch every {@value #LOOP_INTERVAL_TICKS} ticks.
     *
     * @param viewer the player to run the test against
     */
    public static void loopRotate(SwordPlayer viewer) {
        startLoop(viewer, "ENTITY_LOOK + ENTITY_HEAD_ROTATION",
            () -> FakePlayerManager.rotateFake(viewer, 90.0f, 20.0f, true));
    }

    /**
     * Starts a looping test for {@code ANIMATION}: triggers a main-hand swing on the NPC
     * every {@value #LOOP_INTERVAL_TICKS} ticks.
     *
     * @param viewer the player to run the test against
     */
    public static void loopAnimate(SwordPlayer viewer) {
        startLoop(viewer, "ANIMATION (SWING_MAIN_HAND)",
            () -> FakePlayerManager.animateFake(viewer, FakeAnimation.SWING_MAIN_HAND));
    }

    /**
     * Starts a looping test for {@code ENTITY_VELOCITY}: sends an upward impulse (~jump
     * strength) every {@value #LOOP_INTERVAL_TICKS} ticks.
     *
     * @param viewer the player to run the test against
     */
    public static void loopVelocity(SwordPlayer viewer) {
        startLoop(viewer, "ENTITY_VELOCITY",
            () -> FakePlayerManager.velocityFake(viewer, 0.0, 0.42, 0.0));
    }

    /**
     * Starts a looping test for {@code ENTITY_DATA} living-entity flags: equips a shield in the
     * NPC's main hand, then sets the hand-active bit (blocking pose), every
     * {@value #LOOP_INTERVAL_TICKS} ticks.
     *
     * <p>The shield equipment packet is sent on each iteration because the NPC may be
     * re-spawned mid-loop with no equipment. Sending it every cycle is harmless.</p>
     *
     * @param viewer the player to run the test against
     */
    public static void loopEntityFlags(SwordPlayer viewer) {
        startLoop(viewer, "ENTITY_DATA (living entity flags — blocking)", () -> {
            FakePlayerManager.setEquipmentSlot(
                viewer, EnumWrappers.ItemSlot.MAINHAND, ItemStack.of(Material.SHIELD));
            return FakePlayerManager.setEntityFlags(viewer, true, false, false);
        });
    }

    /**
     * Cancels the active loop for the given viewer and despawns the NPC.
     * Does nothing if no loop is running.
     *
     * @param viewer the player whose loop to stop
     */
    public static void stopLoop(SwordPlayer viewer) {
        BukkitTask task = ACTIVE_LOOPS.remove(viewer.player().getUniqueId());
        if (task != null) {
            task.cancel();
            FakePlayerManager.despawnFakePlayer(viewer);
        }
    }

    /**
     * Returns whether a loop is currently active for the given viewer.
     *
     * @param viewer the player to check
     * @return {@code true} if a loop task is running
     */
    public static boolean hasActiveLoop(SwordPlayer viewer) {
        return ACTIVE_LOOPS.containsKey(viewer.player().getUniqueId());
    }

    // =========================================================================
    // Run All (single-shot)
    // =========================================================================

    /**
     * Single-shot batch test: spawns a fresh NPC, sends each of the six packet types once,
     * then despawns. Results and stack traces are logged to the server console.
     *
     * @param viewer the player to run tests against
     * @return {@code true} if every packet was sent without error
     */
    public static boolean runAll(SwordPlayer viewer) {
        Logger log = log();
        log.info("[PacketTester] === Running all fake player packet tests ===");
        FakePlayerManager.spawnFakePlayer(viewer, spawnAhead(viewer));

        int passed = 0;
        if (run(log, "REL_ENTITY_MOVE",
            () -> assertSent(FakePlayerManager.moveRelative(viewer, 0.5, 0.0, 0.0, true)))) passed++;
        if (run(log, "ENTITY_TELEPORT",
            () -> assertSent(FakePlayerManager.teleportFake(viewer, spawnAhead(viewer).add(0, 1, 0))))) passed++;
        if (run(log, "ENTITY_LOOK + ENTITY_HEAD_ROTATION",
            () -> assertSent(FakePlayerManager.rotateFake(viewer, 90.0f, 20.0f, true)))) passed++;
        if (run(log, "ANIMATION (SWING_MAIN_HAND)",
            () -> assertSent(FakePlayerManager.animateFake(viewer, FakeAnimation.SWING_MAIN_HAND)))) passed++;
        if (run(log, "ENTITY_VELOCITY",
            () -> assertSent(FakePlayerManager.velocityFake(viewer, 0.0, 0.42, 0.0)))) passed++;
        if (run(log, "ENTITY_DATA (living entity flags — blocking)", () -> {
            assertSent(FakePlayerManager.setEquipmentSlot(
                viewer, EnumWrappers.ItemSlot.MAINHAND, ItemStack.of(Material.SHIELD)));
            assertSent(FakePlayerManager.setEntityFlags(viewer, true, false, false));
        })) passed++;

        FakePlayerManager.despawnFakePlayer(viewer);
        log.info("[PacketTester] === Results: " + passed + "/6 passed ===");
        return passed == 6;
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Starts (or replaces) a looping task for the given viewer.
     *
     * <p>The NPC is spawned once on the first tick if not already active, then kept alive
     * between iterations. On failure, the loop cancels, the NPC is despawned, and the
     * viewer is messaged.</p>
     *
     * @param viewer  the player to loop against
     * @param label   short packet name logged on each iteration
     * @param packetFn the packet send operation; returns {@code true} on success
     */
    private static void startLoop(SwordPlayer viewer, String label, BooleanPacketOp packetFn) {
        // Cancel any existing loop first (does not despawn — new loop takes over the NPC)
        BukkitTask existing = ACTIVE_LOOPS.remove(viewer.player().getUniqueId());
        if (existing != null) existing.cancel();

        UUID uuid = viewer.player().getUniqueId();
        Logger log = log();

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Spawn NPC on first iteration or if it was externally despawned
                if (!FakePlayerManager.isActive(viewer)) {
                    FakePlayerManager.spawnFakePlayer(viewer, spawnAhead(viewer));
                }

                boolean passed = FakePlayerPacketTester.run(log, label, () -> assertSent(packetFn.send()));
                if (!passed) {
                    ACTIVE_LOOPS.remove(uuid);
                    FakePlayerManager.despawnFakePlayer(viewer);
                    viewer.message(Component.text(
                        "[PacketTester] Loop stopped — " + label + " failed. See console.",
                        NamedTextColor.RED));
                    cancel();
                }
            }
        }.runTaskTimer(Sword.getInstance(), 0L, LOOP_INTERVAL_TICKS);

        ACTIVE_LOOPS.put(uuid, task);
    }

    /** Returns a location 2 blocks horizontally in front of the viewer. */
    private static Location spawnAhead(SwordPlayer viewer) {
        Location loc = viewer.player().getLocation();
        return loc.clone().add(loc.getDirection().setY(0).normalize().multiply(2));
    }

    private static Logger log() {
        return Sword.getInstance().getLogger();
    }

    /**
     * Runs a named test action, logging PASS or FAIL (with full stack trace) to the console.
     *
     * @return {@code true} if the action completed without throwing
     */
    private static boolean run(Logger log, String label, Runnable action) {
        try {
            action.run();
            log.info("[PacketTester] PASS: " + label);
            return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "[PacketTester] FAIL: " + label, e);
            return false;
        }
    }

    /** Throws {@link IllegalStateException} if the packet method returned {@code false}. */
    private static void assertSent(boolean result) {
        if (!result) {
            throw new IllegalStateException(
                "Packet method returned false — check the SEVERE log above for the root cause");
        }
    }

    /** Functional interface for a packet-send operation that returns a success flag. */
    @FunctionalInterface
    private interface BooleanPacketOp {
        boolean send();
    }
}
