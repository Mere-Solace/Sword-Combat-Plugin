package btm.sword.effect;

import btm.sword.Sword;
import btm.sword.util.ParticleWrapper;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/* The Effect class contains all information about a visual effect.
*  It implements java Runnable and therefore can be passed to Bukkit's run task methods.
*
*/

public abstract class Effect implements Runnable {
	private final EffectManager effectManager;
	protected EffectExecutionType type = EffectExecutionType.INSTANT;
	protected List<ParticleWrapper> particles;
	protected double resolution = 3;
	protected int partitions = 1;
	protected int delayTicks = 0;
	protected int period = 1;
	protected int iterations = 1;
	protected boolean usesTargets = false;
	protected Vector originOffset = null;
	protected HashSet<LivingEntity> targets = null;
	
	protected Location location = null;
	protected Vector direction = null;
	
	protected int step = 0;
	protected boolean done = false;
	protected boolean playing = false;
	public List<Location> points = new LinkedList<>();
	public int numPoints = 0;
	public Runnable callback = null;
	
	public Effect(EffectManager effectManager) {
		this.effectManager = effectManager;
		particles = new ArrayList<>();
	}
	
	public Effect(EffectManager effectManager, List<ParticleWrapper> particles) {
		this(effectManager);
		if (particles == null)
			this.particles.add(new ParticleWrapper(Particle.FLAME, 1, 0, 0, 0, 0));
		else
			this.particles.addAll(particles);
	}
	
	public Effect(EffectManager effectManager, List<ParticleWrapper> particles, boolean usesTargets) {
		this(effectManager, particles);
		this.usesTargets = usesTargets;
	}
	
	public Effect(EffectManager effectManager, List<ParticleWrapper> particles, Vector originOffset) {
		this(effectManager, particles);
		this.originOffset = originOffset;
	}
	
	public Effect(EffectManager effectManager, EffectExecutionType type) {
		this(effectManager);
		this.type = type;
	}
	
	public Effect(EffectManager effectManager, EffectExecutionType type, List<ParticleWrapper> particles) {
		this(effectManager, type);
		this.particles = particles;
	}
	
	public Effect(EffectManager effectManager, EffectExecutionType type, List<ParticleWrapper> particles, double resolution) {
		this(effectManager, type, particles);
		this.resolution = resolution;
	}
	
	public Effect(EffectManager effectManager, EffectExecutionType type, List<ParticleWrapper> particles, double resolution, int partitions) {
		this(effectManager, type, particles, resolution);
		this.partitions = partitions;
	}
	
	public Effect(EffectManager effectManager, EffectExecutionType type, List<ParticleWrapper> particles, double resolution, int partitions, int delayTicks) {
		this(effectManager, type, particles, resolution, partitions);
		this.delayTicks = delayTicks;
	}
	
	public Effect(EffectManager effectManager, EffectExecutionType type, List<ParticleWrapper> particles, double resolution, int partitions, int delayTicks, int period) {
		this(effectManager, type, particles, resolution, partitions, delayTicks);
		this.period = period;
	}
	
	public abstract void onRun();
	
	@Override
	public void run() {
		if (done) {
			effectManager.removeEffect(this);
		}
		
		playing = true;
		
		onRun();
		
		display();
		
		if (type == EffectExecutionType.REPEATING) {
			if (iterations == -1) return;
			iterations--;
			if (iterations < 1) done();
		}
		else
			done();
	}
	
	public void display() {
		if (type == EffectExecutionType.INSTANT)
			displayAllPoints();
		
		else if (type == EffectExecutionType.SEQUENTIAL || type == EffectExecutionType.REPEATING) {
			for (int i = 0; i < partitions; i++) {
				new BukkitRunnable() {
					@Override
					public void run() {
						displaySection();
					}
				}.runTaskLater(Sword.getInstance(), delayTicks+((long) period*i));
			}
		}
	}
	
	public void displaySection() {
		for (int i = step; i < numPoints / partitions; i++) {
			effectManager.display(particles, points.get(i));
			step++;
		}
	}
	
	public void displayAllPoints() {
		for (Location l : points) {
			effectManager.display(particles, l);
		}
	}
	
	public int getDelayTicks() {
		return delayTicks;
	}
	
	public int getPeriod() {
		return period;
	}
	
	public List<Location> getPoints() {
		return points;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
	
	public Vector getDirection() {
		return direction;
	}
	
	public void setDirection(Vector direction) {
		this.direction = direction;
	}
	
	public boolean usesTargets() {
		return usesTargets;
	}
	
	public void setOriginOffset(Vector originOffset) {
		this.originOffset = originOffset;
	}
	
	public void setTargets(HashSet<LivingEntity> targets) {
		this.targets = targets;
	}
	
	public void reset() {
		step = 0;
	}
	
	private void done() {
		playing = false;
		done = true;
		effectManager.done(this);
		onDone();
	}
	
	public void onDone() { }
	
	public final void setInfinite() {
		type = EffectExecutionType.REPEATING;
		iterations = -1;
	}
}
