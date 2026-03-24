package btm.sword.system.entity.display;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import btm.sword.system.control.SwordScheduler;
import net.donnypz.displayentityutils.events.AnimationStateChangeEvent;
import net.donnypz.displayentityutils.utils.DisplayEntities.SpawnedDisplayEntityGroup;

/**
 * Bukkit event listener that re-applies weapon-slot transforms after DEU state-machine transitions.
 *
 * <p>DEU's {@link net.donnypz.displayentityutils.utils.DisplayEntities.machine.DisplayStateMachine}
 * always uses {@code playUsingPackets} when transitioning states, which sends per-player
 * PacketEvents metadata packets asynchronously from frame 0. If those packets reach a client
 * before the {@link WeaponAnchorPacketHook} intercepts them (due to pipeline ordering between
 * ProtocolLib and PacketEvents), the anchor briefly shows its vanilla state (LIGHTNING_ROD at
 * near-zero scale). This listener schedules a short-delay call to
 * {@link DisplayRig#reapplyWeaponTransform()} to close that window.</p>
 *
 * <p>Register once at plugin startup via {@link btm.sword.Sword#onEnable()}.</p>
 */
public final class DEUAnimationHook implements Listener {

    /**
     * Active rigs keyed by their DEU group. {@link ConcurrentHashMap} because
     * {@link AnimationStateChangeEvent} may fire from non-primary threads in some DEU versions.
     */
    private static final Map<SpawnedDisplayEntityGroup, DisplayRig> RIGS = new ConcurrentHashMap<>();

    /**
     * Registers a rig so that its weapon transform is re-applied after state transitions.
     * Called from {@link DisplayRig#spawn}.
     *
     * @param group the spawned group backing this rig
     * @param rig   the rig to track
     */
    public static void track(SpawnedDisplayEntityGroup group, DisplayRig rig) {
        RIGS.put(group, rig);
    }

    /**
     * Removes the rig from tracking. Called from {@link DisplayRig#despawn}.
     *
     * @param group the group to stop tracking
     */
    public static void untrack(SpawnedDisplayEntityGroup group) {
        RIGS.remove(group);
    }

    /**
     * After a state-machine transition, schedule a short-delay re-apply of the weapon transform.
     *
     * <p>The 2-tick delay gives DEU's async frame-0 packet send time to complete before we
     * assert server-side state. The re-apply packet is generated via the Bukkit API so
     * ProtocolLib can intercept and force correct values on the wire.</p>
     */
    @EventHandler
    public void onAnimationStateChange(AnimationStateChangeEvent event) {
        if (!(event.getGroup() instanceof SpawnedDisplayEntityGroup group)) return;
        DisplayRig rig = RIGS.get(group);
        if (rig == null) return;
        SwordScheduler.runBukkitTaskLater(rig::reapplyWeaponTransform, 100, TimeUnit.MILLISECONDS);
    }
}
