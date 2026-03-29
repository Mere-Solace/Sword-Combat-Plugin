package btm.sword.system.entity.display;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.mob.AnimationSlots;
import btm.sword.utility.SwordTimeUnit;
import net.donnypz.displayentityutils.events.GroupSpawnedEvent;
import net.donnypz.displayentityutils.managers.DisplayGroupManager;
import net.donnypz.displayentityutils.managers.LoadMethod;
import net.donnypz.displayentityutils.utils.DisplayEntities.DisplayAnimator;
import net.donnypz.displayentityutils.utils.DisplayEntities.DisplayEntityGroup;
import net.donnypz.displayentityutils.utils.DisplayEntities.SpawnedDisplayEntityGroup;
import net.donnypz.displayentityutils.utils.DisplayEntities.SpawnedDisplayEntityPart;
import net.donnypz.displayentityutils.utils.DisplayEntities.machine.DisplayStateMachine;
import net.donnypz.displayentityutils.utils.DisplayEntities.machine.MachineState;
import net.donnypz.displayentityutils.utils.FollowType;
import net.donnypz.displayentityutils.utils.controller.GroupFollowProperties;

/**
 * Visual display rig attached to a {@link Mob}.
 *
 * <p>Wraps a {@link SpawnedDisplayEntityGroup} and a {@link DisplayStateMachine}.
 * The machine automatically transitions between IDLE, WALK, and FALLING states based
 * on the mob's movement. The MELEE state is triggered manually by the AI FSM
 * (see {@link btm.sword.system.entity.ai.state.AttackState}). The DEATH state, when
 * present, is triggered by {@link #triggerDeath()} and plays once before the mob is killed.</p>
 *
 * <p>Weapon slot display: if the DEU group contains a {@link Material#LIGHTNING_ROD}
 * that entity is used as the weapon-slot anchor. Call {@link #setWeaponSlotItem(ItemStack)}
 * to show an item at the mob's hand; the display follows the mob's position and yaw.
 * Per-material offset/rotation/scale is loaded from {@link WeaponDisplayRegistry}.</p>
 *
 * <p>Call {@link #spawn(Mob, String, AnimationSlots)} to create a rig.
 * Call {@link #despawn()} when the mob dies to clean up entities.</p>
 */
public class DisplayRig {

    /**
     * Scoreboard tag added by {@link btm.sword.listeners.EntityListener} to any
     * {@link ItemDisplay} that holds a {@link Material#LIGHTNING_ROD} item at spawn.
     */
    public static final String WEAPON_SLOT_TAG = "sword_weapon_slot";

    private final Mob mob;
    private final SpawnedDisplayEntityGroup group;
    private final DisplayStateMachine stateMachine;
    /** Non-empty when a die animation was registered; used by the animation-complete listener. */
    private final String dieAnimTag;

    /**
     * Set to {@code true} by {@link #lockOnDeath()} once the death sequence begins.
     * Any subsequent {@link #setState} calls are silently dropped so the DEATH
     * animation cannot be interrupted or replaced.
     */
    private boolean isDying;

    /** The tiny LIGHTNING_ROD anchor within the DEU group that marks the main-hand weapon slot. */
    private final @Nullable ItemDisplay weaponAnchor;

    /**
     * The DEU part wrapper for the weapon anchor, used to call DEU's own transform setters
     * (which ultimately delegate to the Bukkit Display API but keep the call within DEU's model).
     */
    private final @Nullable SpawnedDisplayEntityPart weaponAnchorPart;

    /** Periodic refresh task; non-null while a weapon is actively shown. */
    private @Nullable TimeArbiter.TaskHandle weaponRefreshTask;

    /**
     * The item currently shown in the weapon slot, or {@code null} when hidden.
     * Used by {@link #reapplyWeaponTransform()} to re-apply after DEU state transitions.
     */
    private @Nullable ItemStack currentWeaponItem;

    private DisplayRig(Mob mob, SpawnedDisplayEntityGroup group, DisplayStateMachine stateMachine,
            String dieAnimTag, @Nullable ItemDisplay weaponAnchor) {
        this.mob = mob;
        this.group = group;
        this.stateMachine = stateMachine;
        this.dieAnimTag = dieAnimTag;
        this.weaponAnchor = weaponAnchor;
        this.weaponAnchorPart = weaponAnchor != null
            ? SpawnedDisplayEntityPart.getPart(weaponAnchor) : null;
    }

