package btm.sword.effect;

import btm.sword.utils.ParticleWrapper;
import btm.sword.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class ArcShape extends EffectShape {
	double outerRadius;
	double innerRadius;
	double xyRotation;
	double xzRotation;
	double yzRotation;
	double maxArcDegrees;
	
	public ArcShape(EffectExecutionType executionType, List<List<ParticleWrapper>> particles, double resolution, int partitions,
	                double outerRadius, double innerRadius, double xyRotation, double xzRotation, double yzRotation, double maxArcDegrees) {
		super(executionType, particles, resolution, partitions);
		this.outerRadius = outerRadius;
		this.innerRadius = innerRadius;
		this.xyRotation = xyRotation;
		this.xzRotation = xzRotation;
		this.yzRotation = yzRotation;
		this.maxArcDegrees = maxArcDegrees;
	}
	
	@Override
	public List<List<Location>> generatePoints(Location origin, Vector direction) {
		List<Vector> basis = Utils.getBasis(origin, direction);
		List<Vector> newBasis = new ArrayList<>(3);
		for (Vector v : basis) {
			newBasis.add(v.clone());
		}
		Utils.rotateBasis(basis, Math.toRadians(xyRotation), Math.toRadians(xzRotation), Math.toRadians(yzRotation));
		
		LineShape rotatedVectors = new LineShape(
				List.of(
						List.of(
								new ParticleWrapper(Particle.SOUL_FIRE_FLAME, 1, 0, 0, 0, 0)
						)
				),
				2.0, 2.0);
		
		LineShape norms = new LineShape(
				List.of(
						List.of(
								new ParticleWrapper(Particle.FLAME, 1, 0, 0, 0, 0)
						)
				),
				2.0, 2.0);
		
		for (Vector b : basis) {
			rotatedVectors.displayAllParticles(rotatedVectors.generatePoints(origin, b));
		}
		for (Vector rb : newBasis) {
			norms.displayAllParticles(norms.generatePoints(origin, rb));
		}
		
		List<List<Location>> points = new ArrayList<>((int) (maxArcDegrees*Math.pow(resolution, 2)));
		
		Vector up = basis.get(1);
		
		double maxRads = Math.toRadians(maxArcDegrees);
		Vector curDir = basis.getFirst().clone().rotateAroundAxis(up, -((Math.PI/2)-(maxRads/2)));
		
		double spacing;
		
		for (int i = 0; i < maxArcDegrees * resolution; i++) {
			for (int j = 0; j < 10; j++) {
				List<Location> section = new ArrayList<>(10);
				for (double k = innerRadius; k <= outerRadius; k += (outerRadius-innerRadius)/resolution) {
					section.add(origin.add(curDir.clone().multiply(k)));
				}
				curDir.rotateAroundAxis(up, maxRads/10);
			}
		}
		
		return points;
	}
}
