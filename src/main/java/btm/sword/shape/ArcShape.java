package btm.sword.shape;

import org.bukkit.util.Vector;

public class ArcShape extends Shape {
	double radius;
	double arcLength;
	double xyRotation;
	double xzRotation;
	double yzRotation;
	double frequency;
	
	public ArcShape(double radius, double arcLength, double xyRotation, double xzRotation, double yzRotation, double frequency) {
		this.radius = radius;
		this.arcLength = arcLength;
		this.xyRotation = xyRotation;
		this.xzRotation = xzRotation;
		this.yzRotation = yzRotation;
		this.frequency = frequency;
	}
	
	@Override
	void genBasisPointVectors() {
		Vector u = right.clone()
				.rotateAroundAxis(up, -1*((Math.PI/2)-(arcLength/2))-xzRotation)
				.rotateAroundAxis(right, -xyRotation)
				.rotateAroundAxis(forward, -xzRotation)
				.multiply(radius);
		
		double spacing = arcLength/(arcLength*frequency);
		
		for (double i = 0; i <= arcLength; i+=spacing) {
			basisPointVectors.add(u.clone());
			u.rotateAroundAxis(up, -spacing);
		}
	}
}
