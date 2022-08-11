package org.asf.centuria.enums.objects;

public enum WorldObjectMoverNodeType {

	InitPosition(0), Destroy(1), Move(2), Rotate(3), Action(4), Teleporter(5), Slide(6);

	public int value;

	WorldObjectMoverNodeType(int value) {
		this.value = value;
	}

}