package btm.sword.combat.attack.attacktypes;

import btm.sword.combat.appliedEffect.AppliedEffect;
import btm.sword.combat.attack.AttackType;
import btm.sword.util.VectorUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;

public class SlashAttack extends AttackType {
	float maxRange = 6;
	float minRange = 4;
	float thickness = 0.5f;
	double maxAngle = 120;
	double roll = 0;
	
	public SlashAttack(List<AppliedEffect> appliedEffects) {
		super(appliedEffects);
		maxAngle = Math.toRadians(maxAngle);
		roll = Math.toRadians(roll);
	}
	
	@Override
	public HashSet<LivingEntity> getTargets(Player executor) {
		Location o = executor.getEyeLocation();
		Vector e = o.getDirection();
		
		HashSet<LivingEntity> hit = new HashSet<>(o.getNearbyLivingEntities(maxRange));
		
		List<Vector> basis = VectorUtils.getBasis(o, e);
		VectorUtils.rotateBasis(basis, roll, 0);
		
		for (LivingEntity target : hit) {
			Vector toTarget = target.getEyeLocation().subtract(o).toVector();
			
			double forwardDist = toTarget.dot(basis.getLast());
			double sideOffset = Math.abs(toTarget.dot(basis.getFirst()));
			double upOffset = Math.abs(toTarget.dot(basis.get(1)));
			
			if (target.isDead() ||
					forwardDist < minRange ||
					sideOffset > maxRange * Math.abs(Math.cos(Math.min(0, (Math.PI/2)-(maxAngle/2)))) ||
					upOffset > thickness)
				hit.remove(target);
		}
		hit.remove(executor);
		return hit;
	}
}
