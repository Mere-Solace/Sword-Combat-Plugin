package btm.sword.combat.attack;

import btm.sword.Sword;
import btm.sword.effect.Effect;
import btm.sword.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;

public class Attack extends BukkitRunnable {
	private final AttackManager attackManager;
	private final PlayerData playerData;
	private final List<AttackType> attackTypes;
	private final List<Effect> effects;
	private final int durationTicks;
	private int delayTicks = 0;
	
	private Attack next = null;
	
	private Player executor;
	
	private HashSet<LivingEntity> hit = null;
	
	public Attack(AttackManager attackManager, PlayerData playerData, List<AttackType> attackTypes, List<Effect> effects, int durationTicks) {
		this.attackManager = attackManager;
		this.playerData = playerData;
		this.attackTypes = attackTypes;
		this.effects = effects;
		this.durationTicks = durationTicks;
		
		executor = Bukkit.getPlayer(playerData.getUUID());
	}
	
	public Attack(AttackManager attackManager, PlayerData playerData, List<AttackType> attackTypes, List<Effect> effects, int durationTicks, int delayTicks) {
		this(attackManager, playerData, attackTypes, effects, durationTicks);
		this.delayTicks = delayTicks;

		executor = Bukkit.getPlayer(playerData.getUUID());
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
		
		
		executor.sendMessage("Checking for targets!");
		
		
		for (AttackType at : attackTypes) {
			targets = at.getTargets(executor);
			
			
			executor.sendMessage(targets.toString());
			
			
			targets.removeAll(hit);
			
			
			executor.sendMessage("Applying Effects!");
			
			
			at.applyEffects(playerData, targets);
			hit.addAll(targets);
		}
	}
	
	@Override
	public void run() {
		hit();
		
		for (Effect effect : effects) {
			if (effect.usesTargets()) effect.setTargets(hit);
			effect.setLocation(executor.getEyeLocation());
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
