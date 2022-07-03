package org.asf.emuferal.entities.generic;

public class Velocity {

	public Vector3 direction;
	public float speed;

	public Velocity() {
		this.direction = new Vector3();
		this.speed = 0;
	}

	public Velocity(Vector3 direction, float speed) {
		this.direction = direction;
		this.speed = speed;
	}

}
