package btm.sword.combat.attack;

import btm.sword.effectshape.EffectShape;
import btm.sword.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public abstract class Attack extends BukkitRunnable {
	private final PlayerData playerData;
	private final Stack<AttackType> hitboxes;
	private List<EffectShape> effectShapes;
	private int delayTicks;
	
	private Attack next = null;
	
	private HashSet<LivingEntity> hit;
	
	public Attack(PlayerData playerData, Stack<AttackType> hitboxes, List<EffectShape> effectShapes) {
		this.playerData = playerData;
		this.hitboxes = hitboxes;
		this.effectShapes = effectShapes;
	}
	
	public void add(Attack nextAttack) {
		next = nextAttack;
	}
	
	public void getHit() {
		Player executor = Bukkit.getPlayer(playerData.getUUID());
		
		while (!hitboxes.empty()) {
			AttackType type = hitboxes.pop();
		}
	}
	
	@Override
	public void run() {
		
		
		if (next == null)
			return;
		next.run();
	}
}
