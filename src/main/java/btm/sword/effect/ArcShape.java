package btm.sword.effect;

import btm.sword.utils.ParticleWrapper;
import btm.sword.utils.Utils;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class ArcShape extends EffectShape {
	double outerRadius;
	double innerRadius;
	double xyTilt;
	double xzTilt;
	double yzTilt;
	double maxArcDegrees;
	
	public ArcShape(EffectExecutionType executionType, List<List<ParticleWrapper>> particles, double resolution, int partitions,
	                double outerRadius, double innerRadius, double xyTilt, double xzTilt, double yzTilt, double maxArcDegrees) {
		super(executionType, particles, resolution, partitions);
		this.outerRadius = outerRadius;
		this.innerRadius = innerRadius;
		this.xyTilt = xyTilt;
		this.xzTilt = xzTilt;
		this.yzTilt = yzTilt;
		this.maxArcDegrees = maxArcDegrees;
	}
	
	@Override
	public List<List<Location>> generatePoints(Location origin, Vector direction) {
		List<Vector> basis = Utils.getBasis(origin, direction);
		Utils.rotateBasis(basis, Math.toRadians(xyTilt), Math.toRadians(xzTilt), Math.toRadians(yzTilt));
		
		List<List<Location>> points = new ArrayList<>((int) (maxArcDegrees*resolution));
		
		int pointsPerPartition = (int) ((resolution*(maxArcDegrees)*(outerRadius-innerRadius))/partitions);
		
		Vector curDir = basis.getFirst().clone().rotateAroundAxis(basis.get(1), -((Math.PI/2)-(maxArcDegrees/2)));
		return points;
	}
}
