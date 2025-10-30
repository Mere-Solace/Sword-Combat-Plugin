package btm.sword.system.entity;

import btm.sword.Sword;
import btm.sword.system.action.utility.GrabAction;
import btm.sword.system.playerdata.CombatProfile;

import btm.sword.util.VectorUtil;
import com.destroystokyo.paper.entity.Pathfinder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

@Getter
@Setter
public class Hostile extends Combatant {
	private final Mob mob;
	private final Pathfinder pathfinder;
	private BukkitTask currentPathfindingTask;
	private Location origin;
	private final List<Consumer<Combatant>> possibleAttacks;
	
	ItemStack itemInLeftHand = new ItemStack(Material.IRON_AXE);
	ItemStack itemInRightHand = new ItemStack(Material.ENCHANTED_BOOK);
	
	public Hostile(LivingEntity associatedEntity, CombatProfile combatProfile) {
		super(associatedEntity, combatProfile);
		mob = (Mob) self;
		pathfinder = mob.getPathfinder();
		pathfinder.setCanFloat(false);
		pathfinder.setCanOpenDoors(true);
		
		origin = mob.getLocation();
		
		possibleAttacks = new ArrayList<>();
		
		EntityEquipment equipment = associatedEntity.getEquipment();
		if (equipment != null) {
			equipment.setItemInMainHand(itemInLeftHand);
			equipment.setItemInOffHand(itemInRightHand);
			
//			equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
		}
	}
	
	public Mob mob() {
		return mob;
	}
	
	@Override
	public void onTick() {
		super.onTick();
		
		
	}
	
	@Override
	public void onSpawn() {
		super.onSpawn();
		
	}
	
	@Override
	public void onDeath() {
	
	}
	
	public void patrol(Location origin) {
		currentPathfindingTask = new BukkitRunnable() {
			@Override
			public void run() {
				Random random = new Random();
				random.nextFloat();
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 20L);
		pathfinder.moveTo(origin);
	}
	
	public void halt() {
		if (currentPathfindingTask != null && !currentPathfindingTask.isCancelled() && currentPathfindingTask.getTaskId() != -1)
			currentPathfindingTask.cancel();
		pathfinder.moveTo(mob.getLocation());
		mob.setTarget(null);
		mob.setAware(false);
	}
	
	public void surround(List<SwordEntity> targets, List<Combatant> allies) {
		halt();
		
	}
	
	public void approach(SwordEntity target) {
		halt();
		
	}
	
	public void charge(SwordEntity target) {
		halt();
		
	}
	
	public void retreat(SwordEntity target) {
		halt();
		
	}
	
	public void flee(List<SwordEntity> targets) {
		halt();
		
	}
	
	public void randomAttack() {
		Random random = new Random();
		possibleAttacks.get(random.nextInt(possibleAttacks.size())).accept(this);
	}
	
	public void grab() {
		GrabAction.grab(this);
	}
	
	public void jump() {
		new BukkitRunnable() {
			int i = 0;
			@Override
			public void run() {
				if (i >= 1) cancel();
				mob.setVelocity(VectorUtil.UP.clone().multiply(1));
				i++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}
}
