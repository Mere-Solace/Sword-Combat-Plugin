package btm.sword.listeners;

import btm.sword.Sword;
import btm.sword.combat.CombatManager;
import btm.sword.combat.appliedEffect.BounceEffect;
import btm.sword.combat.appliedEffect.DamageEffect;
import btm.sword.combat.attack.Attack;
import btm.sword.combat.attack.AttackManager;
import btm.sword.combat.attack.attacktypes.LaserAttack;
import btm.sword.effect.EffectExecutionType;
import btm.sword.effect.EffectManager;
import btm.sword.effect.effects.Line;
import btm.sword.util.ParticleSpawner;
import btm.sword.util.ParticleWrapper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class ItemUseListener implements Listener {
	AttackManager attackManager = new AttackManager(new EffectManager(new ParticleSpawner(50)));
	
	Attack gun = new Attack(attackManager,
			List.of(
					new LaserAttack(
							List.of(
									new BounceEffect(3, 2),
									new DamageEffect(7)
							)
					)
			),
			
			List.of(
					new Line(
							attackManager.getEffectManager(),
							EffectExecutionType.INSTANT,
							List.of(
									new ParticleWrapper(
											Particle.FLAME,
											3, 0.05, 0.05, 0.05, 0)
							),
					4.0, 25.0
					)
			),
			2
	);
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!event.getAction().isRightClick()) return;
		
		Player player = event.getPlayer();
		Location l = player.getEyeLocation();
		ItemStack item  = player.getInventory().getItemInMainHand();
		
		Material itemType = item.getType();
		switch(itemType) {
			case IRON_SHOVEL, DIAMOND_HOE -> attackManager.start(gun);
			case NETHERITE_SWORD -> {}
			case DIAMOND_AXE -> {}
			case WOODEN_SWORD ->
					player.getWorld().spawnParticle(
					Particle.CLOUD,
					l.clone().add(l.getDirection().multiply(4)),
					3, l.getDirection().getX(), l.getDirection().getY(), l.getDirection().getZ(), 1);
			case NETHERITE_PICKAXE ->
					player.getWorld().spawnParticle(
					Particle.END_ROD,
					l.clone().add(l.getDirection().multiply(4)),
					0, l.getDirection().getX(), l.getDirection().getY(), l.getDirection().getZ(), 2);
			default -> { }
		}
	}
	
	// Dash IN REAL LIFE :D
	@EventHandler
	public void onPlayerDropEvent(PlayerDropItemEvent event) {
		event.setCancelled(true);
		
		Player player = event.getPlayer();
		
		for (int i = 0; i < 2; i++) {
			new BukkitRunnable() {
				@Override
				public void run() {
					player.setVelocity(player.getEyeLocation().getDirection().multiply(0.1).add(new Vector(0, .25, 0)));
				}
			}.runTaskLater(Sword.getInstance(), i);
		}
	}
}
