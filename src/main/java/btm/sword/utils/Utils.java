package btm.sword.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class Utils {
	public static ArrayList<Vector> getBasis(Location origin, Vector dir) {
		Vector ref = new Vector(0,1,0);
		
		if (Math.abs(dir.dot(ref)) > 0.999) {
			double yaw = Math.toRadians(origin.getYaw());
			ref = new Vector(-Math.sin(yaw),0,Math.cos(yaw));
		}
		
		Vector right = ref.getCrossProduct(dir).normalize();
		Vector up = dir.getCrossProduct(right).normalize();
		
		ArrayList<Vector> basis = new ArrayList<>(3);
		
		basis.add(right);
		basis.add(up);
		basis.add(dir);
		
		return basis;
	}
	
	public static Vector transformWithNewBasis(ArrayList<Vector> basis, Vector v) {
		Vector right = basis.getFirst();
		Vector up = basis.get(1);
		Vector forward = basis.getLast();
		
		return right.clone().multiply(v.getX())
				.add(up.clone().multiply(v.getY()))
				.add(forward.clone().multiply(v.getZ()));
	}
}
