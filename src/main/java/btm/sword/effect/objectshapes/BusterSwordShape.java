package btm.sword.effect.objectshapes;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BusterSwordShape {
	public static List<List<Vector>> generate() {
		List<List<Vector>> layers = new ArrayList<>();
		
		// === Layer 1: Short Handle ===
		List<Vector> handle = new ArrayList<>();
		handle.add(new Vector(0, 0, 0));
		handle.add(new Vector(0, 0, 0.5));
		handle.add(new Vector(0, 0, 0.75));
		layers.add(handle);
		
		// === Layer 2: Thick Blade Base ===
		List<Vector> baseBlade = new ArrayList<>();
		double baseWidth = 0.25;
		double[] baseHeights = {0.75, 0.5, 0.25, 0, -0.25, -0.5, -0.75};
		double[] baseZs = {1, 1.25};
		
		for (double z : baseZs) {
			for (double y : baseHeights) {
				baseBlade.add(new Vector(baseWidth, y, z));
				baseBlade.add(new Vector(-baseWidth, y, z));
			}
		}
		layers.add(baseBlade);
		
		// === Layer 3: Long Blade ===
		List<Vector> longBlade = new ArrayList<>();
		double bladeWidth = 0.15; // narrower than base
		double bladeLength = 6;   // how long you want the blade
		double bladeFrequency = 0.25;
		for (double z = 1.5; z <= bladeLength; z += bladeFrequency) {
			for (double y : baseHeights) {
				longBlade.add(new Vector(bladeWidth, y, z));
				longBlade.add(new Vector(-bladeWidth, y, z));
			}
		}
		
		double tipLength = 1;
		double tipHeight = 0.75; // slant height
		
		int tipSteps = 10;
		for (int i = 0; i <= tipSteps; i++) {
			double ratio = (double) i / tipSteps;
			double z = bladeLength + ratio * tipLength;
			double width = bladeWidth * (1 - ratio); // narrowing
			double height = tipHeight * (1 - ratio); // slanting down
			longBlade.add(new Vector(width, height, z));
			longBlade.add(new Vector(-width, height, z));
			longBlade.add(new Vector(width, -height, z));
			longBlade.add(new Vector(-width, -height, z));
		}
		layers.add(longBlade);
		
		return layers;
	}
}