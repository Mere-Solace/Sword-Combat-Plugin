package btm.sword.system.entity.display;

import java.util.List;

import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.entity.mob.AnimationSlots;
import net.donnypz.displayentityutils.events.GroupSpawnedEvent;
import net.donnypz.displayentityutils.managers.DisplayGroupManager;
import net.donnypz.displayentityutils.managers.LoadMethod;
import net.donnypz.displayentityutils.utils.DisplayEntities.DisplayAnimator;
import net.donnypz.displayentityutils.utils.DisplayEntities.DisplayEntityGroup;
import net.donnypz.displayentityutils.utils.DisplayEntities.SpawnedDisplayEntityGroup;
import net.donnypz.displayentityutils.utils.DisplayEntities.machine.DisplayStateMachine;
import net.donnypz.displayentityutils.utils.DisplayEntities.machine.MachineState;
import net.donnypz.displayentityutils.utils.FollowType;
import net.donnypz.displayentityutils.utils.controller.GroupFollowProperties;

/**
 * Visual display rig attached to a {@link Mob}.
 * <p>
 * Wraps a {@link SpawnedDisplayEntityGroup} and a {@link DisplayStateMachine}.
 * The machine automatically transitions between IDLE, WALK, and FALLING states based
 * on the mob's movement. The MELEE state is triggered manually by the AI FSM
 * (see {@link btm.sword.system.entity.ai.state.AttackState}). The DEATH state, when
 * present, is triggered by {@link #triggerDeath()} and plays once before the mob is killed.
 * </p>
 *
 * <p>Call {@link #spawn(Mob, String, AnimationSlots)} to create a rig.
 * Call {@link #despawn()} when the mob dies to clean up entities.</p>
 */
public class DisplayRig {

    /**
     * Scoreboard tag added by {@link btm.sword.listeners.EntityListener} to any
     * {@link ItemDisplay} that holds a {@link org.bukkit.Material#LIGHTNING_ROD} item at spawn.
     * Place a LIGHTNING_ROD on the desired display entity part in your DEU group to designate
     * it as the weapon slot; the runtime item is then swapped via {@link #setWeaponSlotItem}.
     */
    public static final String WEAPON_SLOT_TAG = "sword_weapon_slot";

    private final SpawnedDisplayEntityGroup group;
    private final DisplayStateMachine stateMachine;
    /** Non-empty when a die animation was registered; used by the animation-complete listener. */
    private final String dieAnimTag;

    private DisplayRig(SpawnedDisplayEntityGroup group, DisplayStateMachine stateMachine, String dieAnimTag) {
        this.group = group;
        this.stateMachine = stateMachine;
        this.dieAnimTag = dieAnimTag;
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

        SpawnedDisplayEntityGroup group = def.spawn(mob.getLocation(), GroupSpawnedEvent.SpawnReason.CUSTOM);
        group.rideEntity(mob);
        // Offset the rig downward by the mob's height so the rig sits at ground level,
        // with DISPLAY_RIDE_OFFSET_Y as a fine-tuning knob on top of that.
        group.setRideOffset(new Vector(0, -mob.getHeight() + Config.Hostile.DISPLAY_RIDE_OFFSET_Y, 0));

        // Apply teleport duration to every display part for smooth client-side movement.
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

        return new DisplayRig(group, stateMachine, slots.die().tag() != null ? slots.die().tag() : "");
    }

    /**
     * Manually overrides the current animation state.
     * States registered with {@code transitionLock = true} (e.g. MELEE, DEATH) hold until
     * the animation completes, then the machine resumes automatic transitions.
     *
     * @param type the state to transition to
     */
    public void setState(MachineState.StateType type) {
        stateMachine.setState(type, group);
    }

    /**
     * Triggers the DEATH animation if one was registered. After the animation completes,
     * DEU fires {@link net.donnypz.displayentityutils.events.AnimationCompleteEvent}.
     * If no die animation was registered this is a no-op.
     */
    public void triggerDeath() {
        if (!dieAnimTag.isEmpty()) {
            stateMachine.setState(MachineState.StateType.DEATH, group);
        }
    }

    /**
     * Returns {@code true} if a die animation was registered for this rig.
     *
     * @return whether a DEATH state exists on the state machine
     */
    public boolean hasDieAnimation() {
        return !dieAnimTag.isEmpty();
    }

    /**
     * Returns the DEU animation tag registered for the DEATH state,
     * or an empty string if none was registered.
     *
     * @return the die animation tag
     */
    public String dieAnimTag() {
        return dieAnimTag;
    }

    /**
     * Returns the underlying {@link SpawnedDisplayEntityGroup}.
     *
     * @return the spawned group
     */
    public SpawnedDisplayEntityGroup group() {
        return group;
    }

    /**
     * Returns the weapon-slot {@link ItemDisplay} inside this rig — the part entity whose
     * scale was near-zero at spawn time and was tagged with {@link #WEAPON_SLOT_TAG} by
     * {@link btm.sword.listeners.EntityListener}.
     *
     * @return the tagged item display, or {@code null} if none was found in the group
     */
    public @Nullable ItemDisplay getWeaponSlot() {
        for (ItemDisplay display : group.getPartEntities(ItemDisplay.class)) {
            if (display.getScoreboardTags().contains(WEAPON_SLOT_TAG)) return display;
        }
        return null;
    }

    /**
     * Sets the item shown by the weapon-slot display entity.
     * Does nothing if this rig has no weapon slot.
     *
     * @param item the item to display; {@code null} or air clears the slot
     */
    public void setWeaponSlotItem(@Nullable ItemStack item) {
        ItemDisplay slot = getWeaponSlot();
        if (slot != null) slot.setItemStack(item);
    }

    /**
     * Removes the spawned group from the world and unregisters the state machine.
     * Must be called when the mob dies or is removed.
     */
    public void despawn() {
        stateMachine.removeGroup(group);
        group.unregister(true, true);
    }

    /**
     * Registers a {@link MachineState} in the machine if {@code animTag} is non-empty.
     * Looping states use {@link DisplayAnimator.AnimationType#LOOP};
     * locked states use {@link DisplayAnimator.AnimationType#LINEAR} so DEU knows when
     * the animation ends and can release the transition lock.
     */
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
