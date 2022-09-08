package org.asf.centuria.packets.xt.gameserver.world;

import java.io.IOException;

import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.objects.WorldObjectMoveNodeData;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;

public class WorldObjectInfoPacket implements IXtPacket<WorldObjectInfoPacket> {

	private static final String PACKET_ID = "oi";

	public String id;
	public int defId;
	public String ownerId;
	public WorldObjectMoveNodeData lastMove;

	@Override
	public WorldObjectInfoPacket instantiate() {
		return new WorldObjectInfoPacket();
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
		writer.writeInt(DATA_PREFIX);
		
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
		
		writer.writeString(DATA_SUFFIX);
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// There is no inbound for this packet type.
		return true;
	}

}
