package btm.sword.combat.attack.attacktypes;

import btm.sword.Sword;
import btm.sword.combat.appliedEffect.AppliedEffect;
import btm.sword.combat.attack.AttackType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public class LaserAttack extends AttackType {
	double maxRange = 25;
	double thickness = 0.5;
	
	public LaserAttack(List<AppliedEffect> appliedEffects) {
		super(appliedEffects);
	}
	
	public LaserAttack(List<AppliedEffect> appliedEffects, double maxRange, double thickness) {
		super(appliedEffects);
		this.maxRange = maxRange;
		this.thickness = thickness;
	}
	
	@Override
	public HashSet<LivingEntity> getTargets(Player executor) {
		Location o = executor.getEyeLocation().add(0,-1,0);
		Vector e = o.getDirection();
		
		HashSet<LivingEntity> hit = new HashSet<>();
		
		for (double i = 0; i < maxRange; i += thickness) {
			hit.addAll(o.clone().add(e.clone().multiply(i)).getNearbyLivingEntities(thickness));
		}
		hit.removeIf(Entity::isDead);
		hit.remove(executor);
		Sword.getInstance().getLogger().info(hit.toString());
		return hit;
	}
}
