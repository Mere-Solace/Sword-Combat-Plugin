package btm.sword.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class VectorUtil {
	public static ArrayList<Vector> getBasis(Location origin, Vector dir) {
		Vector ref = new Vector(0,1,0);
		
		if (Math.abs(dir.dot(ref)) > 0.999) {
			double yaw = Math.toRadians(origin.getYaw());
			ref = new Vector(Math.sin(yaw), 0, Math.cos(yaw));
			Cache.testObsidianTearParticle.display(origin.clone().add(ref));
		}
		
		Vector right = dir.getCrossProduct(ref).normalize();
		Vector up = right.getCrossProduct(dir).normalize();
		
		ArrayList<Vector> basis = new ArrayList<>(3);
		
		basis.add(right);
		basis.add(up);
		basis.add(dir);
		
		return basis;
	}
	
	public static ArrayList<Vector> getBasisWithoutPitch(Location origin) {
		Vector up = new Vector(0,1,0);
		double yaw = Math.toRadians(origin.getYaw());
		Vector dir = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
		
		Vector right = dir.getCrossProduct(up).normalize();
		
		ArrayList<Vector> basis = new ArrayList<>(3);
		
		basis.add(right);
		basis.add(up);
		basis.add(dir);
		
		return basis;
	}
	
	public static void rotateBasis(List<Vector> basis, double roll, double yaw) {
		basis.get(1).rotateAroundAxis(basis.getLast(), -roll);
		basis.getFirst().rotateAroundAxis(basis.getLast(), -roll);
		
		basis.getLast().rotateAroundAxis(basis.get(1), yaw);
		basis.getFirst().rotateAroundAxis(basis.get(1), yaw);
	}
	
	public static Vector transformWithNewBasis(ArrayList<Vector> basis, Vector v) {
		Vector right = basis.getFirst();
		Vector up = basis.get(1);
		Vector forward = basis.getLast();
		
		return right.clone().multiply(v.getX())
				.add(up.clone().multiply(v.getY()))
				.add(forward.clone().multiply(v.getZ()));
	}
	
	public static Vector getProjOntoPlan(Vector v, Vector norm) {
		return v.clone().subtract(norm.clone().multiply(v.dot(norm)/norm.lengthSquared()));
	}
}
