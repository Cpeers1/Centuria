package org.asf.emuferal.packets.xt.gameserver.objects;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.entities.objects.WorldObjectMoveNodeData;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;

public class WorldObjectInfo implements IXtPacket<WorldObjectInfo> {

	private static final String PACKET_ID = "oi";

	public String id;
	public int defId;
	public String ownerId;
	public WorldObjectMoveNodeData lastMove;

	@Override
	public WorldObjectInfo instantiate() {
		return new WorldObjectInfo();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		// There is no inbound for this packet type.
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(-1);
		
		writer.writeString(id); // World object ID
		writer.writeInt(defId); // Def Id 
		writer.writeString(ownerId); // Owner ID
		
		writer.writeInt(lastMove.nodeType.value); //Node type
		writer.writeLong(lastMove.serverTime); //server time
		
		//Position (X,Y,Z)
		writer.writeDouble(lastMove.positionInfo.position.x);
		writer.writeDouble(lastMove.positionInfo.position.y);
		writer.writeDouble(lastMove.positionInfo.position.z);
		
		//Rotation (X,Y,Z)
		writer.writeDouble(lastMove.positionInfo.rotation.x);
		writer.writeDouble(lastMove.positionInfo.rotation.y);
		writer.writeDouble(lastMove.positionInfo.rotation.z);
		writer.writeDouble(lastMove.positionInfo.rotation.w);
		
		//Likely Direction (X,Y,Z)
		writer.writeDouble(lastMove.velocity.direction.x);
		writer.writeDouble(lastMove.velocity.direction.y);
		writer.writeDouble(lastMove.velocity.direction.z);
		
		//Speed (Floats are always suffixed in .0, so i know this is speed)
		writer.writeFloat(lastMove.velocity.speed);
		
		//Action Type
		writer.writeInt(lastMove.actorActionType.value);
		
		writer.writeString("");
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// There is no inbound for this packet type.
		return true;
	}

}
