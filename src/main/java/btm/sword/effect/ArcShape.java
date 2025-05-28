//package btm.sword.effect;
//
//import btm.sword.utils.ParticleData;
//import org.bukkit.Location;
//import org.bukkit.World;
//import org.bukkit.entity.LivingEntity;
//import org.bukkit.util.Vector;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//
//public class ArcShape extends EffectShape {
//	double maxAngle;
//	double tilt;
//	double startRange;
//
//	public ArcShape(List<ParticleData> particles, int count, double offset, double maxAngle, double tilt, double startRange) {
//		super(particles, count, offset);
//		this.maxAngle = maxAngle*(Math.PI/180); // total angle in degrees, conversion happens in constructor
//		this.tilt = tilt *(Math.PI/180); // tilt angle clockwise in degrees, converted in constructor
//		this.startRange = startRange;
//	}
//
//	public void display(Location origin, Vector direction, double range, HashSet<LivingEntity> targets) {
//		World world = origin.getWorld();
//
//		List<Vector> vectors = new ArrayList<>();
//		List<Location> points = new ArrayList<>();
//
//		double spacing = maxAngle/(maxAngle*12);
//
//		Vector forward = direction.clone();
//		Vector ref = new Vector(0,1,0);
//		if (Math.abs(forward.dot(ref)) > 0.999) {
//			double yaw = Math.toRadians(origin.getYaw());
//			ref = new Vector(-Math.sin(yaw),0,Math.cos(yaw));
//		}
//		Vector right = ref.clone().crossProduct(forward).normalize();
//		Vector up = forward.clone().crossProduct(right).normalize();
//
//		right.rotateAroundAxis(forward, tilt);
//		up.rotateAroundAxis(forward, tilt);
//
//		Vector u = right.clone().rotateAroundAxis(up, -1*((Math.PI/2)-(maxAngle/2)));
//
//		for (double i = -maxAngle/2; i < maxAngle/2; i+=spacing) {
//			vectors.add(u.clone());
//			points.add(origin.clone().add(u.clone().multiply(range)));
//			u.rotateAroundAxis(up, -1*spacing);
//		}
//
//		LineShape testLine = new LineShape(List.of(particles.getLast()), 3, .01, .25);
//		for (Vector v : vectors) {
//			Location lineOrigin = origin.clone().add(v.clone().multiply(startRange));
//			testLine.display(lineOrigin, v, range-startRange, targets);
//		}
//
//		for (Location l : points) {
//			for (ParticleData p : particles) {
//				if (p.getOptions() != null) {
//					world.spawnParticle(p.getParticle(), l, count, offset, offset, offset, 0, p.getOptions());
//				}
//				else {
//					world.spawnParticle(p.getParticle(), l, count, offset, offset, offset);
//				}
//			}
//		}
//	}
//
//	@Override
//	public void run() {
//
//	}
//}
