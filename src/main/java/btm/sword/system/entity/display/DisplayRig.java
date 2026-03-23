package btm.sword.system.entity.display;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.mob.AnimationSlots;
import btm.sword.utility.display.DisplayUtil;
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
 *
 * <p>Wraps a {@link SpawnedDisplayEntityGroup} and a {@link DisplayStateMachine}.
 * The machine automatically transitions between IDLE, WALK, and FALLING states based
 * on the mob's movement. The MELEE state is triggered manually by the AI FSM
 * (see {@link btm.sword.system.entity.ai.state.AttackState}). The DEATH state, when
 * present, is triggered by {@link #triggerDeath()} and plays once before the mob is killed.</p>
 *
 * <p>Weapon slot display: if the DEU group contains a {@link Material#LIGHTNING_ROD}
 * {@link ItemDisplay} whose scale is below {@link #WEAPON_SLOT_SCALE_THRESHOLD} on all axes,
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

    /**
     * Scale threshold below which all three axes of an {@link ItemDisplay} must fall
     * for it to be treated as a weapon-slot anchor.
     */
    public static final float WEAPON_SLOT_SCALE_THRESHOLD = 0.05f;

    private final Mob mob;
    private final SpawnedDisplayEntityGroup group;
    private final DisplayStateMachine stateMachine;
    /** Non-empty when a die animation was registered; used by the animation-complete listener. */
    private final String dieAnimTag;

    /** The tiny LIGHTNING_ROD anchor within the DEU group that marks the main-hand weapon slot. */
    private final @Nullable ItemDisplay weaponAnchor;

    /** The live item display entity that follows the mob's hand and shows the held item. */
    private @Nullable ItemDisplay weaponDisplay;
    /** Task handle for the mob-follow loop; non-null while a weapon is being shown. */
    private @Nullable TimeArbiter.TaskHandle weaponFollowTask;

    private DisplayRig(Mob mob, SpawnedDisplayEntityGroup group, DisplayStateMachine stateMachine,
            String dieAnimTag, @Nullable ItemDisplay weaponAnchor) {
        this.mob = mob;
        this.group = group;
        this.stateMachine = stateMachine;
        this.dieAnimTag = dieAnimTag;
        this.weaponAnchor = weaponAnchor;
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
        if (group == null) return null;

        group.rideEntity(mob);
        group.setRideOffset(new Vector(0, -mob.getHeight() + Config.Hostile.DISPLAY_RIDE_OFFSET_Y, 0));

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
            if (item == null || item.getType() != Material.LIGHTNING_ROD) continue;
            org.joml.Vector3f scale = id.getTransformation().getScale();
            if (scale.x < WEAPON_SLOT_SCALE_THRESHOLD
                    && scale.y < WEAPON_SLOT_SCALE_THRESHOLD
                    && scale.z < WEAPON_SLOT_SCALE_THRESHOLD) {
                weaponAnchor = id;
                break;
            }
        }

        String dieTag = slots.die().tag() != null ? slots.die().tag() : "";
        return new DisplayRig(mob, group, stateMachine, dieTag, weaponAnchor);
    }

    // -----------------------------------------------------------------------
    // Weapon slot

    /**
     * Shows the given item at the mob's main-hand weapon slot by spawning an
     * {@link ItemDisplay} that follows the mob using the transform from
     * {@link WeaponDisplayRegistry}. Clears any previously displayed item.
     *
     * <p>Passing {@code null} or an air stack hides the weapon display.</p>
     *
     * @param item the item to display, or {@code null} to clear
     */
    public void setWeaponSlotItem(@Nullable ItemStack item) {
        clearWeaponDisplay();

        if (weaponAnchor == null) return;
        if (item == null || item.getType().isAir()) return;

        WeaponDisplayTransform t = WeaponDisplayRegistry.get(item.getType());

        weaponDisplay = (ItemDisplay) mob.getWorld().spawnEntity(
            weaponAnchor.getLocation(), EntityType.ITEM_DISPLAY
        );
        weaponDisplay.setItemStack(item);
        applyTransform(weaponDisplay, t);

        weaponFollowTask = DisplayUtil.itemDisplayFollowMob(
            mob, weaponDisplay,
            t.offsetRight(), t.offsetUp(), t.offsetForward(),
            Config.Hostile.DISPLAY_TELEPORT_DURATION, 2
        );
    }

    // -----------------------------------------------------------------------
    // State machine

    /**
     * Manually overrides the current animation state.
     *
     * @param type the state to transition to
     */
    public void setState(MachineState.StateType type) {
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
     * Removes the spawned group from the world and unregisters the state machine.
     * Must be called when the mob dies or is removed.
     */
    public void despawn() {
        clearWeaponDisplay();
        stateMachine.removeGroup(group);
        group.unregister(true, true);
    }

    // -----------------------------------------------------------------------
    // Private helpers

    /** Cancels the follow task and removes the weapon display entity. */
    private void clearWeaponDisplay() {
        if (weaponFollowTask != null) {
            weaponFollowTask.cancel();
            weaponFollowTask = null;
        }
        if (weaponDisplay != null && weaponDisplay.isValid()) {
            weaponDisplay.remove();
            weaponDisplay = null;
        }
    }

    /**
     * Applies a {@link WeaponDisplayTransform}'s rotation and scale to an {@link ItemDisplay}
     * via its {@link Transformation}. Offset is handled separately by the follow task.
     */
    private static void applyTransform(ItemDisplay display, WeaponDisplayTransform t) {
        Quaternionf rot = new Quaternionf().rotateXYZ(
            (float) Math.toRadians(t.rotX()),
            (float) Math.toRadians(t.rotY()),
            (float) Math.toRadians(t.rotZ())
        );
        display.setTransformation(new Transformation(
            new Vector3f(0, 0, 0),
            rot,
            new Vector3f(t.scale(), t.scale(), t.scale()),
            new Quaternionf()
        ));
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
