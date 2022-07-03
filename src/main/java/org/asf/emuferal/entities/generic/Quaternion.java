package org.asf.emuferal.entities.generic;

public class Quaternion {

	public double x;
	public double y;
	public double z;
	public double w;

	public Quaternion() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		this.w = 0;
	}

	public Quaternion(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
}
