package btm.sword.entity.ai;

import org.bukkit.Bukkit;

import com.destroystokyo.paper.entity.ai.MobGoals;

/** Holds the shared {@link com.destroystokyo.paper.entity.ai.MobGoals} instance used to register and remove mob AI goals. */
public final class MobGoalArbiter {

    private MobGoalArbiter() {}

    public static final MobGoals GOALS = Bukkit.getMobGoals();


}
