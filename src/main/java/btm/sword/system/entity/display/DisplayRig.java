package btm.sword.system.entity.display;

import java.util.List;

import org.bukkit.entity.Display;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import btm.sword.Sword;
import btm.sword.config.Config;
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
 * (see {@link btm.sword.system.entity.ai.state.AttackState}).
 * </p>
 *
 * <p>To spawn a rig for a mob, call {@link #spawn(Mob, String)} with the mob and
 * the DEU group tag. Call {@link #despawn()} when the mob dies to clean up entities.</p>
 */
public class DisplayRig {

    private final SpawnedDisplayEntityGroup group;
    private final DisplayStateMachine stateMachine;

    private DisplayRig(SpawnedDisplayEntityGroup group, DisplayStateMachine stateMachine) {
        this.group = group;
        this.stateMachine = stateMachine;
    }

    /**
     * Spawns a display rig for the given mob using the named DEU group tag.
     * Returns {@code null} if the group cannot be found in DEU's local storage.
     *
     * @param mob      the mob to attach the rig to
     * @param groupTag the DEU group tag to spawn
     * @return the new rig, or {@code null} on failure
     */
    public static @Nullable DisplayRig spawn(Mob mob, String groupTag) {
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
        addState(stateMachine, MachineState.StateType.IDLE, Config.Hostile.DISPLAY_ANIM_IDLE, false);
        addState(stateMachine, MachineState.StateType.WALK, Config.Hostile.DISPLAY_ANIM_WALK, false);
        addState(stateMachine, MachineState.StateType.FALLING, Config.Hostile.DISPLAY_ANIM_FALL, false);
        addState(stateMachine, MachineState.StateType.MELEE, Config.Hostile.DISPLAY_ANIM_MELEE, true);
        stateMachine.addGroup(group);

        return new DisplayRig(group, stateMachine);
    }

    /**
     * Manually overrides the current animation state.
     * States registered with {@code transitionLock = true} (e.g. MELEE) hold until
     * the animation completes, then the machine resumes automatic transitions.
     *
     * @param type the state to transition to
     */
    public void setState(MachineState.StateType type) {
        stateMachine.setState(type, group);
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
