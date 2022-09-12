package org.asf.centuria.entities.objects;

import org.asf.centuria.entities.generic.Velocity;
import org.asf.centuria.enums.objects.WorldObjectMoverNodeType;

/**
 * Class that holds data for a world object move node... This is used by OI
 * packets to tell the client how to move an object.
 * 
 * @author owen9
 *
 */
public class WorldObjectMoveNodeData {

	public WorldObjectMoverNodeType nodeType;

	public long serverTime;

	public WorldObjectPositionInfo positionInfo;

	public Velocity velocity;

	public int actorActionType;

	public WorldObjectMoveNodeData() {
		this.nodeType = WorldObjectMoverNodeType.InitPosition;
		this.serverTime = 0;
		this.positionInfo = new WorldObjectPositionInfo();
		this.velocity = new Velocity();
		this.actorActionType = 0;
	}

	public WorldObjectMoveNodeData(WorldObjectMoverNodeType nodeType, long serverTime,
			WorldObjectPositionInfo positionInfo, Velocity velocity, int actorActionType) {
		this.nodeType = nodeType;
		this.serverTime = serverTime;
		this.positionInfo = positionInfo;
		this.velocity = velocity;
		this.actorActionType = actorActionType;
	}
}
