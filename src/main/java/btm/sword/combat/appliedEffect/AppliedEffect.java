package btm.sword.combat.appliedEffect;

import btm.sword.player.PlayerData;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;

public abstract class AppliedEffect {
	public abstract void applyEffect(PlayerData executorData, HashSet<LivingEntity> targets);
}
