package btm.sword.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

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
	
	public static void rotateBasis(List<Vector> basis, double radiansAroundRight, double radiansAroundUp, double radiansAroundForward) {
		double cosYZ = Math.cos(radiansAroundRight);
		double sinYZ = Math.sin(radiansAroundRight);
		
		double cosXZ = Math.cos(radiansAroundUp);
		double sinXZ = Math.sin(radiansAroundUp);
		
		double cosXY = Math.cos(radiansAroundForward);
		double sinXY = Math.sin(radiansAroundForward);
		
		double[][] R = new double[3][3];
		R[0][0] = cosXZ * cosXY;
		R[0][1] = cosXY * sinYZ * sinXZ - cosYZ * sinXY;
		R[0][2] = cosYZ * cosXY * sinXZ + sinYZ * sinXY;
		
		R[1][0] = cosXZ * sinXY;
		R[1][1] = cosYZ * cosXY + sinYZ * sinXZ * sinXY;
		R[1][2] = -cosXY * sinYZ + cosYZ * sinXZ * sinXY;
		
		R[2][0] = -sinXZ;
		R[2][1] = cosXZ * sinYZ;
		R[2][2] = cosYZ * cosXZ;

		for (int i = 0; i < basis.size(); i++) {
			Vector v = basis.get(i);
			double x = R[0][0] * v.getX() + R[0][1] * v.getY() + R[0][2] * v.getZ();
			double y = R[1][0] * v.getX() + R[1][1] * v.getY() + R[1][2] * v.getZ();
			double z = R[2][0] * v.getX() + R[2][1] * v.getY() + R[2][2] * v.getZ();
			basis.set(i, new Vector(x, y, z));
		}
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
