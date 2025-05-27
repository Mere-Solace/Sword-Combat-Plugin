package btm.sword.shape;

import org.bukkit.util.Vector;

public class LineShape extends Shape {
	double length;
	double frequency;
	
	public LineShape(double length, double frequency) {
		this.length = length;
		this.frequency = frequency;
	}
	
	@Override
	void genBasisPointVectors() {
		double x = length/(length*frequency);
		Vector v = new Vector(0, 0, 1);
		for (double i = 0; i < length; i+=x) {
			basisPointVectors.add(v.clone());
			v.multiply(i);
		}
	}
}
