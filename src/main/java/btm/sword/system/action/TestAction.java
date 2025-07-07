package btm.sword.system.action;

import btm.sword.Sword;
import btm.sword.system.SwordScheduler;
import btm.sword.system.entity.Combatant;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.concurrent.TimeUnit;

public class TestAction extends SwordAction {
	public static void testAttack(Combatant executor) {
		LivingEntity ex = executor.entity();
		Location o = ex.getEyeLocation();
		int period = 5;
		
		float radius = 2.5f;
		float xScale = 0.75f;
		float yScale = 0.25f;
		float zScale = 2.0f;
		float startAngle = (float) -Math.PI/2;
		float endAngle = (float) Math.PI/2;
		float increment = (float) Math.PI/45;
		int t = 0;
		for (float i = startAngle; i < endAngle; i+=increment) {
			float x = i;
			int T = t;
			SwordScheduler.runLater(new BukkitRunnable() {
				final Vector3f base = new Vector3f(radius*(float) Math.sin(x), 0, radius*(float) Math.cos(x));
				@Override
				public void run() {
					BlockDisplay bd = (BlockDisplay) ex.getWorld().spawnEntity(o, EntityType.BLOCK_DISPLAY);
					bd.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
					bd.setTransformation(
							new Transformation(
									new Vector3f(base).sub(xScale/2, yScale/2, zScale/2),
									new Quaternionf().rotateY(x),
									new Vector3f(xScale, yScale, zScale),
									new Quaternionf()
							)
					);
					bd.setGlowing(true);
					bd.setGlowColorOverride(Color.BLUE);
					
					SwordScheduler.runLater(new BukkitRunnable() {
						@Override
						public void run() {
							new BukkitRunnable() {
								final float check = 0.07f*xScale;
								final float rate = 0.7f;
								@Override
								public void run() {
									Transformation cur = bd.getTransformation();
									Vector3f s = cur.getScale().mul(rate);
									bd.setTransformation(
											new Transformation(
													new Vector3f(base).sub(new Vector3f(s).mul(0.5f)),
													cur.getLeftRotation(),
													s,
													cur.getRightRotation()
											)
									);
									if (s.x() < check) {
										bd.remove();
										cancel();
									}
								}
							}.runTaskTimer(Sword.getInstance(), 0L, 1L);
						}
					},100 + period*T, TimeUnit.MILLISECONDS);
				}
			}, period*t, TimeUnit.MILLISECONDS);
			t++;
		}
	}
}
