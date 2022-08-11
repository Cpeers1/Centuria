package org.asf.centuria.packets.xt.gameserver.players;

import java.io.IOException;

import org.asf.centuria.data.XtWriter;
import org.asf.centuria.packets.xt.gameserver.objects.WorldObjectInfo;

import com.google.gson.JsonObject;

public class PlayerWorldObjectInfo extends WorldObjectInfo {
    public JsonObject look; //TODO: make into a component (eventually)
    public String displayName;
    public int unknownValue; //TODO: what is this??

    @Override
	public void build(XtWriter writer) throws IOException {
		//I can't call super because super will finalize everything
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
		
        // Look and name
        writer.writeString(look.toString());
        writer.writeString(displayName);
        writer.writeInt(unknownValue);
        
        writer.writeString(""); // data suffix
	}
}