    /**
     * Spawns a display rig for the given mob using the named DEU group tag and per-type
     * animation slots.
     *
     * @param mob      the mob to attach the rig to
     * @param groupTag the DEU group tag to spawn
     * @param slots    the animation slot tags to register on the state machine
     * @return the new rig, or {@code null} if the group cannot be found in DEU's local storage
     */
    public static @Nullable DisplayRig spawn(Mob mob, String groupTag, AnimationSlots slots) {
        DisplayEntityGroup def = DisplayGroupManager.getGroup(LoadMethod.LOCAL, groupTag);
        if (def == null) {
            Sword.getInstance().getLogger().warning(
                "[DisplayRig] Group not found: " + groupTag + " — skipping display rig."
            );
            return null;
        }

        SpawnedDisplayEntityGroup group;
        try {
            group = def.spawn(mob.getLocation(), GroupSpawnedEvent.SpawnReason.CUSTOM);
        } catch (Exception e) {
            Sword.getInstance().getLogger().warning(
                "[DisplayRig] Exception while spawning group '" + groupTag + "': " + e.getMessage()
            );
            return null;
        }
        if (group == null) return null;

        try {
            group.rideEntity(mob);
        } catch (Exception e) {
            Sword.getInstance().getLogger().warning(
                "[DisplayRig] Exception while mounting group '" + groupTag + "' to mob: " + e.getMessage()
            );
            group.unregister(true, true);
            return null;
        }

        for (Display display : group.getPartEntities(Display.class)) {
            display.setTeleportDuration(Config.Hostile.DISPLAY_TELEPORT_DURATION);
        }

        GroupFollowProperties yawFollow = GroupFollowProperties.builder(FollowType.YAW)
            .setId("hostile_yaw")
            .setTeleportationDuration(Config.Hostile.DISPLAY_TELEPORT_DURATION)
            .build();
        group.followEntityDirection(mob, yawFollow);

        DisplayStateMachine stateMachine = new DisplayStateMachine(mob.getUniqueId() + "_rig");
        addState(stateMachine, MachineState.StateType.IDLE,    slots.idle().tag(),   false);
        addState(stateMachine, MachineState.StateType.WALK,    slots.walk().tag(),   false);
        addState(stateMachine, MachineState.StateType.FALLING, slots.fall().tag(),   false);
        addState(stateMachine, MachineState.StateType.MELEE,   slots.attack().tag(), true);
        addState(stateMachine, MachineState.StateType.DEATH,   slots.die().tag(),    true);
        stateMachine.addGroup(group);

        // Find the main-hand weapon-slot anchor: a LIGHTNING_ROD ItemDisplay at near-zero scale.
        ItemDisplay weaponAnchor = null;
        for (ItemDisplay id : group.getPartEntities(ItemDisplay.class)) {
            ItemStack item = id.getItemStack();
            if (item.getType() != Material.LIGHTNING_ROD) continue;
            weaponAnchor = id;
            break;
        }


        String dieTag = slots.die().tag() != null ? slots.die().tag() : "";
        DisplayRig rig = new DisplayRig(mob, group, stateMachine, dieTag, weaponAnchor);
        DEUAnimationHook.track(group, rig);
        return rig;
    }

    // -----------------------------------------------------------------------
    // Weapon slot

