package btm.sword.shape;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class LineShape extends Shape {
	double length;
	double frequency;
	
	public LineShape(Vector forward, Location o, double length, double frequency) {
		super(forward, o);
		this.length = length;
		this.frequency = frequency;
	}
	
	@Override
	void generateVectors() {
		double x = length/(length*frequency);
		Vector v = forward.clone();
		for (double i = 0; i < length; i+=x) {
			basisPointVectors.add(v.clone());
			v.multiply(i);
		}
	}
}
