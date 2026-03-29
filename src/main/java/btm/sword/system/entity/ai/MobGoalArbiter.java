package btm.sword.system.entity.ai;

import org.bukkit.Bukkit;

import com.destroystokyo.paper.entity.ai.MobGoals;

public final class MobGoalArbiter {

    private MobGoalArbiter() {}

    public static final MobGoals GOALS = Bukkit.getMobGoals();


}
