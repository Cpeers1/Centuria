package org.asf.emuferal.packets.xt.gameserver.sanctuaries;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.entities.objects.WorldObjectMoveNodeData;
import org.asf.emuferal.entities.sanctuaries.SanctuaryObjectData;
import org.asf.emuferal.enums.sanctuaries.SanctuaryObjectType;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.objects.WorldObjectInfo;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonObject;

/**
 * An extension of the object info packet that also includes data for sanctuary
 * funiture objects.
 * 
 * @author owen9
 *
 */
public class SanctuaryWorldObjectInfo extends WorldObjectInfo {

	public SanctuaryObjectData sancObjectInfo;
	public boolean writeFurnitureInfo;
	public JsonObject funitureObject;
	public SanctuaryObjectType objectType;
	
	@Override
	public void build(XtWriter writer) throws IOException {
		//TODO: I can't call super because super will finalize everything
		
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
		
		writer.writeInt(objectType.value); //sanctuary object type
		
		if (writeFurnitureInfo)
			writer.writeString(funitureObject.toString());

		writer.writeString(String.valueOf(sancObjectInfo.gridId)); // grid
		writer.writeString(sancObjectInfo.parentId); // parent item ??
		writer.writeInt(sancObjectInfo.state); // state
		writer.writeString(""); // data suffix
	}

}
