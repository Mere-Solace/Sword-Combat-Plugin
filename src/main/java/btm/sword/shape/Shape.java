package btm.sword.shape;


import org.bukkit.*;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;

public abstract class Shape {
	protected Collection<Vector> basisPointVectors;
	private Collection<Vector> cachedPointVectors;
	protected Vector up;
	protected Vector forward;
	protected Vector right;
	
	public Shape() {
		basisPointVectors = new ArrayList<>();
		cachedPointVectors = null;
		up = new Vector (0, 1, 0);
		forward = new Vector (0, 0, 1);
		right = new Vector (1, 0, 0);
		
		generateVectors();
	}
	
	public Shape(Vector forward, Location o) {
		basisPointVectors = new ArrayList<>();
		cachedPointVectors = null;
		setForward(forward, o);
		generateVectors();
	}
	
	abstract void generateVectors();
	
	public void setForward(Vector e, Location o) {
		if (forward.dot(e) > .999) return;
		
		cachedPointVectors = null;
		
		this.forward = e.clone();
		Vector ref = new Vector(0,1,0);
		if (Math.abs(e.dot(ref)) > .999) {
			double yaw = Math.toRadians(o.getYaw());
			ref = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
		}
		right = ref.getCrossProduct(forward).normalize();
		up = right.getCrossProduct(forward).normalize();
	}
	
	private void calcPointVectors() {
		if (basisPointVectors == null) return;
		
		if (cachedPointVectors == null) {
			cachedPointVectors = new ArrayList<>(basisPointVectors.size());
		}
		else cachedPointVectors.clear();
		
		for (Vector u : basisPointVectors) {
			cachedPointVectors.add(
				right.clone().multiply(u.getX())
				.add(up.clone().multiply(u.getY()))
				.add(forward.clone().multiply(u.getZ()))
			);
		}
	}
	
	public Collection<Location> generatePoints(Location o) {
		if (cachedPointVectors == null) {
			calcPointVectors();
		}
		
		Collection<Location> points = new ArrayList<>(cachedPointVectors.size());
		
		for (Vector u : cachedPointVectors) {
			points.add(o.clone().add(u));
		}
		
		return points;
	}
}