    /**
     * Shows the given item at the mob's main-hand weapon slot, or hides the slot.
     *
     * <p>This method drives two distinct hook states:</p>
     * <ul>
     *   <li><b>Armed</b> ({@code item} is a real weapon): registers a {@link WeaponAnchorPacketHook}
     *       override that forces the weapon item, scale, and rotation offset on every outgoing
     *       metadata packet for the anchor. A 5-tick refresh loop triggers periodic Bukkit-side
     *       metadata flushes so DEU state-machine resets are overridden promptly.</li>
     *   <li><b>Hidden</b> ({@code item} is {@code null} or AIR): registers the hook with
     *       {@code AIR} and near-zero scale so DEU's own reset packets (which would restore the
     *       LIGHTNING_ROD marker) are still intercepted and suppressed.</li>
     * </ul>
     *
     * @param item the item to display, or {@code null} to hide the slot
     */
    public void setWeaponSlotItem(@Nullable ItemStack item) {
        // Tear down previous state before registering the new one.
        stopWeaponRefreshTask();
        if (weaponAnchor != null) WeaponAnchorPacketHook.clear(weaponAnchor);

        if (weaponAnchor == null || !weaponAnchor.isValid()) return;

        if (item == null || item.getType() == Material.AIR) {
            // Hidden state: keep the hook active so DEU's item-reset packets are suppressed.
            currentWeaponItem = null;
            WeaponAnchorPacketHook.override(
                weaponAnchor, new ItemStack(Material.AIR), 0.001f, new Quaternionf(), new Vector3f());
            weaponAnchor.setGlowing(false);
            weaponAnchor.setItemStack(new ItemStack(Material.AIR));
            return;
        }

        // Armed state.
        currentWeaponItem = item;

        final ItemDisplay anchor = weaponAnchor;

        // 5-tick refresh: forces a Bukkit metadata packet so DEU state-machine resets are
        // overridden within one loop cycle. The packet hook intercepts these too.
        weaponRefreshTask = TimeArbiter.runTimeBoundBukkitTaskOnTimer(
            () -> {
                if (!mob.isValid() || !anchor.isValid()) return;
                anchor.setGlowing(true);
                anchor.setGlowColorOverride(Config.SwordColor.ATTACK_QUICK_GLOW);
                anchor.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND);
                anchor.setItemStack(item);
            },
            null, null, 0, 1,
            DisplayRig.class, "setWeaponSlotItem"
        );
    }

    // -----------------------------------------------------------------------
    // State machine

    /**
     * Manually overrides the current animation state.
     * No-op if the rig is locked in the death animation ({@link #lockOnDeath()} was called).
     *
     * @param type the state to transition to
     */
    public void setState(MachineState.StateType type) {
        if (isDying) return;
        stateMachine.setState(type, group);
    }

    /**
     * Triggers the DEATH animation if one was registered.
     * If no die animation was registered this is a no-op.
     */
    public void triggerDeath() {
        if (!dieAnimTag.isEmpty()) {
            stateMachine.setState(MachineState.StateType.DEATH, group);
        }
    }

    /**
     * Freezes the rig in place and locks all subsequent animation state changes.
     *
     * <p>Called when the mob enters its death sequence. After this:</p>
     * <ul>
     *   <li>The group stops riding the mob — its server-side position is frozen
     *       at the point of death regardless of mob movement or gravity.</li>
     *   <li>The yaw follow is cancelled.</li>
     *   <li>{@link #setState} becomes a permanent no-op, preventing the state
     *       machine from transitioning away from DEATH.</li>
     * </ul>
     */
    public void lockOnDeath() {
        isDying = true;
        group.dismount();
        group.stopFollowingEntity();
    }

    /** Returns {@code true} if a die animation was registered for this rig. */
    public boolean hasDieAnimation() {
        return !dieAnimTag.isEmpty();
    }

    /**
     * Returns the DEU animation tag registered for the DEATH state,
     * or an empty string if none was registered.
     */
    public String dieAnimTag() {
        return dieAnimTag;
    }

    /** Returns the underlying {@link SpawnedDisplayEntityGroup}. */
    public SpawnedDisplayEntityGroup group() {
        return group;
    }

    // -----------------------------------------------------------------------
    // Lifecycle

    /**
     * Re-applies the currently armed weapon transform to the anchor using DEU's part API.
     *
     * <p>Called by {@link DEUAnimationHook} a few ticks after a state-machine transition.
     * DEU's {@code playUsingPackets} sends frame-0 packets asynchronously; if those packets
     * bypass ProtocolLib's pipeline position, a brief reset window can occur. This call
     * re-asserts the correct server-side entity state (scale + item) so that both
     * the ProtocolLib hook and any subsequent server-to-client resends reflect our values.</p>
     */
    public void reapplyWeaponTransform() {
        if (currentWeaponItem == null || weaponAnchorPart == null || weaponAnchor == null) return;
        if (!weaponAnchor.isValid()) return;
        WeaponDisplayTransform t = WeaponDisplayRegistry.get(currentWeaponItem.getType());
        // Use DEU's part API to set scale — sends a Bukkit metadata packet that the hook intercepts.
        weaponAnchorPart.setDisplayScale(t.scale(), t.scale(), t.scale());
        weaponAnchor.setItemStack(currentWeaponItem);
    }

    /**
     * Removes the spawned group from the world and unregisters the state machine.
     * Must be called when the mob dies or is removed.
     *
     * <p>State-machine and packet-hook teardown happens synchronously so no further
     * animation or weapon-override packets are sent.  The actual entity removal
     * ({@code group.unregister}) is deferred one tick because this method is often
     * invoked during {@code EntityRemoveFromWorldEvent}, at which point Paper's chunk
     * system may have the section locked and will silently drop any {@code entity.remove()}
     * calls made on the same tick.</p>
     */
    public void despawn() {
        DEUAnimationHook.untrack(group);
        clearWeaponDisplay();
        stateMachine.removeGroup(group);
        SpawnedDisplayEntityGroup groupRef = this.group;
        SwordScheduler.runBukkitTaskLater(
            () -> groupRef.unregister(true, true),
            SwordTimeUnit.MILLISECONDS_PER_TICK,
            TimeUnit.MILLISECONDS
        );
    }

    // -----------------------------------------------------------------------
    // Private helpers

    /** Cancels the refresh task and fully removes the packet hook. Called only from {@link #despawn}. */
    private void clearWeaponDisplay() {
        stopWeaponRefreshTask();
        if (weaponAnchor != null) WeaponAnchorPacketHook.clear(weaponAnchor);
    }

    private void stopWeaponRefreshTask() {
        if (weaponRefreshTask != null) {
            weaponRefreshTask.cancel();
            weaponRefreshTask = null;
        }
    }

    private static void addState(
            DisplayStateMachine machine,
            MachineState.StateType type,
            String animTag,
            boolean transitionLock) {
        if (animTag == null || animTag.isEmpty()) return;
        DisplayAnimator.AnimationType animType = transitionLock
            ? DisplayAnimator.AnimationType.LINEAR
            : DisplayAnimator.AnimationType.LOOP;
        MachineState state = new MachineState(machine, type, List.of(animTag), LoadMethod.LOCAL, animType, transitionLock);
        machine.addState(state);
    }
}
