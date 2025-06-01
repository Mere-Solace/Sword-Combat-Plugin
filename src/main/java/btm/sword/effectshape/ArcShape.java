package btm.sword.effectshape;

import btm.sword.utils.ParticleWrapper;
import btm.sword.utils.VectorUtils;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;

public class ArcShape extends EffectShape {
	double outerRadius;
	double innerRadius;
	double yawOffset;
	double pitchOffset;
	double maxAngle;
	boolean clockwise = false;
	
	public ArcShape(List<List<ParticleWrapper>> particles, double resolution, int partitions, int delayTicks,
	                double outerRadius, double innerRadius, double yawOffset, double pitchOffset, double maxAngle) {
		super(particles, resolution, partitions, delayTicks);
		this.outerRadius = outerRadius;
		this.innerRadius = innerRadius;
		this.yawOffset = Math.toRadians(yawOffset);
		this.pitchOffset = Math.toRadians(pitchOffset);
		this.maxAngle = Math.toRadians(maxAngle);
		
		points = new LinkedList<>();
	}
	
	@Override
	public void generatePoints(Location origin, Vector direction) {
		List<Vector> basis = VectorUtils.getBasis(origin, direction);
		VectorUtils.rotateBasis(basis, yawOffset, pitchOffset);
		
		Vector up = basis.get(1);
		
		double startAngle = -1*(maxAngle/2);
		double angleInc = 1/(resolution);
		if (clockwise) {
			startAngle*=-1;
			angleInc*=-1;
		}
		
		Vector cur = basis.getLast().clone().rotateAroundAxis(up, startAngle).normalize();
		
		double maxAnglePerPart = maxAngle/partitions;
		
		for (int i = 0; i < partitions; i++) {
			List<Location> section = new LinkedList<>();
			for (double t = 0; t <= maxAnglePerPart; t += Math.abs(angleInc)) {
				if (innerRadius == outerRadius) {
					section.add(origin.clone().add(cur.clone().multiply(outerRadius)));
				} else {
					for (double x = innerRadius; x < outerRadius; x += (outerRadius - innerRadius) / resolution) {
						section.add(origin.clone().add(cur.clone().multiply(x)));
					}
				}
				
				cur.rotateAroundAxis(up, angleInc);
			}
			points.add(section);
		}
	}
}
