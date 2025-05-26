package btm.sword.visualeffect;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ArcVisual extends VisualEffect {
	double maxAngle;
	double tiltClockWise;
	
	public ArcVisual(List<Particle> particles, int count, double offset, double maxAngle, double tiltClockWise) {
		super(particles, count, offset);
		this.maxAngle = maxAngle*(Math.PI/180); // total angle in degrees, conversion happens in constructor
		this.tiltClockWise = tiltClockWise*(Math.PI/180); // tilt angle clockwise in degrees, converted in constructor
	}
	
	@Override
	public void drawEffect(Location origin, Vector direction, double range, HashSet<LivingEntity> targets) {
		List<Vector> vectors = new ArrayList<>();
		
		double spacing = maxAngle/(maxAngle*10);
		
		Vector forward = direction.clone();
		Vector ref = new Vector(0,1,0);
		if (Math.abs(forward.dot(ref)) > 0.999) {
			double yaw = Math.toRadians(origin.getYaw());
			ref = new Vector(-Math.sin(yaw),0,Math.cos(yaw));
		}
		Vector right = ref.clone().crossProduct(forward).normalize();
		Vector up = forward.clone().crossProduct(right).normalize();
		
		right.rotateAroundAxis(forward, tiltClockWise);
		up.rotateAroundAxis(forward, tiltClockWise);
		
		Vector cur = right.clone().rotateAroundAxis(up, -1*((Math.PI/2)-(maxAngle/2)));
		
		for (double i = -maxAngle/2; i < maxAngle/2; i+=spacing) {
			vectors.add(cur.clone().rotateAroundAxis(up, -1*spacing));
			cur.rotateAroundAxis(up, -1*spacing);
		}
		LineVisual testLine = new LineVisual(particles, 3, .01, .25);
		for (Vector v : vectors) {
			testLine.drawEffect(origin, v, range, targets);
		}
	}
}
