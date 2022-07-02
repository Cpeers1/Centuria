package org.asf.emuferal.entities.objects;

import org.asf.emuferal.entities.generic.Velocity;
import org.asf.emuferal.enums.actors.ActorActionType;
import org.asf.emuferal.enums.objects.WorldObjectMoverNodeType;

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

	public ActorActionType actorActionType;

	public WorldObjectMoveNodeData() {
		this.nodeType = WorldObjectMoverNodeType.InitPosition;
		this.serverTime = 0;
		this.positionInfo = new WorldObjectPositionInfo();
		this.velocity = new Velocity();
		this.actorActionType = ActorActionType.None;
	}

	public WorldObjectMoveNodeData(WorldObjectMoverNodeType nodeType, long serverTime,
			WorldObjectPositionInfo positionInfo, Velocity velocity, ActorActionType actorActionType) {
		this.nodeType = nodeType;
		this.serverTime = serverTime;
		this.positionInfo = positionInfo;
		this.velocity = velocity;
		this.actorActionType = actorActionType;
	}
}
