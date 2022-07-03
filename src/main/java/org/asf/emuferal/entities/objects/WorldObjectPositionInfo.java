package org.asf.emuferal.entities.objects;

import org.asf.emuferal.entities.generic.Quaternion;
import org.asf.emuferal.entities.generic.Vector3;

public class WorldObjectPositionInfo {

	public Vector3 position;
	public Quaternion rotation;

	public WorldObjectPositionInfo() {
		this.position = new Vector3();
		this.rotation = new Quaternion();
	}

	public WorldObjectPositionInfo(Vector3 position) {
		this.position = position;
		this.rotation = new Quaternion();
	}

	public WorldObjectPositionInfo(Vector3 position, Quaternion rotation) {
		this.position = position;
		this.rotation = rotation;
	}

	public WorldObjectPositionInfo(double posX, double posY, double posZ, double rotX, double rotY, double rotZ,
			double rotW) {
		this.position = new Vector3(posX, posY, posZ);
		this.rotation = new Quaternion(rotX, rotY, rotZ, rotW);
	}

}
