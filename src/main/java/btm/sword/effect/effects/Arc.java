package btm.sword.effect.effects;

import btm.sword.effect.Effect;
import btm.sword.effect.EffectExecutionType;
import btm.sword.effect.EffectManager;
import btm.sword.util.ParticleWrapper;
import btm.sword.util.VectorUtils;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.List;

public class Arc extends Effect {
	double outerRadius = 5;
	double innerRadius = 3;
	double yawOffset = 0;
	double rollOffset = 90;
	double maxAngle = 120;
	boolean clockwise = false;
	
	public Arc(EffectManager effectManager) {
		super(effectManager, List.of(new ParticleWrapper(Particle.FLAME, 1, 0, 0,0, 0)));
	}
	
	public Arc(EffectManager effectManager, double outerRadius, double innerRadius, double yawOffset, double rollOffset, double maxAngle) {
		this(effectManager);
		this.outerRadius = outerRadius;
		this.innerRadius = innerRadius;
		this.yawOffset = Math.toRadians(yawOffset);
		this.rollOffset = Math.toRadians(rollOffset);
		this.maxAngle = Math.toRadians(maxAngle);
	}
	
	public Arc(EffectManager effectManager, EffectExecutionType type, List<ParticleWrapper> particles, double resolution, int partitions, int delayTicks, int period,
	           double outerRadius, double innerRadius, double yawOffset, double rollOffset, double maxAngle) {
		super(effectManager, type, particles, resolution, partitions, delayTicks, period);
		this.outerRadius = outerRadius;
		this.innerRadius = innerRadius;
		this.yawOffset = Math.toRadians(yawOffset);
		this.rollOffset = Math.toRadians(rollOffset);
		this.maxAngle = Math.toRadians(maxAngle);
	}
	
	@Override
	public void onRun() {
		
		if (location == null)
			return;
		if (direction == null)
			setDirection(location.getDirection());
		
		List<Vector> basis = VectorUtils.getBasis(location, direction);
		VectorUtils.rotateBasis(basis, yawOffset, rollOffset);
		
		Vector up = basis.get(1);
		
		double startAngle = -1*(maxAngle/2);
		double angleInc = 1/(resolution);
		if (clockwise) {
			startAngle*=-1;
			angleInc*=-1;
		}
		
		Vector cur = basis.getLast().clone().rotateAroundAxis(up, startAngle).normalize();
		
		double maxAnglePerPart = maxAngle/partitions;
		
		for (double t = 0; t <= maxAnglePerPart; t += Math.abs(angleInc)) {
			if (innerRadius == outerRadius) {
				points.add(location.add(cur.multiply(outerRadius)));
				cur.normalize();
			} else {
				for (double x = innerRadius; x < outerRadius; x += (outerRadius - innerRadius) / resolution) {
					points.add(location.add(cur.multiply(x)));
					location.subtract(cur);
					cur.normalize();
				}
			}
			
			cur.rotateAroundAxis(up, angleInc);
		}
	}
}
