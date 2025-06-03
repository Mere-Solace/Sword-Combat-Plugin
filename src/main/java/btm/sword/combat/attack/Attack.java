package btm.sword.combat.attack;

import btm.sword.Sword;
import btm.sword.effect.Effect;
import btm.sword.player.PlayerData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;

public abstract class Attack extends BukkitRunnable {
	private final AttackManager attackManager;
	private final List<AttackType> attackTypes;
	private final List<Effect> effects;
	private final int durationTicks;
	private int delayTicks = 0;
	
	private Attack next = null;
	
	private PlayerData playerData;
	private Player executor;
	
	private HashSet<LivingEntity> hit = null;
	
	public Attack(AttackManager attackManager, List<AttackType> attackTypes, List<Effect> effects, int durationTicks) {
		this.attackManager = attackManager;
		this.attackTypes = attackTypes;
		this.effects = effects;
		this.durationTicks = durationTicks;
		
//		executor = Bukkit.getPlayer(playerData.getUUID());
	}
	
	public Attack(AttackManager attackManager, List<AttackType> attackTypes, List<Effect> effects, int durationTicks, int delayTicks) {
		this(attackManager, attackTypes, effects, durationTicks);
		this.delayTicks = delayTicks;

//		executor = Bukkit.getPlayer(playerData.getUUID());
	}
	
	public void add(Attack nextAttack) {
		next = nextAttack;
	}
	
	public int getDelayTicks() {
		return delayTicks;
	}
	
	public void hit() {
		hit = new HashSet<>(executor.getLocation().getNearbyLivingEntities(25).size());
		HashSet<LivingEntity> targets;
		for (AttackType at : attackTypes) {
			targets = at.getTargets(executor);
			targets.removeAll(hit);
			
			at.applyEffects(playerData, targets);
			hit.addAll(targets);
		}
	}
	
	@Override
	public void run() {
		hit();
		
		for (Effect effect : effects) {
			if (effect.usesTargets()) effect.setTargets(hit);
			effect.setLocation(executor.getLocation());
			attackManager.runAttackEffect(effect);
		}
		
		if (next == null)
			return;
		
		new BukkitRunnable() {
			@Override
			public void run() {
				next.run();
			}
		}.runTaskLater(Sword.getInstance(), next.getDelayTicks()+durationTicks);
	}
}
