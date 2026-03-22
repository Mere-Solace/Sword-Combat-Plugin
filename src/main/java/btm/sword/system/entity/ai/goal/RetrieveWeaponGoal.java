package btm.sword.system.entity.ai.goal;

import java.util.EnumSet;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import btm.sword.Sword;
import btm.sword.system.action.throwing.types.ThrownItem;
import btm.sword.system.entity.impl.Hostile;

/**
 * Pathfinds toward the {@link Hostile}'s lodged thrown item at 120% movement speed.
 *
 * <p>Registered by {@code RetrieveWeaponState.onEnter} and removed by {@code RetrieveWeaponState.onExit}.
 * {@link #shouldStayActive()} deactivates when the lodged item is cleared or its display becomes invalid.
 */
public class RetrieveWeaponGoal implements Goal<@NotNull Mob> {

    /** Unique key for this goal; used for lookup and removal. */
    public static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "retrieve_weapon"));

    private static final double RETRIEVE_SPEED = 1.2;

    private final Mob mob;
    private final Hostile hostile;

    /**
     * Constructs a {@code RetrieveWeaponGoal} for the given mob and its Sword wrapper.
     *
     * @param mob     the Bukkit {@link Mob} to pathfind
     * @param hostile the {@link Hostile} wrapper owning the lodged item reference
     */
    public RetrieveWeaponGoal(Mob mob, Hostile hostile) {
        this.mob = mob;
        this.hostile = hostile;
    }

    /** Activates immediately on registration. */
    @Override
    public boolean shouldActivate() {
        return true;
    }

    /**
     * Returns {@code false} when the lodged item is cleared or its display has been removed,
     * acting as a secondary exit signal alongside the FSM transition.
     *
     * @return {@code true} while a valid lodged item exists
     */
    @Override
    public boolean shouldStayActive() {
        ThrownItem item = hostile.getLodgedThrowItem();
        return item != null && item.getDisplay() != null && item.getDisplay().isValid();
    }

    /** Starts pathfinding toward the lodged item on goal activation. */
    @Override
    public void start() {
        pathfindToItem();
    }

    /** Stops pathfinding when the goal deactivates. */
    @Override
    public void stop() {
        mob.getPathfinder().stopPathfinding();
    }

    /** Re-issues the pathfind command each tick to track the item's current location. */
    @Override
    public void tick() {
        pathfindToItem();
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }

    private void pathfindToItem() {
        ThrownItem item = hostile.getLodgedThrowItem();
        if (item == null || item.getDisplay() == null || !item.getDisplay().isValid()) return;
        mob.getPathfinder().moveTo(item.getDisplay().getLocation(), RETRIEVE_SPEED);
    }
}
