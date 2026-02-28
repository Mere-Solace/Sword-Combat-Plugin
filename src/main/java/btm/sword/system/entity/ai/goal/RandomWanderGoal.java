package btm.sword.system.entity.ai.goal;

import btm.sword.Sword;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

public class RandomWanderGoal implements Goal<@NotNull Mob>, Listener {
    private static final GoalKey<@NotNull Mob> KEY =
        GoalKey.of(Mob.class, new NamespacedKey(Sword.getInstance(), "random_wander"));


    private final Mob mob;

    public RandomWanderGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
        // Called every tick to check if goal should start
        return !mob.getPathfinder().hasPath();
    }

    @Override
    public boolean shouldStayActive() {
        // Whether it keeps running once started
        return true;
    }

    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, Sword.getInstance());
        moveRandom();
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public void tick() {
        if (!mob.getPathfinder().hasPath()) {
            moveRandom();
        }
    }

    private void moveRandom() {
        Location base = mob.getLocation();
        double dx = ThreadLocalRandom.current().nextDouble(-10, 10);
        double dz = ThreadLocalRandom.current().nextDouble(-10, 10);
        Location target = base.clone().add(dx, 0, dz);
        mob.getPathfinder().moveTo(target);
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}
