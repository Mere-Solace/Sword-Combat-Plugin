package btm.sword.system.entity.display;

import btm.sword.Sword;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.function.Function;

public class ThrownItem {
	ItemDisplay display;
	Location origin;
	Location cur;
	Function<Integer, Vector> positionFunction;
	
	boolean grounded;
	boolean hit;
	
	public void onRelease() {
		// when the item is thrown
		int[] step = {0};
		new BukkitRunnable() {
			@Override
			public void run() {
				evaluate();
				if (grounded || hit) {
					onEnd();
					cancel();
				}
				cur = origin.clone().add(positionFunction.apply(step[0]));
				display.teleport(cur.setDirection(calcDirection()));
				step[0]++;
			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}
	
	public void onEnd() {
	
	}
	
	public void onGrounded() {
		// when the item enters the ground
		
	}
	
	public void onHit() {
		// when the item hits an entity
	}
	
	public void evaluate() {
	
	}
	
	public void groundedCheck() {
	
	}
	
	public void hitCheck() {
	
	}
	
	public Vector calcDirection() {
		return new Vector();
	}
}
